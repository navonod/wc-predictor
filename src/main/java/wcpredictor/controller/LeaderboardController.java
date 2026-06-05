package wcpredictor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import wcpredictor.entity.User;
import wcpredictor.service.GameService;
import wcpredictor.service.ScoringService;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class LeaderboardController {

    private final ScoringService scoringService;
    private final GameService gameService;

    public LeaderboardController(ScoringService scoringService, GameService gameService) {
        this.scoringService = scoringService;
        this.gameService = gameService;
    }

    @GetMapping("/leaderboard")
    public String leaderboard(@RequestParam(required = false) UUID gameId,
                               Model model, @ModelAttribute("currentUser") User currentUser) {
        Set<UUID> gameUserIds = Set.of();
        if (gameId != null) {
            var game = gameService.findById(gameId);
            if (game.isPresent()) {
                gameUserIds = game.get().getUsers().stream()
                        .map(User::getId).collect(Collectors.toSet());
                model.addAttribute("selectedGame", game.get());
            }
        }
        model.addAttribute("leaderboard", scoringService.getLeaderboard(gameUserIds));
        model.addAttribute("games", currentUser.getGames());
        return "leaderboard";
    }
}
