package wcpredictor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wcpredictor.entity.Match;
import wcpredictor.entity.RoundType;
import wcpredictor.entity.Team;
import wcpredictor.entity.Tournament;
import wcpredictor.entity.TournamentTeam;
import wcpredictor.repository.MatchRepository;
import wcpredictor.repository.TournamentTeamRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnockoutBracketService {

    private static final Logger log = LoggerFactory.getLogger(KnockoutBracketService.class);

    private final MatchRepository matchRepository;
    private final TournamentTeamRepository tournamentTeamRepo;
    private final RoundOf32CombinationService combinationService;

    public KnockoutBracketService(MatchRepository matchRepository,
                                   TournamentTeamRepository tournamentTeamRepo,
                                   RoundOf32CombinationService combinationService) {
        this.matchRepository = matchRepository;
        this.tournamentTeamRepo = tournamentTeamRepo;
        this.combinationService = combinationService;
    }

    public List<BracketMatch> getKnockoutBracket(Map<UUID, int[]> scores, UUID tournamentId) {
        Map<Character, List<GroupStanding>> standings =
                computeStandings(scores, tournamentId);
        List<GroupStanding> bestThirds = getBestThirdPlacedTeams(standings);
        if (bestThirds.size() < 8) return List.of();

        Set<Character> thirdPlaceGroups = bestThirds.stream()
                .map(GroupStanding::getGroupLetter)
                .collect(Collectors.toSet());

        var optionOpt = combinationService.findOption(thirdPlaceGroups);
        if (optionOpt.isEmpty()) return List.of();
        int option = optionOpt.get();

        List<Character> firstPlaceGroups = combinationService.getFirstPlaceGroups();
        List<Character> thirdPlaceSlots = combinationService.getThirdPlaceGroupsForOption(option);

        Map<Character, Team> winners = new HashMap<>();
        Map<Character, Team> runnersUp = new HashMap<>();
        Map<Character, Team> thirds = new HashMap<>();

        for (var entry : standings.entrySet()) {
            char g = entry.getKey();
            List<GroupStanding> gs = entry.getValue();
            if (gs.size() >= 1) winners.put(g, gs.get(0).getTeam());
            if (gs.size() >= 2) runnersUp.put(g, gs.get(1).getTeam());
            if (gs.size() >= 3) thirds.put(g, gs.get(2).getTeam());
        }

        List<BracketMatch> bracket = new ArrayList<>();

        LocalDateTime[] dates1 = {
            LocalDateTime.of(2026, 6, 28, 13, 0), LocalDateTime.of(2026, 6, 28, 20, 0),
            LocalDateTime.of(2026, 6, 29, 13, 0), LocalDateTime.of(2026, 6, 29, 16, 0),
            LocalDateTime.of(2026, 6, 30, 13, 0), LocalDateTime.of(2026, 6, 30, 20, 0),
            LocalDateTime.of(2026, 7, 1, 13, 0),  LocalDateTime.of(2026, 7, 1, 20, 0),
        };
        String[] venues1 = {
            "Levi's Stadium, Santa Clara", "NRG Stadium, Houston",
            "BC Place, Vancouver", "Lumen Field, Seattle",
            "AT&T Stadium, Dallas", "MetLife Stadium, East Rutherford",
            "Hard Rock Stadium, Miami", "Mercedes-Benz Stadium, Atlanta"
        };

        for (int i = 0; i < 8; i++) {
            Team t1 = winners.get(firstPlaceGroups.get(i));
            Team t2 = thirds.get(thirdPlaceSlots.get(i));
            if (t1 != null && t2 != null) {
                bracket.add(new BracketMatch(73 + i, dates1[i], venues1[i], t1.getName(), t2.getName()));
            }
        }

        addIf(bracket, 81, LocalDateTime.of(2026, 6, 28, 16, 0), "Gillette Stadium, Boston", runnersUp, 'A', 'B');
        addIf(bracket, 82, LocalDateTime.of(2026, 6, 29, 20, 0), "Arrowhead Stadium, Kansas City", runnersUp, 'C', 'F');
        addIf(bracket, 83, LocalDateTime.of(2026, 6, 30, 16, 0), "SoFi Stadium, Los Angeles", runnersUp, 'D', 'E');
        addIf(bracket, 84, LocalDateTime.of(2026, 7, 1, 16, 0), "Lincoln Financial Field, Philadelphia", runnersUp, 'H', 'J');
        addIf(bracket, 85, LocalDateTime.of(2026, 7, 2, 13, 0), "BMO Field, Toronto", runnersUp, 'G', 'I');
        addIf(bracket, 86, LocalDateTime.of(2026, 7, 2, 20, 0), "BC Place, Vancouver", runnersUp, 'K', 'L');
        addWinnerRunner(bracket, 87, LocalDateTime.of(2026, 7, 3, 13, 0), "AT&T Stadium, Dallas", winners, 'C', runnersUp, 'J');
        addWinnerRunner(bracket, 88, LocalDateTime.of(2026, 7, 3, 20, 0), "NRG Stadium, Houston", winners, 'F', runnersUp, 'H');

        log.info("Built knockout bracket: option {}, {} matches", option, bracket.size());
        return bracket;
    }

    Map<Character, List<GroupStanding>> computeStandings(Map<UUID, int[]> scores, UUID tournamentId) {
        Map<Character, List<GroupStanding>> standings = new LinkedHashMap<>();

        for (char group = 'A'; group <= 'L'; group++) {
            var ttEntries = tournamentTeamRepo.findByTournamentIdAndGroupLetterOrderBySortOrderAsc(
                    tournamentId, String.valueOf(group));
            if (ttEntries.isEmpty()) continue;

            List<Team> teams = ttEntries.stream().map(TournamentTeam::getTeam).toList();
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

            List<GroupStanding> gs = new ArrayList<>();
            for (Team team : teams) {
                int[] r = records.get(team.getId());
                gs.add(new GroupStanding(team, group, r[0], r[1], r[2], r[3], r[4], r[5], 0));
            }
            gs.sort(Comparator.naturalOrder());
            for (int i = 0; i < gs.size(); i++) {
                var s = gs.get(i);
                gs.set(i, new GroupStanding(s.getTeam(), group, s.getPlayed(), s.getWon(),
                        s.getDrawn(), s.getLost(), s.getGoalsFor(), s.getGoalsAgainst(), i + 1));
            }
            standings.put(group, gs);
        }
        return standings;
    }

    List<GroupStanding> getBestThirdPlacedTeams(Map<Character, List<GroupStanding>> standings) {
        List<GroupStanding> thirdPlaced = new ArrayList<>();
        for (var entry : standings.entrySet()) {
            List<GroupStanding> group = entry.getValue();
            if (group.size() >= 3) thirdPlaced.add(group.get(2));
        }
        thirdPlaced.sort(Comparator.naturalOrder());
        return thirdPlaced.subList(0, Math.min(8, thirdPlaced.size()));
    }

    private void addIf(List<BracketMatch> bracket, int num, LocalDateTime date,
                        String venue, Map<Character, Team> map, char g1, char g2) {
        Team t1 = map.get(g1), t2 = map.get(g2);
        if (t1 != null && t2 != null)
            bracket.add(new BracketMatch(num, date, venue, t1.getName(), t2.getName()));
    }

    private void addWinnerRunner(List<BracketMatch> bracket, int num, LocalDateTime date,
                                  String venue, Map<Character, Team> winners, char wg,
                                  Map<Character, Team> runnersUp, char rg) {
        Team t1 = winners.get(wg), t2 = runnersUp.get(rg);
        if (t1 != null && t2 != null)
            bracket.add(new BracketMatch(num, date, venue, t1.getName(), t2.getName()));
    }

    public Map<RoundType, List<BracketMatch>> getAllKnockoutRounds(Map<UUID, int[]> scores, UUID tournamentId) {
        Map<RoundType, List<BracketMatch>> rounds = new LinkedHashMap<>();

        List<BracketMatch> r32 = getKnockoutBracket(scores, tournamentId);
        rounds.put(RoundType.ROUND_OF_32, r32);

        Map<Integer, String> winnerCache = new HashMap<>();
        for (BracketMatch m : r32) {
            int[] s = scores.get(matchIdByNumber(m.getMatchNumber()));
            if (s != null) winnerCache.put(m.getMatchNumber(), team1Wins(s) ? m.getTeam1Name() : m.getTeam2Name());
        }

        List<Match> knockoutTemplates = matchRepository.findByRoundInOrderByMatchDateAsc(
                List.of(RoundType.ROUND_OF_16, RoundType.QUARTER_FINAL,
                        RoundType.SEMI_FINAL, RoundType.THIRD_PLACE, RoundType.FINAL));

        Object[][] sources = {
            {74,77,89},{73,75,90},{76,78,91},{79,80,92},{83,84,93},{81,82,94},{86,88,95},{85,87,96},
            {89,90,97},{93,94,98},{91,92,99},{95,96,100},
            {97,98,101},{99,100,102},
            {101,102,103,true},{101,102,104,false}
        };
        int si = 0;

        for (Match tmpl : knockoutTemplates) {
            Object[] src = sources[si++];
            int src1 = (int) src[0], src2 = (int) src[1], dest = (int) src[2];
            boolean isThirdPlace = src.length > 3 && (boolean) src[3];

            String t1Name = winnerName(src1, winnerCache, scores, isThirdPlace);
            String t2Name = winnerName(src2, winnerCache, scores, isThirdPlace);

            BracketMatch bm = new BracketMatch(dest, tmpl.getMatchDate(),
                    tmpl.getVenue() != null ? tmpl.getVenue() : "", t1Name, t2Name);
            rounds.computeIfAbsent(tmpl.getRound(), k -> new ArrayList<>()).add(bm);

            int[] destScores = scores.get(tmpl.getId());
            if (destScores != null) {
                winnerCache.put(dest, team1Wins(destScores) ? t1Name : t2Name);
            }
        }
        return rounds;
    }

    private String winnerName(int matchNum, Map<Integer, String> cache,
                               Map<UUID, int[]> scores, boolean isLoser) {
        String cached = cache.get(matchNum);
        if (cached != null) {
            if (!isLoser) return cached;
            var match = matchRepository.findAll().stream()
                    .filter(m -> m.getMatchNumber() == matchNum).findFirst().orElse(null);
            if (match != null) {
                int[] s = scores.get(match.getId());
                if (s != null) {
                    boolean t1Won = team1Wins(s);
                    String loser = t1Won ? match.getTeam2() != null ? match.getTeam2().getName() : null
                                         : match.getTeam1() != null ? match.getTeam1().getName() : null;
                    if (loser != null) return loser;
                }
            }
        }
        return "Winner Match " + matchNum;
    }

    @Transactional
    public Map<RoundType, List<BracketMatch>> getActualAllKnockoutRounds(UUID tournamentId) {
        Map<UUID, int[]> actualScores = new HashMap<>();
        Map<Integer, UUID> numToId = new HashMap<>();
        for (Match m : matchRepository.findByTournamentId(tournamentId)) {
            numToId.put(m.getMatchNumber(), m.getId());
            if (m.getTeam1Score() != null && m.getTeam2Score() != null) {
                if (m.getTeam1PenaltiesScore() != null) {
                    actualScores.put(m.getId(), new int[]{m.getTeam1Score(), m.getTeam2Score(),
                            m.getTeam1PenaltiesScore(), m.getTeam2PenaltiesScore()});
                } else {
                    actualScores.put(m.getId(), new int[]{m.getTeam1Score(), m.getTeam2Score()});
                }
            }
        }

        Map<Character, List<GroupStanding>> standings =
                computeStandings(actualScores, tournamentId);
        Map<Character, Team> winners = new HashMap<>();
        Map<Character, Team> runnersUp = new HashMap<>();
        for (var entry : standings.entrySet()) {
            char g = entry.getKey();
            List<GroupStanding> gs = entry.getValue();
            if (gs.size() >= 1) winners.put(g, gs.get(0).getTeam());
            if (gs.size() >= 2) runnersUp.put(g, gs.get(1).getTeam());
        }

        List<String> thirdDesc = combinationService.getPossibleThirdPlaceDescriptions();
        List<Character> fpGroups = combinationService.getFirstPlaceGroups();
        Map<Integer, String[]> descByMatch = buildDescByMatch(thirdDesc, fpGroups);

        List<BracketMatch> r32 = new ArrayList<>();
        List<Match> r32Templates = matchRepository.findByRoundOrderByMatchDateAsc(RoundType.ROUND_OF_32);
        for (Match tmpl : r32Templates) {
            int num = tmpl.getMatchNumber();
            String[] descs = descByMatch.getOrDefault(num, new String[]{"?", "?"});
            String t1Name = resolveByDesc(num, descs[0], winners, runnersUp, standings,
                    fpGroups, thirdDesc, actualScores);
            String t2Name = resolveByDesc(num, descs[1], winners, runnersUp, standings,
                    fpGroups, thirdDesc, actualScores);
            r32.add(new BracketMatch(num, tmpl.getMatchDate(),
                    tmpl.getVenue() != null ? tmpl.getVenue() : "", t1Name, t2Name));
        }

        return buildLaterRounds(r32, actualScores, numToId, tournamentId);
    }

    private Map<Integer, String[]> buildDescByMatch(List<String> thirdDesc, List<Character> fpGroups) {
        Map<Integer, String[]> map = new LinkedHashMap<>();
        map.put(73, new String[]{"2A", "2B"});
        map.put(78, new String[]{"2E", "2I"});
        map.put(83, new String[]{"2K", "2L"});
        map.put(88, new String[]{"2D", "2G"});
        map.put(75, new String[]{"1F", "2C"});
        map.put(76, new String[]{"1C", "2F"});
        map.put(84, new String[]{"1H", "2J"});
        map.put(86, new String[]{"1J", "2H"});
        int[][] winnerThirdSlots = {
            {74, 3}, {77, 5}, {79, 0}, {80, 7}, {81, 2}, {82, 4}, {85, 1}, {87, 6}
        };
        for (int[] slot : winnerThirdSlots) {
            int matchNum = slot[0];
            int fpIdx = slot[1];
            char group = fpGroups.get(fpIdx);
            map.put(matchNum, new String[]{"1" + group, "3" + thirdDesc.get(fpIdx)});
        }
        return map;
    }

    private String resolveByDesc(int matchNum, String desc,
                                  Map<Character, Team> winners, Map<Character, Team> runnersUp,
                                  Map<Character, List<GroupStanding>> standings,
                                  List<Character> fpGroups, List<String> thirdDesc,
                                  Map<UUID, int[]> scores) {
        if (desc.startsWith("1")) {
            char g = desc.charAt(1);
            Team t = winners.get(g);
            return teamOrDesc(t, desc);
        }
        if (desc.startsWith("2")) {
            char g = desc.charAt(1);
            Team t = runnersUp.get(g);
            return teamOrDesc(t, desc);
        }
        if (desc.startsWith("3")) {
            for (int i = 0; i < fpGroups.size(); i++) {
                if (desc.substring(1).equals(thirdDesc.get(i))) {
                    return thirdPlaceOrActual(fpGroups.get(i), standings, scores,
                            thirdDesc.get(i), desc);
                }
            }
        }
        return desc;
    }

    private String teamOrDesc(Team team, String desc) {
        return team != null ? team.getName() : desc;
    }

    private String thirdPlaceOrActual(char winnerGroup, Map<Character, List<GroupStanding>> standings,
                                        Map<UUID, int[]> scores, String thirdSlotGroup, String desc) {
        List<GroupStanding> bestThirds = getBestThirdPlacedTeams(standings);
        Set<Character> thirdGroups = bestThirds.stream().map(GroupStanding::getGroupLetter).collect(Collectors.toSet());
        if (thirdGroups.size() < 8) return desc;
        var opt = combinationService.findOption(thirdGroups);
        if (opt.isEmpty()) return desc;
        List<Character> slotGroups = combinationService.getThirdPlaceGroupsForOption(opt.get());
        int slotIdx = combinationService.getFirstPlaceGroups().indexOf(winnerGroup);
        if (slotIdx < 0 || slotIdx >= slotGroups.size()) return desc;
        char actualGroup = slotGroups.get(slotIdx);
        for (GroupStanding gs : bestThirds) {
            if (gs.getGroupLetter() == actualGroup) return gs.getTeam().getName();
        }
        return desc;
    }

    private BracketMatch ruVsRu(int num, LocalDateTime date, String venue,
                                  Map<Character, Team> runnersUp, char g1, char g2) {
        Team t1 = runnersUp.get(g1), t2 = runnersUp.get(g2);
        return new BracketMatch(num, date, venue,
                teamOrDesc(t1, "2" + g1), teamOrDesc(t2, "2" + g2));
    }

    private BracketMatch wnVsRu(int num, LocalDateTime date, String venue,
                                  Map<Character, Team> winners, char wg,
                                  Map<Character, Team> runnersUp, char rg) {
        Team t1 = winners.get(wg), t2 = runnersUp.get(rg);
        return new BracketMatch(num, date, venue,
                teamOrDesc(t1, "1" + wg), teamOrDesc(t2, "2" + rg));
    }

    private Map<RoundType, List<BracketMatch>> buildLaterRounds(List<BracketMatch> r32,
                                                                  Map<UUID, int[]> scores, Map<Integer, UUID> numToId,
                                                                  UUID tournamentId) {
        Map<RoundType, List<BracketMatch>> rounds = new LinkedHashMap<>();
        rounds.put(RoundType.ROUND_OF_32, r32);

        Map<Integer, String> winnerCache = new HashMap<>();
        for (BracketMatch m : r32) {
            UUID mid = numToId.get(m.getMatchNumber());
            int[] s = scores.get(mid);
            if (s != null) {
                winnerCache.put(m.getMatchNumber(), team1Wins(s) ? m.getTeam1Name() : m.getTeam2Name());
            }
        }

        List<Match> knockoutTemplates = matchRepository.findByRoundInOrderByMatchDateAsc(
                List.of(RoundType.ROUND_OF_16, RoundType.QUARTER_FINAL,
                        RoundType.SEMI_FINAL, RoundType.THIRD_PLACE, RoundType.FINAL));

        Object[][] sources = {
            {74,77,89},{73,75,90},{76,78,91},{79,80,92},{83,84,93},{81,82,94},{86,88,95},{85,87,96},
            {89,90,97},{93,94,98},{91,92,99},{95,96,100},
            {97,98,101},{99,100,102},
            {101,102,103,true},{101,102,104,false}
        };
        int si = 0;

        for (Match tmpl : knockoutTemplates) {
            Object[] src = sources[si++];
            int src1 = (int) src[0], src2 = (int) src[1], dest = (int) src[2];
            boolean isThirdPlace = src.length > 3 && (boolean) src[3];

            String t1Name = winnerOrDesc(src1, winnerCache, scores, isThirdPlace, false);
            String t2Name = winnerOrDesc(src2, winnerCache, scores, isThirdPlace, true);

            BracketMatch bm = new BracketMatch(dest, tmpl.getMatchDate(),
                    tmpl.getVenue() != null ? tmpl.getVenue() : "", t1Name, t2Name);
            rounds.computeIfAbsent(tmpl.getRound(), k -> new ArrayList<>()).add(bm);

            int[] destScores = scores.get(tmpl.getId());
            if (destScores != null) {
                winnerCache.put(dest, team1Wins(destScores) ? t1Name : t2Name);
            }
        }
        return rounds;
    }

    private String winnerOrDesc(int matchNum, Map<Integer, String> cache,
                                 Map<UUID, int[]> scores, boolean isLoser, boolean isSecond) {
        String cached = cache.get(matchNum);
        if (cached != null) {
            if (!isLoser) return cached;
            var match = matchRepository.findAll().stream()
                    .filter(m -> m.getMatchNumber() == matchNum).findFirst().orElse(null);
            if (match != null) {
                int[] s = scores.get(match.getId());
                if (s != null) {
                    boolean t1Won = team1Wins(s);
                    String loser = t1Won ? (match.getTeam2() != null ? match.getTeam2().getName() : null)
                                         : (match.getTeam1() != null ? match.getTeam1().getName() : null);
                    if (loser != null) return loser;
                }
            }
        }
        String prefix = isLoser ? "L" : "W";
        return prefix + matchNum;
    }

    private boolean team1Wins(int[] scores) {
        if (scores[0] != scores[1]) return scores[0] > scores[1];
        if (scores.length >= 4) return scores[2] > scores[3];
        return false;
    }

    private UUID matchIdByNumber(int matchNum) {
        return matchRepository.findAll().stream()
                .filter(m -> m.getMatchNumber() == matchNum).findFirst()
                .map(Match::getId).orElse(null);
    }

    @Transactional
    public void propagateWinner(UUID matchId) {
        Match match = matchRepository.findById(matchId).orElse(null);
        if (match == null || match.getMatchNumber() < 73) return;
        if (match.getTeam1Score() == null || match.getTeam2Score() == null) return;

        int[] scores;
        if (match.getTeam1PenaltiesScore() != null) {
            scores = new int[]{match.getTeam1Score(), match.getTeam2Score(),
                    match.getTeam1PenaltiesScore(), match.getTeam2PenaltiesScore()};
        } else {
            scores = new int[]{match.getTeam1Score(), match.getTeam2Score()};
        }
        boolean t1Win = team1Wins(scores);

        Team winner;
        if (match.getTeam1() != null && match.getTeam2() != null) {
            winner = t1Win ? match.getTeam1() : match.getTeam2();
        } else {
            Team t1 = resolveBracketTeam(match.getMatchNumber(), true);
            Team t2 = resolveBracketTeam(match.getMatchNumber(), false);
            winner = t1Win ? t1 : t2;
        }
        if (winner == null) return;

        int[][] sources = {
            {74,77,89},{73,75,90},{76,78,91},{79,80,92},{83,84,93},{81,82,94},{86,88,95},{85,87,96},
            {89,90,97},{93,94,98},{91,92,99},{95,96,100},
            {97,98,101},{99,100,102},
            {101,102,103,1},{101,102,104,0}
        };

        int matchNum = match.getMatchNumber();
        for (int[] src : sources) {
            boolean isLoser = src.length > 3 && src[3] == 1;
            int destNum = src[2];
            if (src[0] == matchNum) {
                Team t = isLoser ? getLoser(match, winner) : winner;
                if (t != null) setTeam(destNum, true, t);
            }
            if (src[1] == matchNum) {
                Team t = isLoser ? getLoser(match, winner) : winner;
                if (t != null) setTeam(destNum, false, t);
            }
        }
    }

    private Team resolveBracketTeam(int matchNum, boolean isTeam1) {
        UUID tournamentId = tournamentTeamRepo.findAll().stream()
                .findFirst().map(TournamentTeam::getTournament).map(Tournament::getId).orElse(null);
        if (tournamentId == null) return null;

        Map<UUID, int[]> scores = new HashMap<>();
        for (Match m : matchRepository.findByTournamentId(tournamentId)) {
            if (m.getTeam1Score() != null && m.getTeam2Score() != null) {
                scores.put(m.getId(), new int[]{m.getTeam1Score(), m.getTeam2Score()});
            }
        }
        Map<Character, List<GroupStanding>> standings = computeStandings(scores, tournamentId);

        List<String> thirdDesc = combinationService.getPossibleThirdPlaceDescriptions();
        List<Character> fpGroups = combinationService.getFirstPlaceGroups();
        Map<Integer, String[]> descByMatch = buildDescByMatch(thirdDesc, fpGroups);

        String[] descs = descByMatch.get(matchNum);
        if (descs == null) return null;
        String desc = isTeam1 ? descs[0] : descs[1];

        char g;
        if (desc.startsWith("1") || desc.startsWith("2")) {
            g = desc.charAt(1);
            List<GroupStanding> gs = standings.get(g);
            if (gs == null) return null;
            if (desc.startsWith("1") && gs.size() >= 1) return gs.get(0).getTeam();
            if (desc.startsWith("2") && gs.size() >= 2) return gs.get(1).getTeam();
        }
        if (desc.startsWith("3")) {
            for (int i = 0; i < fpGroups.size(); i++) {
                if (desc.substring(1).equals(thirdDesc.get(i))) {
                    char winnerGroup = fpGroups.get(i);
                    var bestThirds = getBestThirdPlacedTeams(standings);
                    var opt = combinationService.findOption(
                            bestThirds.stream().map(GroupStanding::getGroupLetter).collect(Collectors.toSet()));
                    if (opt.isPresent()) {
                        var slotGroups = combinationService.getThirdPlaceGroupsForOption(opt.get());
                        if (i < slotGroups.size()) {
                            char actualGroup = slotGroups.get(i);
                            for (GroupStanding gs : bestThirds) {
                                if (gs.getGroupLetter() == actualGroup) return gs.getTeam();
                            }
                        }
                    }
                    break;
                }
            }
        }
        return null;
    }

    private Team getLoser(Match match, Team winner) {
        if (match.getTeam1() != null && !match.getTeam1().getId().equals(winner.getId())) return match.getTeam1();
        if (match.getTeam2() != null && !match.getTeam2().getId().equals(winner.getId())) return match.getTeam2();
        return null;
    }

    private void setTeam(int destNum, boolean isTeam1, Team team) {
        Match dest = matchRepository.findAll().stream()
                .filter(m -> m.getMatchNumber() == destNum).findFirst().orElse(null);
        if (dest == null || team == null) return;
        if (isTeam1) dest.setTeam1(team);
        else dest.setTeam2(team);
        matchRepository.save(dest);
    }

    @Transactional
    public void propagateMatch(UUID matchId) {
        Match match = matchRepository.findById(matchId).orElse(null);
        if (match == null) return;
        if (match.getTeam1Score() == null || match.getTeam2Score() == null) return;
        RoundType round = match.getRound();
        if (round == RoundType.GROUP_MD3) {
            propagateGroupStage();
        } else if (match.getMatchNumber() >= 73) {
            propagateKnockoutWinner(match);
        }
    }

    @Transactional
    public void propagateRound(RoundType round) {
        if (round == RoundType.GROUP_MD3) {
            propagateGroupStage();
        } else {
            var matches = matchRepository.findByRoundOrderByMatchDateAsc(round);
            for (Match m : matches) {
                if (m.getTeam1Score() != null && m.getTeam2Score() != null
                        && m.getMatchNumber() >= 73) {
                    propagateKnockoutWinner(m);
                }
            }
        }
    }

    private void propagateGroupStage() {
        UUID tournamentId = matchRepository.findAll().stream()
                .findFirst().map(Match::getTournament).map(Tournament::getId).orElse(null);
        if (tournamentId == null) return;
        Map<UUID, int[]> scores = new HashMap<>();
        for (Match m : matchRepository.findByRoundOrderByMatchDateAsc(RoundType.GROUP_MD1)) {
            if (m.getTeam1Score() != null && m.getTeam2Score() != null)
                scores.put(m.getId(), new int[]{m.getTeam1Score(), m.getTeam2Score()});
        }
        for (Match m : matchRepository.findByRoundOrderByMatchDateAsc(RoundType.GROUP_MD2)) {
            if (m.getTeam1Score() != null && m.getTeam2Score() != null)
                scores.put(m.getId(), new int[]{m.getTeam1Score(), m.getTeam2Score()});
        }
        for (Match m : matchRepository.findByRoundOrderByMatchDateAsc(RoundType.GROUP_MD3)) {
            if (m.getTeam1Score() != null && m.getTeam2Score() != null)
                scores.put(m.getId(), new int[]{m.getTeam1Score(), m.getTeam2Score()});
        }
        Map<Character, List<GroupStanding>> standings = computeStandings(scores, tournamentId);
        List<String> thirdDesc = combinationService.getPossibleThirdPlaceDescriptions();
        List<Character> fpGroups = combinationService.getFirstPlaceGroups();
        Map<Integer, String[]> descByMatch = buildDescByMatch(thirdDesc, fpGroups);
        Map<Character, Team> winners = new HashMap<>();
        Map<Character, Team> runnersUp = new HashMap<>();
        for (var entry : standings.entrySet()) {
            char g = entry.getKey(); var gs = entry.getValue();
            if (gs.size() >= 1) winners.put(g, gs.get(0).getTeam());
            if (gs.size() >= 2) runnersUp.put(g, gs.get(1).getTeam());
        }
        List<GroupStanding> bestThirds = getBestThirdPlacedTeams(standings);
        Set<Character> thirdGroups = bestThirds.stream().map(GroupStanding::getGroupLetter).collect(Collectors.toSet());
        var opt = combinationService.findOption(thirdGroups);
        List<Character> slotGroups = opt.isPresent()
                ? combinationService.getThirdPlaceGroupsForOption(opt.get()) : List.of();
        for (var entry : descByMatch.entrySet()) {
            int matchNum = entry.getKey();
            String[] descs = entry.getValue();
            Match dest = matchRepository.findAll().stream()
                    .filter(m -> m.getMatchNumber() == matchNum).findFirst().orElse(null);
            if (dest == null) continue;
            Team t1 = resolveTeamFromDesc(descs[0], winners, runnersUp, standings, fpGroups,
                    thirdDesc, bestThirds, slotGroups);
            Team t2 = resolveTeamFromDesc(descs[1], winners, runnersUp, standings, fpGroups,
                    thirdDesc, bestThirds, slotGroups);
            if (t1 != null) dest.setTeam1(t1);
            if (t2 != null) dest.setTeam2(t2);
            if (t1 != null || t2 != null) matchRepository.save(dest);
        }
    }

    private Team resolveTeamFromDesc(String desc, Map<Character, Team> winners,
                                       Map<Character, Team> runnersUp,
                                       Map<Character, List<GroupStanding>> standings,
                                       List<Character> fpGroups, List<String> thirdDesc,
                                       List<GroupStanding> bestThirds, List<Character> slotGroups) {
        if (desc == null) return null;
        if (desc.startsWith("1")) { char g = desc.charAt(1); return winners.get(g); }
        if (desc.startsWith("2")) { char g = desc.charAt(1); return runnersUp.get(g); }
        if (desc.startsWith("3")) {
            for (int i = 0; i < fpGroups.size(); i++) {
                if (desc.substring(1).equals(thirdDesc.get(i))) {
                    if (i < slotGroups.size()) {
                        char actualGroup = slotGroups.get(i);
                        for (GroupStanding gs : bestThirds) {
                            if (gs.getGroupLetter() == actualGroup) return gs.getTeam();
                        }
                    }
                    break;
                }
            }
        }
        return null;
    }

    private void propagateKnockoutWinner(Match match) {
        int[] scores;
        if (match.getTeam1PenaltiesScore() != null) {
            scores = new int[]{match.getTeam1Score(), match.getTeam2Score(),
                    match.getTeam1PenaltiesScore(), match.getTeam2PenaltiesScore()};
        } else {
            scores = new int[]{match.getTeam1Score(), match.getTeam2Score()};
        }
        boolean t1Win = team1Wins(scores);
        Team winner;
        if (match.getTeam1() != null && match.getTeam2() != null) {
            winner = t1Win ? match.getTeam1() : match.getTeam2();
        } else {
            Team t1 = resolveBracketTeam(match.getMatchNumber(), true);
            Team t2 = resolveBracketTeam(match.getMatchNumber(), false);
            winner = t1Win ? t1 : t2;
        }
        if (winner == null) return;
        int[][] sources = {
            {74,77,89},{73,75,90},{76,78,91},{79,80,92},{83,84,93},{81,82,94},{86,88,95},{85,87,96},
            {89,90,97},{93,94,98},{91,92,99},{95,96,100},
            {97,98,101},{99,100,102},
            {101,102,103,1},{101,102,104,0}
        };
        int matchNum = match.getMatchNumber();
        for (int[] src : sources) {
            boolean isLoser = src.length > 3 && src[3] == 1;
            int destNum = src[2];
            if (src[0] == matchNum) {
                Team t = isLoser ? getLoser(match, winner) : winner;
                if (t != null) setTeam(destNum, true, t);
            }
            if (src[1] == matchNum) {
                Team t = isLoser ? getLoser(match, winner) : winner;
                if (t != null) setTeam(destNum, false, t);
            }
        }
    }
}
