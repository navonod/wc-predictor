package wcpredictor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import wcpredictor.entity.*;
import wcpredictor.service.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class PredictionController {

    private final MatchService matchService;
    private final PredictionService predictionService;
    private final TeamService teamService;
    private final UserService userService;
    private final TournamentService tournamentService;
    private final TimeService timeService;

    public PredictionController(MatchService matchService, PredictionService predictionService,
                                 TeamService teamService, UserService userService,
                                 TournamentService tournamentService, TimeService timeService) {
        this.matchService = matchService;
        this.predictionService = predictionService;
        this.teamService = teamService;
        this.userService = userService;
        this.tournamentService = tournamentService;
        this.timeService = timeService;
    }

    private boolean tournamentStarted() {
        var now = timeService.now();
        for (Match m : matchService.getAllMatches()) {
            if (m.getMatchDate() != null && now.isAfter(m.getMatchDate())) return true;
        }
        return false;
    }

    private User getCurrentUser(Principal principal) {
        return userService.findByEmailAddress(principal.getName()).orElseThrow();
    }

    @GetMapping("/predict")
    public String predictDashboard(Model model, Principal principal) {
        User user = getCurrentUser(principal);
        UUID tournamentId = tournamentService.findAll().stream().findFirst()
                .map(Tournament::getId).orElse(null);
        model.addAttribute("hasTournamentPrediction", predictionService.getUserTournamentPrediction(user.getId()).isPresent());
        model.addAttribute("hasGroupPredictions", predictionService.hasGroupPredictions(user.getId()));
        model.addAttribute("standings", predictionService.getUserGroupStandings(user.getId(), tournamentId));
        model.addAttribute("tournamentStarted", tournamentStarted());

        // Stage view fragment data for all three matchdays
        var now = timeService.now();
        var userScores = predictionService.getUserMatchPredictionScores(user.getId());
        List<RoundType> mdRounds = List.of(RoundType.GROUP_MD1, RoundType.GROUP_MD2, RoundType.GROUP_MD3);
        for (RoundType r : mdRounds) {
            var matches = matchService.getMatchesByRound(r);
            Map<UUID, Boolean> locked = new HashMap<>();
            boolean allLocked = true;
            for (Match m : matches) {
                boolean lock = m.isLocked(now);
                locked.put(m.getId(), lock);
                if (!lock) allLocked = false;
            }
            String suffix = r.name().substring(r.name().length() - 1); // "1", "2", or "3"
            model.addAttribute("matches" + suffix, matches);
            model.addAttribute("locked" + suffix, locked);
            model.addAttribute("allLocked" + suffix, allLocked);
        }
        model.addAttribute("mdScores", userScores);
        model.addAttribute("actualScores", predictionService.getActualScores());
        model.addAttribute("mdPoints", predictionService.getPointsMap(user.getId()));
        return "predict";
    }

    @GetMapping("/predict/tournament")
    public String tournamentPredictions(Model model, Principal principal) {
        User user = getCurrentUser(principal);
        var existing = predictionService.getUserTournamentPrediction(user.getId());
        model.addAttribute("prediction", existing.orElse(new TournamentPrediction()));
        model.addAttribute("teams", teamService.getAllTeams());
        model.addAttribute("tournamentStarted", tournamentStarted());
        return "predict-tournament";
    }

    @PostMapping("/predict/tournament")
    public String saveTournamentPredictions(@RequestParam(required = false) String goldenBoot,
                                             @RequestParam(required = false) String goldenBall,
                                             @RequestParam(required = false) String goldenGlove,
                                             @RequestParam(required = false) String youngPlayer,
                                             @RequestParam(required = false) String fairPlay,
                                             @RequestParam(required = false) String entertaining,
                                             @RequestParam(required = false) UUID finalist1,
                                             @RequestParam(required = false) UUID finalist2,
                                              @RequestParam(required = false) UUID champion,
                                             Principal principal, RedirectAttributes ra) {
        if (tournamentStarted()) {
            ra.addFlashAttribute("error", "The tournament has started. Tournament predictions are closed.");
            return "redirect:/predict";
        }
        User user = getCurrentUser(principal);
        TournamentPrediction tp = new TournamentPrediction();
        tp.setGoldenBoot(goldenBoot);
        tp.setGoldenBall(goldenBall);
        tp.setGoldenGlove(goldenGlove);
        tp.setYoungPlayer(youngPlayer);
        tp.setFairPlay(fairPlay);
        tp.setMostEntertaining(entertaining);
        tp.setFinalist1(finalist1 != null ? teamService.findById(finalist1).orElse(null) : null);
        tp.setFinalist2(finalist2 != null ? teamService.findById(finalist2).orElse(null) : null);
        tp.setChampion(champion != null ? teamService.findById(champion).orElse(null) : null);
        predictionService.saveTournamentPrediction(user, tp);
        return "redirect:/predict?tournamentSaved";
    }

    @GetMapping("/predict/group")
    public String groupPredictions(Model model, Principal principal) {
        User user = getCurrentUser(principal);
        UUID tournamentId = tournamentService.findAll().stream().findFirst()
                .map(wcpredictor.entity.Tournament::getId).orElse(null);
        if (tournamentId == null) {
            model.addAttribute("groups", new LinkedHashMap<>());
        } else {
            model.addAttribute("groups", teamService.getGroupedTeams(tournamentId));
        }

        var predictions = predictionService.getUserGroupAdvancementPredictions(user.getId());
        model.addAttribute("predictedTeamIds", predictions.stream()
                .map(p -> p.getTeam().getId().toString()).collect(Collectors.toSet()));
        model.addAttribute("tournamentStarted", tournamentStarted());

        return "predict-group";
    }

    @PostMapping("/predict/group")
    public String saveGroupPredictions(@RequestParam("teamId") List<UUID> teamIds, Principal principal,
                                        RedirectAttributes ra) {
        User user = getCurrentUser(principal);
        UUID tournamentId = tournamentService.findAll().stream().findFirst()
                .map(Tournament::getId).orElse(null);
        try {
            predictionService.saveGroupAdvancementPredictions(user, teamIds, tournamentId);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/predict/group";
        }
        return "redirect:/predict?groupSaved";
    }

    @GetMapping("/predict/group/{group}")
    public String groupMatchPredictionsLegacy(@PathVariable char group,
                                               @RequestParam(required = false, defaultValue = "GROUP_MD1") String round,
                                               Principal principal) {
        User user = getCurrentUser(principal);
        return "redirect:/predict/user/" + user.getId() + "/group/" + group + "?round=" + round;
    }

    @GetMapping("/predict/user/{userId}/group/{group}")
    public String groupMatchPredictions(@PathVariable UUID userId, @PathVariable char group,
                                         @RequestParam(required = false, defaultValue = "GROUP_MD1") String round,
                                         Model model, Principal principal) {
        User viewer = getCurrentUser(principal);
        User target = userService.findById(userId).orElse(null);
        if (target == null) return "redirect:/predict";

        boolean isSelf = viewer.getId().equals(userId);
        var matches = matchService.getMatchesByGroup(String.valueOf(group));
        var now = timeService.now();
        Map<UUID, Boolean> matchLocked = new HashMap<>();
        boolean allLocked = true;
        for (Match m : matches) {
            boolean locked = m.isLocked(now);
            matchLocked.put(m.getId(), locked);
            if (!locked) allLocked = false;
        }

        model.addAttribute("target", target);
        model.addAttribute("isSelf", isSelf);
        model.addAttribute("group", String.valueOf(group));
        model.addAttribute("groupChar", group);
        model.addAttribute("groups", "ABCDEFGHIJKL".chars().mapToObj(c -> String.valueOf((char) c)).toList());
        model.addAttribute("matches", matches);
        model.addAttribute("selectedRound", round);
        model.addAttribute("rounds", List.of("GROUP_MD1", "GROUP_MD2", "GROUP_MD3"));
        model.addAttribute("scores", predictionService.getUserMatchPredictionScores(target.getId()));
        model.addAttribute("matchLocked", matchLocked);
        model.addAttribute("allLocked", allLocked);
        model.addAttribute("actual", predictionService.getActualScores());
        model.addAttribute("points", predictionService.getPointsMap(target.getId()));
        return "predict-group-matches";
    }

    @PostMapping("/predict/group/{group}/save")
    public String saveGroupMatchPrediction(@PathVariable char group,
                                            @RequestParam Map<String, String> params,
                                            @RequestParam String redirect,
                                            Principal principal,
                                        RedirectAttributes ra) {
        User user = getCurrentUser(principal);
        try {
            for (var entry : params.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("score_") && key.endsWith("_1")) {
                    String matchIdStr = key.substring(6, key.length() - 2);
                    String s2 = params.get("score_" + matchIdStr + "_2");
                    if (s2 != null && !entry.getValue().isBlank() && !s2.isBlank()) {
                        UUID matchId = UUID.fromString(matchIdStr);
                        predictionService.saveMatchPrediction(user, matchId,
                                Integer.parseInt(entry.getValue()), Integer.parseInt(s2));
                    }
                }
            }
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:" + redirect;
        }
        return "redirect:" + redirect;
    }

    @PostMapping("/api/predict/match/save")
    @ResponseBody
    public Map<String, Object> ajaxSaveMatchPrediction(@RequestParam UUID matchId,
                                                        @RequestParam int team1Score,
                                                        @RequestParam int team2Score,
                                                        Principal principal) {
        User user = getCurrentUser(principal);
        try {
            predictionService.saveMatchPrediction(user, matchId, team1Score, team2Score);
            return Map.of("success", true);
        } catch (IllegalStateException e) {
            return Map.of("error", e.getMessage());
        }
    }

    @GetMapping("/predict/user/{userId}/stage/{roundType}")
    public String stagePredictions(@PathVariable UUID userId, @PathVariable String roundType,
                                    Model model, Principal principal) {
        User viewer = getCurrentUser(principal);
        User target = userService.findById(userId).orElse(null);
        if (target == null) return "redirect:/predict";

        RoundType round;
        try { round = RoundType.valueOf(roundType.toUpperCase()); }
        catch (IllegalArgumentException e) { return "redirect:/predict"; }

        boolean isSelf = viewer.getId().equals(userId);
        var matches = matchService.getMatchesByRound(round);
        var now = timeService.now();
        Map<UUID, Boolean> matchLocked = new HashMap<>();
        boolean allLocked = true;
        for (Match m : matches) {
            boolean locked = m.isLocked(now);
            matchLocked.put(m.getId(), locked);
            if (!locked) allLocked = false;
        }

        model.addAttribute("target", target);
        model.addAttribute("isSelf", isSelf);
        model.addAttribute("round", round);
        model.addAttribute("matches", matches);
        model.addAttribute("scores", predictionService.getUserMatchPredictionScores(target.getId()));
        model.addAttribute("matchLocked", matchLocked);
        model.addAttribute("allLocked", allLocked);
        model.addAttribute("actual", predictionService.getActualScores());
        model.addAttribute("points", predictionService.getPointsMap(target.getId()));
        return "predict-stage";
    }
}