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

    public PredictionController(MatchService matchService, PredictionService predictionService,
                                 TeamService teamService, UserService userService,
                                 TournamentService tournamentService) {
        this.matchService = matchService;
        this.predictionService = predictionService;
        this.teamService = teamService;
        this.userService = userService;
        this.tournamentService = tournamentService;
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
                                            Principal principal,
                                            RedirectAttributes ra) {
        User user = getCurrentUser(principal);
        try {
            predictionService.saveMatchPrediction(user, matchId, team1Score, team2Score);
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:" + redirect;
        }
        return "redirect:" + redirect;
    }
}
