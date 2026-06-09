package wcpredictor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wcpredictor.entity.*;
import wcpredictor.repository.MatchRepository;
import wcpredictor.repository.SimulatedMatchRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupStageSimulationService {

    private static final Logger log = LoggerFactory.getLogger(GroupStageSimulationService.class);
    private static final Random RANDOM = new Random();

    private final MatchService matchService;
    private final TeamService teamService;
    private final MatchRepository matchRepository;
    private final SimulatedMatchRepository simulatedRepo;
    private final RoundOf32CombinationService combinationService;
    private final TournamentService tournamentService;

    public GroupStageSimulationService(MatchService matchService, TeamService teamService,
                                        MatchRepository matchRepository,
                                        SimulatedMatchRepository simulatedRepo,
                                        RoundOf32CombinationService combinationService,
                                        TournamentService tournamentService) {
        this.matchService = matchService;
        this.teamService = teamService;
        this.matchRepository = matchRepository;
        this.simulatedRepo = simulatedRepo;
        this.combinationService = combinationService;
        this.tournamentService = tournamentService;
    }

    private UUID getFirstTournamentId() {
        return tournamentService.findAll().stream().findFirst()
                .map(Tournament::getId).orElse(null);
    }

    @Transactional
    public void randomize(User user) {
        List<RoundType> groupRounds = List.of(RoundType.GROUP_MD1, RoundType.GROUP_MD2, RoundType.GROUP_MD3);
        for (RoundType round : groupRounds) {
            for (Match match : matchService.getMatchesByRound(round)) {
                SimulatedMatch sm = simulatedRepo.findByUserAndMatch(user, match)
                        .orElse(new SimulatedMatch());
                sm.setUser(user);
                sm.setMatch(match);
                sm.setTeam1Score(weightedRandomScore());
                sm.setTeam2Score(weightedRandomScore());
                sm.setTimestamp(Instant.now());
                simulatedRepo.save(sm);
            }
        }
        log.info("Randomized simulation for user {}", user.getEmailAddress());
    }

    @Transactional
    public void saveScore(User user, UUID matchId, int team1Score, int team2Score) {
        Match match = matchService.findById(matchId).orElseThrow();
        SimulatedMatch sm = simulatedRepo.findByUserAndMatch(user, match)
                .orElse(new SimulatedMatch());
        sm.setUser(user);
        sm.setMatch(match);
        sm.setTeam1Score(team1Score);
        sm.setTeam2Score(team2Score);
        sm.setTimestamp(Instant.now());
        simulatedRepo.save(sm);
    }

    @Transactional
    public void clearSimulation(User user) {
        simulatedRepo.deleteByUser(user);
        log.info("Cleared simulation for user {}", user.getEmailAddress());
    }

    public Map<UUID, int[]> getUserScores(User user) {
        Map<UUID, int[]> scores = new HashMap<>();
        for (SimulatedMatch sm : simulatedRepo.findByUser(user)) {
            scores.put(sm.getMatch().getId(),
                    new int[]{sm.getTeam1Score(), sm.getTeam2Score()});
        }
        return scores;
    }

    public boolean hasSimulation(User user) {
        return !simulatedRepo.findByUser(user).isEmpty();
    }

    public Map<Character, List<GroupStanding>> getUserStandings(User user) {
        Map<UUID, int[]> scores = getUserScores(user);
        Map<Character, List<GroupStanding>> standings = new LinkedHashMap<>();
        UUID tournamentId = getFirstTournamentId();

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

    public List<GroupStanding> getUserBestThirds(User user) {
        Map<Character, List<GroupStanding>> standings = getUserStandings(user);
        return getBestThirdPlacedTeams(standings);
    }

    public List<GroupStanding> getBestThirdPlacedTeams(Map<Character, List<GroupStanding>> standings) {
        List<GroupStanding> thirdPlaced = new ArrayList<>();
        for (var entry : standings.entrySet()) {
            List<GroupStanding> group = entry.getValue();
            if (group.size() >= 3) {
                thirdPlaced.add(group.get(2));
            }
        }
        thirdPlaced.sort(Comparator.naturalOrder());
        return thirdPlaced.subList(0, Math.min(8, thirdPlaced.size()));
    }

    public List<BracketMatch> getKnockoutBracket(User user) {
        Map<Character, List<GroupStanding>> standings = getUserStandings(user);
        List<GroupStanding> bestThirds = getBestThirdPlacedTeams(standings);
        if (bestThirds.size() < 8) return List.of();

        Set<Character> thirdPlaceGroups = bestThirds.stream()
                .map(gs -> gs.getGroupLetter())
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
        int matchNum = 73;

        // 73-80: 1A,1B,1D,1E,1G,1I,1K,1L vs 3rd-place (combination-dependent)
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
                bracket.add(new BracketMatch(matchNum + i, dates1[i], venues1[i], t1.getName(), t2.getName()));
            }
        }

        // 81: 2A vs 2B
        addIf(bracket, 81, LocalDateTime.of(2026, 6, 28, 16, 0),
                "Gillette Stadium, Boston", runnersUp, 'A', 'B');

        // 82: 2C vs 2F
        addIf(bracket, 82, LocalDateTime.of(2026, 6, 29, 20, 0),
                "Arrowhead Stadium, Kansas City", runnersUp, 'C', 'F');

        // 83: 2D vs 2E
        addIf(bracket, 83, LocalDateTime.of(2026, 6, 30, 16, 0),
                "SoFi Stadium, Los Angeles", runnersUp, 'D', 'E');

        // 84: 2H vs 2J
        addIf(bracket, 84, LocalDateTime.of(2026, 7, 1, 16, 0),
                "Lincoln Financial Field, Philadelphia", runnersUp, 'H', 'J');

        // 85: 2G vs 2I
        addIf(bracket, 85, LocalDateTime.of(2026, 7, 2, 13, 0),
                "BMO Field, Toronto", runnersUp, 'G', 'I');

        // 86: 2K vs 2L
        addIf(bracket, 86, LocalDateTime.of(2026, 7, 2, 20, 0),
                "BC Place, Vancouver", runnersUp, 'K', 'L');

        // 87: 1C vs 2J
        addWinnerRunner(bracket, 87, LocalDateTime.of(2026, 7, 3, 13, 0),
                "AT&T Stadium, Dallas", winners, 'C', runnersUp, 'J');

        // 88: 1F vs 2H
        addWinnerRunner(bracket, 88, LocalDateTime.of(2026, 7, 3, 20, 0),
                "NRG Stadium, Houston", winners, 'F', runnersUp, 'H');

        log.info("Built knockout bracket for user {}: option {}, {} matches",
                user.getEmailAddress(), option, bracket.size());
        return bracket;
    }

    private void addIf(List<BracketMatch> bracket, int num, LocalDateTime date,
                        String venue, Map<Character, Team> map, char g1, char g2) {
        Team t1 = map.get(g1);
        Team t2 = map.get(g2);
        if (t1 != null && t2 != null) {
            bracket.add(new BracketMatch(num, date, venue, t1.getName(), t2.getName()));
        }
    }

    private void addWinnerRunner(List<BracketMatch> bracket, int num, LocalDateTime date,
                                  String venue, Map<Character, Team> winners, char wg,
                                  Map<Character, Team> runnersUp, char rg) {
        Team t1 = winners.get(wg);
        Team t2 = runnersUp.get(rg);
        if (t1 != null && t2 != null) {
            bracket.add(new BracketMatch(num, date, venue, t1.getName(), t2.getName()));
        }
    }

    public List<Match> getUnsimulatedGroupMatches() {
        return matchRepository.findAllByOrderByMatchDateAsc().stream()
                .filter(m -> m.getGroupLetter() != null && m.getGroupLetter().length() == 1)
                .collect(Collectors.toList());
    }

    private int weightedRandomScore() {
        double roll = RANDOM.nextDouble();
        if (roll < 0.35) return 0;
        if (roll < 0.65) return 1;
        if (roll < 0.83) return 2;
        if (roll < 0.93) return 3;
        if (roll < 0.97) return 4;
        if (roll < 0.985) return 5;
        if (roll < 0.993) return 6;
        if (roll < 0.997) return 7;
        if (roll < 0.999) return 8;
        if (roll < 0.9998) return 9;
        return 10;
    }

    public static class BracketMatch {
        private final int matchNumber;
        private final LocalDateTime matchDate;
        private final String venue;
        private final String team1Name;
        private final String team2Name;

        public BracketMatch(int matchNumber, LocalDateTime matchDate, String venue,
                             String team1Name, String team2Name) {
            this.matchNumber = matchNumber;
            this.matchDate = matchDate;
            this.venue = venue;
            this.team1Name = team1Name;
            this.team2Name = team2Name;
        }

        public int getMatchNumber() { return matchNumber; }
        public LocalDateTime getMatchDate() { return matchDate; }
        public String getVenue() { return venue; }
        public String getTeam1Name() { return team1Name; }
        public String getTeam2Name() { return team2Name; }
    }
}
