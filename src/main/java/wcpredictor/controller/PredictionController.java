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

    public PredictionController(MatchService matchService, PredictionService predictionService,
                                 TeamService teamService, UserService userService) {
        this.matchService = matchService;
        this.predictionService = predictionService;
        this.teamService = teamService;
        this.userService = userService;
    }

    private User getCurrentUser(Principal principal) {
        return userService.findByEmailAddress(principal.getName()).orElseThrow();
    }

    @GetMapping("/predict")
    public String predictDashboard(Model model, Principal principal) {
        User user = getCurrentUser(principal);
        model.addAttribute("hasTournamentPrediction", predictionService.getUserTournamentPrediction(user.getId()).isPresent());
        model.addAttribute("hasGroupPredictions", predictionService.hasGroupPredictions(user.getId()));
        model.addAttribute("standings", predictionService.getUserGroupStandings(user.getId()));
        return "predict";
    }

    @GetMapping("/predict/tournament")
    public String tournamentPredictions(Model model, Principal principal) {
        User user = getCurrentUser(principal);
        var existing = predictionService.getUserTournamentPrediction(user.getId());
        model.addAttribute("prediction", existing.orElse(new TournamentPrediction()));
        model.addAttribute("teams", teamService.getAllTeams());
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
                                             Principal principal) {
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
        List<Team> allTeams = teamService.getGroupedTeams();

        Map<Character, List<Team>> groups = new LinkedHashMap<>();
        for (Team t : allTeams) {
            if (t.getGroupLetter() != null) {
                groups.computeIfAbsent(t.getGroupLetter().charAt(0), k -> new ArrayList<>()).add(t);
            }
        }
        model.addAttribute("groups", groups);

        var predictions = predictionService.getUserGroupAdvancementPredictions(user.getId());
        model.addAttribute("predictedTeamIds", predictions.stream()
                .map(p -> p.getTeam().getId().toString()).collect(Collectors.toSet()));

        return "predict-group";
    }

    @PostMapping("/predict/group")
    public String saveGroupPredictions(@RequestParam("teamId") List<UUID> teamIds, Principal principal) {
        User user = getCurrentUser(principal);
        predictionService.saveGroupAdvancementPredictions(user, teamIds);
        return "redirect:/predict?groupSaved";
    }

    @GetMapping("/predict/group/{group}")
    public String groupMatchPredictions(@PathVariable char group,
                                         @RequestParam(required = false, defaultValue = "GROUP_MD1") String round,
                                         Model model, Principal principal) {
        User user = getCurrentUser(principal);
        var matches = matchService.getMatchesByGroup(String.valueOf(group));
        model.addAttribute("group", group);
        model.addAttribute("matches", matches);
        model.addAttribute("selectedRound", round);
        model.addAttribute("rounds", List.of("GROUP_MD1", "GROUP_MD2", "GROUP_MD3"));
        model.addAttribute("scores", predictionService.getUserMatchPredictionScores(user.getId()));
        return "predict-group-matches";
    }

    @PostMapping("/predict/group/{group}/save")
    public String saveGroupMatchPrediction(@PathVariable char group,
                                            @RequestParam UUID matchId,
                                            @RequestParam int team1Score,
                                            @RequestParam int team2Score,
                                            @RequestParam String redirect,
                                            Principal principal) {
        User user = getCurrentUser(principal);
        predictionService.saveMatchPrediction(user, matchId, team1Score, team2Score);
        return "redirect:" + redirect;
    }
}
