package wcpredictor.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wcpredictor.entity.*;
import wcpredictor.repository.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PredictionService {

    private final MatchPredictionRepository matchPredictionRepo;
    private final TournamentPredictionRepository tournamentPredictionRepo;
    private final GroupAdvancementPredictionRepository groupAdvancementPredictionRepo;
    private final MatchRepository matchRepository;
    private final SettingRepository settingRepository;
    private final TeamService teamService;

    public PredictionService(MatchPredictionRepository matchPredictionRepo,
                             TournamentPredictionRepository tournamentPredictionRepo,
                             GroupAdvancementPredictionRepository groupAdvancementPredictionRepo,
                             MatchRepository matchRepository,
                             SettingRepository settingRepository,
                             TeamService teamService) {
        this.matchPredictionRepo = matchPredictionRepo;
        this.tournamentPredictionRepo = tournamentPredictionRepo;
        this.groupAdvancementPredictionRepo = groupAdvancementPredictionRepo;
        this.matchRepository = matchRepository;
        this.settingRepository = settingRepository;
        this.teamService = teamService;
    }

    @Transactional
    public void saveMatchPrediction(User user, UUID matchId, Integer team1Score, Integer team2Score) {
        Match match = matchRepository.findById(matchId).orElseThrow();
        if (match.isLocked()) {
            throw new IllegalStateException("Predictions are closed for this match.");
        }
        MatchPrediction prediction = matchPredictionRepo.findByUserIdAndMatchId(user.getId(), matchId)
                .orElse(new MatchPrediction());
        prediction.setUser(user);
        prediction.setMatch(match);
        prediction.setTeam1Score(team1Score);
        prediction.setTeam2Score(team2Score);
        prediction.setTimestamp(Instant.now());
        matchPredictionRepo.save(prediction);
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

    @Transactional
    public void saveTournamentPrediction(User user, TournamentPrediction prediction) {
        TournamentPrediction existing = tournamentPredictionRepo.findByUserId(user.getId())
                .orElse(new TournamentPrediction());
        existing.setUser(user);
        existing.setGoldenBoot(prediction.getGoldenBoot());
        existing.setGoldenBall(prediction.getGoldenBall());
        existing.setGoldenGlove(prediction.getGoldenGlove());
        existing.setYoungPlayer(prediction.getYoungPlayer());
        existing.setFairPlay(prediction.getFairPlay());
        existing.setMostEntertaining(prediction.getMostEntertaining());
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
    public void saveGroupAdvancementPredictions(User user, List<UUID> advancingTeamIds) {
        Map<Character, Integer> groupCounts = new LinkedHashMap<>();
        for (char g = 'A'; g <= 'L'; g++) {
            groupCounts.put(g, 0);
        }

        for (UUID teamId : advancingTeamIds) {
            var team = teamService.findById(teamId);
            if (team.isPresent() && team.get().getGroupLetter() != null) {
                char g = team.get().getGroupLetter().charAt(0);
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

    public Map<Character, List<GroupStanding>> getUserGroupStandings(UUID userId) {
        Map<UUID, int[]> scores = getUserMatchPredictionScores(userId);
        Map<Character, List<GroupStanding>> standings = new LinkedHashMap<>();

        for (char group = 'A'; group <= 'L'; group++) {
            List<Team> teams = teamService.getTeamsByGroup(String.valueOf(group));
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
                groupStandings.add(new GroupStanding(team, r[0], r[1], r[2], r[3], r[4], r[5], 0));
            }
            groupStandings.sort(Comparator.naturalOrder());
            for (int i = 0; i < groupStandings.size(); i++) {
                groupStandings.set(i, new GroupStanding(
                        groupStandings.get(i).getTeam(),
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
