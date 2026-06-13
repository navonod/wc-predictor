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
public class SimulationService {

    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);
    private static final Random RANDOM = new Random();

    private final MatchService matchService;
    private final TeamService teamService;
    private final MatchRepository matchRepository;
    private final SimulatedMatchRepository simulatedRepo;
    private final TournamentService tournamentService;
    private final KnockoutBracketService bracketService;

    public SimulationService(MatchService matchService, TeamService teamService,
                              MatchRepository matchRepository,
                              SimulatedMatchRepository simulatedRepo,
                              TournamentService tournamentService,
                              KnockoutBracketService bracketService) {
        this.matchService = matchService;
        this.teamService = teamService;
        this.matchRepository = matchRepository;
        this.simulatedRepo = simulatedRepo;
        this.tournamentService = tournamentService;
        this.bracketService = bracketService;
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
        return computeStandings(getUserScores(user), getFirstTournamentId());
    }

    public List<GroupStanding> getUserBestThirds(User user) {
        return getBestThirdPlacedTeams(getUserStandings(user));
    }

    public List<BracketMatch> getKnockoutBracket(User user) {
        return bracketService.getKnockoutBracket(getUserScores(user), getFirstTournamentId());
    }

    private UUID getFirstTournamentId() {
        return tournamentService.findAll().stream().findFirst()
                .map(Tournament::getId).orElse(null);
    }

    private Map<Character, List<GroupStanding>> computeStandings(Map<UUID, int[]> scores, UUID tournamentId) {
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
}
