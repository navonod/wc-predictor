package wcpredictor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wcpredictor.entity.*;
import wcpredictor.repository.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PredictionService {

    private static final Logger log = LoggerFactory.getLogger(PredictionService.class);

    private final MatchPredictionRepository matchPredictionRepo;
    private final TournamentPredictionRepository tournamentPredictionRepo;
    private final GroupAdvancementPredictionRepository groupAdvancementPredictionRepo;
    private final MatchRepository matchRepository;
    private final SettingRepository settingRepository;
    private final TeamService teamService;
    private final TimeService timeService;
    private final ScoringService scoringService;
    private final KnockoutBracketService bracketService;
    private final TournamentActualRepository tournamentActualRepo;

    public PredictionService(MatchPredictionRepository matchPredictionRepo,
                             TournamentPredictionRepository tournamentPredictionRepo,
                             GroupAdvancementPredictionRepository groupAdvancementPredictionRepo,
                             MatchRepository matchRepository,
                             SettingRepository settingRepository,
                             TeamService teamService,
                             TimeService timeService,
                             ScoringService scoringService,
                             KnockoutBracketService bracketService,
                             TournamentActualRepository tournamentActualRepo) {
        this.matchPredictionRepo = matchPredictionRepo;
        this.tournamentPredictionRepo = tournamentPredictionRepo;
        this.groupAdvancementPredictionRepo = groupAdvancementPredictionRepo;
        this.matchRepository = matchRepository;
        this.settingRepository = settingRepository;
        this.teamService = teamService;
        this.timeService = timeService;
        this.scoringService = scoringService;
        this.bracketService = bracketService;
        this.tournamentActualRepo = tournamentActualRepo;
    }

    @Transactional
    public TournamentActual getOrCreateTournamentActual(UUID tournamentId) {
        if (tournamentId == null) return null;
        var actual = tournamentActualRepo.findByTournamentId(tournamentId)
                .orElse(new TournamentActual());

        Match finalMatch = matchRepository.findAll().stream()
                .filter(m -> m.getMatchNumber() == 104).findFirst().orElse(null);
        boolean changed = false;
        if (finalMatch != null) {
            if (actual.getFinalist1() == null && finalMatch.getTeam1() != null) {
                actual.setFinalist1(finalMatch.getTeam1()); changed = true;
            }
            if (actual.getFinalist2() == null && finalMatch.getTeam2() != null) {
                actual.setFinalist2(finalMatch.getTeam2()); changed = true;
            }
            if (actual.getChampionTeam() == null && finalMatch.getTeam1Score() != null
                    && finalMatch.getTeam2Score() != null) {
                Team winner = finalMatch.getTeam1Score() > finalMatch.getTeam2Score()
                        ? finalMatch.getTeam1() : finalMatch.getTeam2();
                actual.setChampionTeam(winner);
                changed = true;
            }
        }
        if (changed) {
            Tournament t = new Tournament();
            t.setId(tournamentId);
            actual.setTournament(t);
            tournamentActualRepo.save(actual);
        }
        return actual;
    }

    private String trim(String s) {
        return s != null ? s.trim() : null;
    }

    @Transactional
    public void trimAllPredictions() {
        for (var pred : tournamentPredictionRepo.findAll()) {
            boolean changed = false;
            if (pred.getFairPlay() != null && !pred.getFairPlay().equals(pred.getFairPlay().trim())) {
                pred.setFairPlay(pred.getFairPlay().trim()); changed = true;
            }
            if (pred.getMostEntertaining() != null && !pred.getMostEntertaining().equals(pred.getMostEntertaining().trim())) {
                pred.setMostEntertaining(pred.getMostEntertaining().trim()); changed = true;
            }
            if (changed) tournamentPredictionRepo.save(pred);
        }
    }

    @Transactional
    public void saveTournamentActualFromSettings(UUID tournamentId, Map<String, String> params) {
        Tournament t = new Tournament();
        t.setId(tournamentId);
        var actual = tournamentActualRepo.findByTournamentId(tournamentId)
                .orElse(new TournamentActual());
        actual.setTournament(t);

        String gb = params.get("actual_golden_boot");
        String gball = params.get("actual_golden_ball");
        String gglove = params.get("actual_golden_glove");
        String yp = params.get("actual_young_player");
        String fp = params.get("actual_fair_play");
        String ent = params.get("actual_entertaining");

        if (gb != null) actual.setGoldenBoot(gb.isBlank() ? null : gb.trim());
        if (gball != null) actual.setGoldenBall(gball.isBlank() ? null : gball.trim());
        if (gglove != null) actual.setGoldenGlove(gglove.isBlank() ? null : gglove.trim());
        if (yp != null) actual.setYoungPlayer(yp.isBlank() ? null : yp.trim());
        if (fp != null && !fp.isBlank()) actual.setFairPlay(getTeamRef(UUID.fromString(fp)));
        else actual.setFairPlay(null);
        if (ent != null && !ent.isBlank()) actual.setMostEntertaining(getTeamRef(UUID.fromString(ent)));
        else actual.setMostEntertaining(null);

        tournamentActualRepo.save(actual);
    }

    private Team getTeamRef(UUID id) {
        Team t = new Team();
        t.setId(id);
        return t;
    }

    @Transactional
    public void recalculateAllAwardPoints(UUID tournamentId) {
        var actual = tournamentActualRepo.findByTournamentId(tournamentId).orElse(null);
        if (actual == null) return;

        double championPts = getSettingDouble("points_champion", 20);
        double finalistPts = getSettingDouble("points_finalist", 10);
        double fairPlayPts = getSettingDouble("points_fair_play", 3);
        double entertainingPts = getSettingDouble("points_entertaining", 3);

        for (var pred : tournamentPredictionRepo.findAll()) {
            double pts = 0;
            if (actual.getChampionTeam() != null && pred.getChampion() != null
                    && actual.getChampionTeam().getId().equals(pred.getChampion().getId())) pts += championPts;
            if (actual.getFinalist1() != null && pred.getFinalist1() != null
                    && (actual.getFinalist1().getId().equals(pred.getFinalist1().getId())
                        || (actual.getFinalist2() != null && actual.getFinalist2().getId().equals(pred.getFinalist1().getId()))))
                pts += finalistPts;
            if (actual.getFinalist2() != null && pred.getFinalist2() != null
                    && (actual.getFinalist2().getId().equals(pred.getFinalist2().getId())
                        || (actual.getFinalist1() != null && actual.getFinalist1().getId().equals(pred.getFinalist2().getId()))))
                pts += finalistPts;
            if (actual.getFairPlay() != null && pred.getFairPlay() != null
                    && actual.getFairPlay().getName() != null
                    && actual.getFairPlay().getName().trim().equalsIgnoreCase(pred.getFairPlay().trim())) pts += fairPlayPts;
            if (actual.getMostEntertaining() != null && pred.getMostEntertaining() != null
                    && actual.getMostEntertaining().getName() != null
                    && actual.getMostEntertaining().getName().trim().equalsIgnoreCase(pred.getMostEntertaining().trim())) pts += entertainingPts;
            pred.setAwardPoints(pts);
            tournamentPredictionRepo.save(pred);
        }
    }

    public Map<String, Double> getAwardSettings() {
        Map<String, Double> map = new LinkedHashMap<>();
        map.put("golden_boot", getSettingDouble("points_golden_boot", 5));
        map.put("golden_ball", getSettingDouble("points_golden_ball", 5));
        map.put("golden_glove", getSettingDouble("points_golden_glove", 5));
        map.put("young_player", getSettingDouble("points_young_player", 5));
        map.put("fair_play", getSettingDouble("points_fair_play", 3));
        map.put("entertaining", getSettingDouble("points_entertaining", 3));
        map.put("finalist", getSettingDouble("points_finalist", 10));
        map.put("champion", getSettingDouble("points_champion", 20));
        return map;
    }

    @Transactional
    public void saveManualAwardPoints(UUID userId, double points,
                                        boolean boot, boolean ball, boolean glove, boolean young) {
        tournamentPredictionRepo.findByUserId(userId).ifPresent(pred -> {
            pred.setManualAwardPoints(points);
            pred.setGoldenBootCorrect(boot);
            pred.setGoldenBallCorrect(ball);
            pred.setGoldenGloveCorrect(glove);
            pred.setYoungPlayerCorrect(young);
            tournamentPredictionRepo.save(pred);
        });
    }

    private double getSettingDouble(String name, double def) {
        return settingRepository.findByName(name)
                .map(s -> Double.parseDouble(s.getValue())).orElse(def);
    }

    @Transactional
    public void saveMatchPrediction(User user, UUID matchId, Integer team1Score, Integer team2Score) {
        Match match = matchRepository.findById(matchId).orElseThrow();
        if (match.isLocked(timeService.now())) {
            throw new IllegalStateException("Predictions are closed for this match.");
        }
        MatchPrediction prediction = matchPredictionRepo.findByUserIdAndMatchId(user.getId(), matchId)
                .orElse(new MatchPrediction());
        prediction.setUser(user);
        prediction.setMatch(match);
        prediction.setTeam1Score(team1Score);
        prediction.setTeam2Score(team2Score);
        prediction.setTimestamp(Instant.now());
        prediction.setPointsEarned(scoringService.scoreMatch(prediction));
        matchPredictionRepo.save(prediction);
    }

    @Transactional
    public int recalculatePointsForMatch(UUID matchId) {
        int count = 0;
        for (var pred : matchPredictionRepo.findByMatchId(matchId)) {
            pred.setPointsEarned(scoringService.scoreMatch(pred));
            matchPredictionRepo.save(pred);
            count++;
        }
        return count;
    }

    public List<MatchPrediction> getUserMatchPredictions(UUID userId) {
        return matchPredictionRepo.findByUserId(userId);
    }

    public Map<UUID, MatchPrediction> getUserMatchPredictionsMap(UUID userId) {
        return matchPredictionRepo.findByUserId(userId).stream()
                .collect(Collectors.toMap(p -> p.getMatch().getId(), p -> p));
    }

    public Optional<MatchPrediction> getUserPredictionForMatch(UUID userId, UUID matchId) {
        return matchPredictionRepo.findByUserIdAndMatchId(userId, matchId);
    }

    public List<BracketMatch> getKnockoutBracket(UUID userId, UUID tournamentId) {
        return bracketService.getKnockoutBracket(getUserMatchPredictionScores(userId), tournamentId);
    }

    public Map<RoundType, List<BracketMatch>> getAllKnockoutRounds(UUID userId, UUID tournamentId) {
        return bracketService.getAllKnockoutRounds(getUserMatchPredictionScores(userId), tournamentId);
    }

    public Map<RoundType, List<BracketMatch>> getActualAllKnockoutRounds(UUID tournamentId) {
        return bracketService.getActualAllKnockoutRounds(tournamentId);
    }

    public boolean isGroupStageComplete() {
        return scoringService.isGroupStageComplete();
    }

    public Set<UUID> getAdvancingTeamIds() {
        return scoringService.getAdvancingTeamIds();
    }

    public Match findCurrentMatch() {
        var now = timeService.now();
        var all = matchRepository.findAllByOrderByMatchDateAsc();
        for (var m : all) {
            if (m.getMatchDate() != null) {
                boolean inWindow = !now.isBefore(m.getMatchDate().minusMinutes(15))
                        && now.isBefore(m.getMatchDate().plusHours(3));
                if (inWindow) return m;
            }
        }
        return null;
    }

    public List<Match> findAllLiveMatches() {
        var now = timeService.now();
        List<Match> live = new ArrayList<>();
        for (var m : matchRepository.findAllByOrderByMatchDateAsc()) {
            if (m.getMatchDate() != null) {
                boolean inWindow = !now.isBefore(m.getMatchDate().minusMinutes(15))
                        && now.isBefore(m.getMatchDate().plusHours(3));
                if (inWindow) live.add(m);
            }
        }
        return live;
    }

    public int getCurrentMatchday() {
        var now = timeService.now();
        LocalDateTime[] mdStarts = {
            LocalDateTime.of(2026, 6, 11, 0, 0),
            LocalDateTime.of(2026, 6, 18, 0, 0),
            LocalDateTime.of(2026, 6, 24, 0, 0),
        };
        for (int i = 2; i >= 0; i--) {
            if (!now.isBefore(mdStarts[i])) return i + 1;
        }
        return 1;
    }

    public RoundType getActiveKnockoutRound() {
        List<RoundType> koRounds = List.of(RoundType.ROUND_OF_32, RoundType.ROUND_OF_16,
                RoundType.QUARTER_FINAL, RoundType.SEMI_FINAL, RoundType.THIRD_PLACE, RoundType.FINAL);
        for (int i = koRounds.size() - 1; i >= 0; i--) {
            RoundType r = koRounds.get(i);
            var matches = matchRepository.findByRoundOrderByMatchDateAsc(r);
            if (matches.isEmpty()) continue;
            boolean allScored = matches.stream().allMatch(m -> m.getTeam1Score() != null);
            if (allScored) {
                return i + 1 < koRounds.size() ? koRounds.get(i + 1) : r;
            }
        }
        return RoundType.ROUND_OF_32;
    }

    public List<AsItStandEntry> getAsItStandsBoard(Match match, UUID currentUserId) {
        var results = new ArrayList<AsItStandEntry>();
        boolean hasScores = match.getTeam1Score() != null && match.getTeam2Score() != null;
        var preds = matchPredictionRepo.findByMatchId(match.getId());
        for (var pred : preds) {
            double ais = hasScores ? scoringService.scoreMatch(pred) : 0;
            double total = scoringService.getTotalPoints(pred.getUser().getId());
            results.add(new AsItStandEntry(pred.getUser(), pred, ais, total));
        }
        results.sort((a, b) -> Double.compare(b.ais, a.ais));
        return results;
    }


    public static class AsItStandEntry {
        public final User user;
        public final MatchPrediction prediction;
        public final double ais;
        public final double total;

        public AsItStandEntry(User user, MatchPrediction pred, double ais, double total) {
            this.user = user;
            this.prediction = pred;
            this.ais = ais;
            this.total = total;
        }
    }

    @Transactional
    public void saveTournamentPrediction(User user, TournamentPrediction prediction, UUID tournamentId) {
        TournamentPrediction existing = tournamentPredictionRepo.findByUserId(user.getId())
                .orElse(new TournamentPrediction());
        existing.setUser(user);
        if (tournamentId != null) {
            Tournament t = new Tournament();
            t.setId(tournamentId);
            existing.setTournament(t);
        }
        existing.setGoldenBoot(trim(prediction.getGoldenBoot()));
        existing.setGoldenBall(trim(prediction.getGoldenBall()));
        existing.setGoldenGlove(trim(prediction.getGoldenGlove()));
        existing.setYoungPlayer(trim(prediction.getYoungPlayer()));
        existing.setFairPlay(trim(prediction.getFairPlay()));
        existing.setMostEntertaining(trim(prediction.getMostEntertaining()));
        existing.setFinalist1(prediction.getFinalist1());
        existing.setFinalist2(prediction.getFinalist2());
        existing.setChampion(prediction.getChampion());
        existing.setTimestamp(Instant.now());
        tournamentPredictionRepo.save(existing);
    }

    public Optional<TournamentPrediction> getUserTournamentPrediction(UUID userId) {
        return tournamentPredictionRepo.findByUserId(userId);
    }

    @Transactional
    public void saveGroupAdvancementPredictions(User user, List<UUID> advancingTeamIds, UUID tournamentId) {
        Map<Character, Integer> groupCounts = new LinkedHashMap<>();
        for (char g = 'A'; g <= 'L'; g++) {
            groupCounts.put(g, 0);
        }

        for (UUID teamId : advancingTeamIds) {
            var tt = teamService.getTournamentTeam(tournamentId, teamId);
            if (tt.isPresent() && tt.get().getGroupLetter() != null) {
                char g = tt.get().getGroupLetter().charAt(0);
                groupCounts.merge(g, 1, Integer::sum);
            }
        }

        for (var entry : groupCounts.entrySet()) {
            if (entry.getValue() > 3) {
                throw new IllegalArgumentException("Group " + entry.getKey() + " has " + entry.getValue()
                        + " teams selected. Maximum is 3 per group.");
            }
            if (entry.getValue() < 2) {
                throw new IllegalArgumentException("Group " + entry.getKey() + " has " + entry.getValue()
                        + " teams selected. At least 2 per group required.");
            }
        }

        long groupsWith3 = groupCounts.values().stream().filter(c -> c == 3).count();
        if (groupsWith3 > 8) {
            throw new IllegalArgumentException(groupsWith3
                    + " groups have 3 teams selected. Only 8 groups may have 3 teams (the 8 best third-place advancers).");
        }

        groupAdvancementPredictionRepo.deleteByUserId(user.getId());
        Set<UUID> top2Ids = new HashSet<>(advancingTeamIds.subList(0, Math.min(24, advancingTeamIds.size())));
        Set<UUID> thirdIds = new HashSet<>();
        if (advancingTeamIds.size() > 24) {
            thirdIds = new HashSet<>(advancingTeamIds.subList(24, advancingTeamIds.size()));
        }
        for (UUID teamId : advancingTeamIds) {
            var g = new GroupAdvancementPrediction();
            var team = new Team();
            team.setId(teamId);
            g.setUser(user);
            g.setTeam(team);
            g.setThirdPlaceAdvancer(thirdIds.contains(teamId));
            groupAdvancementPredictionRepo.save(g);
        }
    }

    public List<GroupAdvancementPrediction> getUserGroupAdvancementPredictions(UUID userId) {
        return groupAdvancementPredictionRepo.findByUserId(userId);
    }

    public boolean hasGroupPredictions(UUID userId) {
        return !groupAdvancementPredictionRepo.findByUserId(userId).isEmpty();
    }

    public Map<UUID, int[]> getUserMatchPredictionScores(UUID userId) {
        return matchPredictionRepo.findByUserId(userId).stream()
                .collect(Collectors.toMap(
                        p -> p.getMatch().getId(),
                        p -> new int[]{p.getTeam1Score(), p.getTeam2Score()}));
    }

    public Map<UUID, int[]> getActualScores() {
        return matchRepository.findAll().stream()
                .filter(m -> m.getTeam1Score() != null)
                .collect(Collectors.toMap(Match::getId,
                        m -> new int[]{m.getTeam1Score(), m.getTeam2Score()}));
    }

    public Map<UUID, int[]> getActualPenalties() {
        return matchRepository.findAll().stream()
                .filter(m -> m.getTeam1PenaltiesScore() != null)
                .collect(Collectors.toMap(Match::getId,
                        m -> new int[]{m.getTeam1PenaltiesScore(), m.getTeam2PenaltiesScore()}));
    }

    public Map<Integer, int[]> getActualScoresByMatchNumber() {
        return matchRepository.findAll().stream()
                .filter(m -> m.getTeam1Score() != null)
                .collect(Collectors.toMap(Match::getMatchNumber,
                        m -> {
                            if (m.getTeam1PenaltiesScore() != null) {
                                return new int[]{m.getTeam1Score(), m.getTeam2Score(),
                                        m.getTeam1PenaltiesScore(), m.getTeam2PenaltiesScore()};
                            }
                            return new int[]{m.getTeam1Score(), m.getTeam2Score()};
                        }));
    }

    public Map<UUID, Double> getPointsMap(UUID userId) {
        return matchPredictionRepo.findByUserId(userId).stream()
                .filter(p -> p.getPointsEarned() != null)
                .collect(Collectors.toMap(p -> p.getMatch().getId(), MatchPrediction::getPointsEarned));
    }

    public Map<Character, List<GroupStanding>> getUserGroupStandings(UUID userId, UUID tournamentId) {
        Map<UUID, int[]> scores = getUserMatchPredictionScores(userId);
        Map<Character, List<GroupStanding>> standings = new LinkedHashMap<>();

        for (char group = 'A'; group <= 'L'; group++) {
            List<Team> teams = teamService.getTeamsByGroup(tournamentId, String.valueOf(group));
            if (teams.isEmpty()) continue;
            List<Match> matches = matchRepository.findByGroupLetterOrderByMatchDateAsc(String.valueOf(group));

            Map<UUID, int[]> records = new LinkedHashMap<>();
            for (Team team : teams) {
                records.put(team.getId(), new int[]{0, 0, 0, 0, 0, 0});
            }

            for (Match match : matches) {
                int[] sim = scores.get(match.getId());
                if (sim == null) continue;
                int s1 = sim[0];
                int s2 = sim[1];
                UUID t1Id = match.getTeam1().getId();
                UUID t2Id = match.getTeam2().getId();

                int[] r1 = records.get(t1Id);
                int[] r2 = records.get(t2Id);
                r1[0]++; r2[0]++;
                r1[4] += s1; r2[4] += s2;
                r1[5] += s2; r2[5] += s1;

                if (s1 > s2) { r1[1]++; r2[3]++; }
                else if (s1 < s2) { r1[3]++; r2[1]++; }
                else { r1[2]++; r2[2]++; }
            }

            List<GroupStanding> groupStandings = new ArrayList<>();
            for (Team team : teams) {
                int[] r = records.get(team.getId());
                groupStandings.add(new GroupStanding(team, group, r[0], r[1], r[2], r[3], r[4], r[5], 0));
            }
            groupStandings.sort(Comparator.naturalOrder());
            for (int i = 0; i < groupStandings.size(); i++) {
                groupStandings.set(i, new GroupStanding(
                        groupStandings.get(i).getTeam(),
                        group,
                        groupStandings.get(i).getPlayed(),
                        groupStandings.get(i).getWon(),
                        groupStandings.get(i).getDrawn(),
                        groupStandings.get(i).getLost(),
                        groupStandings.get(i).getGoalsFor(),
                        groupStandings.get(i).getGoalsAgainst(),
                        i + 1));
            }
            standings.put(group, groupStandings);
        }
        return standings;
    }

    public Map<Character, List<GroupStanding>> getActualGroupStandings(UUID tournamentId) {
        Map<UUID, int[]> scores = new HashMap<>();
        for (Match m : matchRepository.findByTournamentId(tournamentId)) {
            if (m.getTeam1Score() != null && m.getTeam2Score() != null) {
                scores.put(m.getId(), new int[]{m.getTeam1Score(), m.getTeam2Score()});
            }
        }
        Map<Character, List<GroupStanding>> standings = new LinkedHashMap<>();

        for (char group = 'A'; group <= 'L'; group++) {
            List<Team> teams = teamService.getTeamsByGroup(tournamentId, String.valueOf(group));
            if (teams.isEmpty()) continue;
            List<Match> matches = matchRepository.findByGroupLetterOrderByMatchDateAsc(String.valueOf(group));

            Map<UUID, int[]> records = new LinkedHashMap<>();
            for (Team team : teams) {
                records.put(team.getId(), new int[]{0, 0, 0, 0, 0, 0});
            }

            for (Match match : matches) {
                int[] sim = scores.get(match.getId());
                if (sim == null) continue;
                int s1 = sim[0], s2 = sim[1];
                UUID t1Id = match.getTeam1().getId(), t2Id = match.getTeam2().getId();
                int[] r1 = records.get(t1Id), r2 = records.get(t2Id);
                r1[0]++; r2[0]++;
                r1[4] += s1; r2[4] += s2;
                r1[5] += s2; r2[5] += s1;
                if (s1 > s2) { r1[1]++; r2[3]++; }
                else if (s1 < s2) { r1[3]++; r2[1]++; }
                else { r1[2]++; r2[2]++; }
            }

            List<GroupStanding> groupStandings = new ArrayList<>();
            for (Team team : teams) {
                int[] r = records.get(team.getId());
                groupStandings.add(new GroupStanding(team, group, r[0], r[1], r[2], r[3], r[4], r[5], 0));
            }
            groupStandings.sort(Comparator.naturalOrder());
            for (int i = 0; i < groupStandings.size(); i++) {
                groupStandings.set(i, new GroupStanding(
                        groupStandings.get(i).getTeam(),
                        group,
                        groupStandings.get(i).getPlayed(),
                        groupStandings.get(i).getWon(),
                        groupStandings.get(i).getDrawn(),
                        groupStandings.get(i).getLost(),
                        groupStandings.get(i).getGoalsFor(),
                        groupStandings.get(i).getGoalsAgainst(),
                        i + 1));
            }
            standings.put(group, groupStandings);
        }
        return standings;
    }
}
