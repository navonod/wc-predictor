package wcpredictor.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import wcpredictor.entity.User;
import wcpredictor.service.*;

import java.util.UUID;

@Controller
public class PlayerController {

    private static final Logger log = LoggerFactory.getLogger(PlayerController.class);

    private final UserService userService;
    private final PredictionService predictionService;
    private final ScoringService scoringService;
    private final TournamentService tournamentService;

    public PlayerController(UserService userService, PredictionService predictionService,
                             ScoringService scoringService, TournamentService tournamentService) {
        this.userService = userService;
        this.predictionService = predictionService;
        this.scoringService = scoringService;
        this.tournamentService = tournamentService;
    }

    @GetMapping("/player/{id}")
    public String viewPlayer(@PathVariable UUID id, Model model) {
        var targetOpt = userService.findById(id);
        if (targetOpt.isEmpty()) return "redirect:/leaderboard";
        var target = targetOpt.get();

        var auth = SecurityContextHolder.getContext().getAuthentication();
        var viewer = auth != null ? userService.findByEmailAddress(auth.getName()).orElse(null) : null;

        boolean isSelf = viewer != null && viewer.getId().equals(target.getId());
        boolean isAdmin = viewer != null && viewer.isAdmin();

        model.addAttribute("player", target);
        model.addAttribute("isSelf", isSelf);
        model.addAttribute("isAdmin", isAdmin);

        var prediction = predictionService.getUserTournamentPrediction(target.getId()).orElse(null);
        model.addAttribute("prediction", prediction);

        UUID tournamentId = tournamentService.findAll().stream().findFirst()
                .map(wcpredictor.entity.Tournament::getId).orElse(null);
        model.addAttribute("actual", predictionService.getOrCreateTournamentActual(tournamentId));
        model.addAttribute("settings", predictionService.getAwardSettings());

        predictionService.recalculateAllAwardPoints(tournamentId);
        prediction = predictionService.getUserTournamentPrediction(target.getId()).orElse(null);
        model.addAttribute("prediction", prediction);

        model.addAttribute("matchPoints", scoringService.getTotalMatchPoints(target.getId()));
        model.addAttribute("groupPoints", scoringService.calculateGroupAdvancementPoints(target.getId()));
        model.addAttribute("awardPoints", scoringService.getTotalAwardPoints(target.getId()));

        return "player";
    }

    @PostMapping("/player/{id}/edit")
    public String editPlayer(@PathVariable UUID id,
                              @RequestParam String nickname,
                              @RequestParam String firstName,
                              @RequestParam String lastName) {
        var playerOpt = userService.findById(id);
        if (playerOpt.isEmpty()) return "redirect:/leaderboard";
        var player = playerOpt.get();

        var auth = SecurityContextHolder.getContext().getAuthentication();
        var viewer = auth != null ? userService.findByEmailAddress(auth.getName()).orElse(null) : null;
        boolean isSelf = viewer != null && viewer.getId().equals(id);
        boolean isAdmin = viewer != null && viewer.isAdmin();

        if (!isSelf && !isAdmin) return "redirect:/leaderboard";

        player.setNickname(nickname);
        player.setFirstName(firstName);
        player.setLastName(lastName);
        userService.save(player);
        return "redirect:/player/" + id;
    }
}
