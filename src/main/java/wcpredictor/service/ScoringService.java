package wcpredictor.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wcpredictor.entity.*;
import wcpredictor.repository.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScoringService {

    private final MatchRepository matchRepository;
    private final MatchPredictionRepository matchPredictionRepo;
    private final TournamentPredictionRepository tournamentPredictionRepo;
    private final GroupAdvancementPredictionRepository groupAdvancementPredictionRepo;
    private final UserRepository userRepository;
    private final SettingRepository settingRepository;

    public ScoringService(MatchRepository matchRepository,
                          MatchPredictionRepository matchPredictionRepo,
                          TournamentPredictionRepository tournamentPredictionRepo,
                          GroupAdvancementPredictionRepository groupAdvancementPredictionRepo,
                          UserRepository userRepository,
                          SettingRepository settingRepository) {
        this.matchRepository = matchRepository;
        this.matchPredictionRepo = matchPredictionRepo;
        this.tournamentPredictionRepo = tournamentPredictionRepo;
        this.groupAdvancementPredictionRepo = groupAdvancementPredictionRepo;
        this.userRepository = userRepository;
        this.settingRepository = settingRepository;
    }

    public double scoreMatch(MatchPrediction prediction) {
        Match match = prediction.getMatch();
        Integer s1 = match.getTeam1Score();
        Integer s2 = match.getTeam2Score();
        Integer p1 = prediction.getTeam1Score();
        Integer p2 = prediction.getTeam2Score();

        if (s1 == null || s2 == null || p1 == null || p2 == null) return 0;

        int scoreDiff = s1 - s2;
        int predictDiff = p1 - p2;

        if (s1.equals(p1) && s2.equals(p2)) return getDoubleSetting("points_correct_score");
        if (scoreDiff != 0 && scoreDiff == predictDiff) return getDoubleSetting("points_correct_margin");
        if (s1.equals(s2) && p1.equals(p2)) return getDoubleSetting("points_draw_diff");
        if ((scoreDiff > 0 && predictDiff > 0) || (scoreDiff < 0 && predictDiff < 0))
            return getDoubleSetting("points_correct_result");
        return 0;
    }

    public Map<UUID, Double> calculateMatchScoresForUser(UUID userId) {
        Map<UUID, Double> scores = new LinkedHashMap<>();
        List<MatchPrediction> predictions = matchPredictionRepo.findByUserId(userId);
        for (MatchPrediction p : predictions) {
            if (p.getMatch().getTeam1Score() != null) {
                scores.put(p.getMatch().getId(), scoreMatch(p));
            }
        }
        return scores;
    }

    public double getTotalMatchPoints(UUID userId) {
        return calculateMatchScoresForUser(userId).values().stream()
                .mapToDouble(Double::doubleValue).sum();
    }

    public Map<String, Double> calculateTournamentAwardPoints(UUID userId) {
        Map<String, Double> points = new LinkedHashMap<>();
        var tpOpt = tournamentPredictionRepo.findByUserId(userId);
        if (tpOpt.isEmpty()) return points;

        TournamentPrediction tp = tpOpt.get();
        points.put("goldenBoot", tp.getGoldenBoot() != null ? getDoubleSetting("points_golden_boot") : 0);
        points.put("goldenBall", tp.getGoldenBall() != null ? getDoubleSetting("points_golden_ball") : 0);
        points.put("goldenGlove", tp.getGoldenGlove() != null ? getDoubleSetting("points_golden_glove") : 0);
        points.put("youngPlayer", tp.getYoungPlayer() != null ? getDoubleSetting("points_young_player") : 0);
        points.put("fairPlay", tp.getFairPlay() != null ? getDoubleSetting("points_fair_play") : 0);
        points.put("entertaining", tp.getMostEntertaining() != null ? getDoubleSetting("points_entertaining") : 0);
        points.put("finalist1", tp.getFinalist1() != null ? getDoubleSetting("points_finalist") : 0);
        points.put("finalist2", tp.getFinalist2() != null ? getDoubleSetting("points_finalist") : 0);
        points.put("champion", tp.getChampion() != null ? getDoubleSetting("points_champion") : 0);

        return points;
    }

    public double getTotalAwardPoints(UUID userId) {
        return calculateTournamentAwardPoints(userId).values().stream()
                .mapToDouble(Double::doubleValue).sum();
    }

    public double calculateGroupAdvancementPoints(UUID userId) {
        var predictions = groupAdvancementPredictionRepo.findByUserId(userId);
        double points = 0;
        double perCorrect = getDoubleSetting("points_group_qualifier");
        for (var pred : predictions) {
            points += perCorrect;
        }
        return points;
    }

    public double getTotalPoints(UUID userId) {
        return getTotalMatchPoints(userId) + getTotalAwardPoints(userId) + calculateGroupAdvancementPoints(userId);
    }

    public List<UserScoreSummary> getLeaderboard() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .filter(u -> !u.isAdmin())
                .map(u -> {
                    UserScoreSummary s = new UserScoreSummary();
                    s.setUserId(u.getId());
                    s.setDisplayName(getDisplayName(u));
                    s.setMatchPoints(getTotalMatchPoints(u.getId()));
                    s.setAwardPoints(getTotalAwardPoints(u.getId()));
                    s.setGroupPoints(calculateGroupAdvancementPoints(u.getId()));
                    s.setTotalPoints(s.getMatchPoints() + s.getAwardPoints() + s.getGroupPoints());
                    return s;
                })
                .sorted((a, b) -> Double.compare(b.getTotalPoints(), a.getTotalPoints()))
                .collect(Collectors.toList());
    }

    private String getDisplayName(User user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) return user.getNickname();
        if (user.getFirstName() != null && !user.getFirstName().isBlank())
            return user.getFirstName() + (user.getLastName() != null ? " " + user.getLastName() : "");
        return user.getEmailAddress();
    }

    private double getDoubleSetting(String name) {
        return settingRepository.findByName(name)
                .map(s -> Double.parseDouble(s.getValue())).orElse(0.0);
    }

    public static class UserScoreSummary {
        private UUID userId;
        private String displayName;
        private double matchPoints;
        private double awardPoints;
        private double groupPoints;
        private double totalPoints;

        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public double getMatchPoints() { return matchPoints; }
        public void setMatchPoints(double matchPoints) { this.matchPoints = matchPoints; }
        public double getAwardPoints() { return awardPoints; }
        public void setAwardPoints(double awardPoints) { this.awardPoints = awardPoints; }
        public double getGroupPoints() { return groupPoints; }
        public void setGroupPoints(double groupPoints) { this.groupPoints = groupPoints; }
        public double getTotalPoints() { return totalPoints; }
        public void setTotalPoints(double totalPoints) { this.totalPoints = totalPoints; }
    }
}
