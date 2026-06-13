package wcpredictor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import wcpredictor.entity.Match;
import wcpredictor.entity.Team;
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

    private Map<Character, List<GroupStanding>> computeStandings(Map<UUID, int[]> scores, UUID tournamentId) {
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

    private List<GroupStanding> getBestThirdPlacedTeams(Map<Character, List<GroupStanding>> standings) {
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
            if (s != null) winnerCache.put(m.getMatchNumber(), s[0] > s[1] ? m.getTeam1Name() : m.getTeam2Name());
        }

        List<Match> knockoutTemplates = matchRepository.findByRoundInOrderByMatchDateAsc(
                List.of(RoundType.ROUND_OF_16, RoundType.QUARTER_FINAL,
                        RoundType.SEMI_FINAL, RoundType.THIRD_PLACE, RoundType.FINAL));

        int[][] sources = {
            {74,77,90},{73,75,89},{76,78,91},{79,80,92},{83,84,93},{81,82,94},{86,88,95},{85,87,96},
            {89,90,97},{93,94,98},{91,92,99},{95,96,100},
            {97,98,101},{99,100,102},
            {101,102,103,true},{101,102,104,false}
        };
        int si = 0;

        for (Match tmpl : knockoutTemplates) {
            int[] src = sources[si++];
            int src1 = src[0], src2 = src[1], dest = src[2];
            boolean isThirdPlace = src.length > 3 && (boolean) src[3];

            String t1Name = winnerName(src1, winnerCache, scores, isThirdPlace);
            String t2Name = winnerName(src2, winnerCache, scores, isThirdPlace);

            BracketMatch bm = new BracketMatch(dest, tmpl.getMatchDate(),
                    tmpl.getVenue() != null ? tmpl.getVenue() : "", t1Name, t2Name);
            rounds.computeIfAbsent(tmpl.getRound(), k -> new ArrayList<>()).add(bm);

            int[] destScores = scores.get(tmpl.getId());
            if (destScores != null && dest.isThirdPlace()) {
                winnerCache.put(dest, destScores[0] > destScores[1] ? t1Name : t2Name);
            } else if (destScores != null) {
                winnerCache.put(dest, destScores[0] > destScores[1] ? t1Name : t2Name);
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
                    boolean t1Won = s[0] > s[1];
                    String loser = t1Won ? match.getTeam2() != null ? match.getTeam2().getName() : null
                                         : match.getTeam1() != null ? match.getTeam1().getName() : null;
                    if (loser != null) return loser;
                }
            }
        }
        return "Winner Match " + matchNum;
    }

    private UUID matchIdByNumber(int matchNum) {
        return matchRepository.findAll().stream()
                .filter(m -> m.getMatchNumber() == matchNum).findFirst()
                .map(Match::getId).orElse(null);
    }
}
