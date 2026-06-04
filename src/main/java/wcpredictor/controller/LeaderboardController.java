package wcpredictor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import wcpredictor.service.ScoringService;

import java.security.Principal;

@Controller
public class LeaderboardController {

    private final ScoringService scoringService;

    public LeaderboardController(ScoringService scoringService) {
        this.scoringService = scoringService;
    }

    @GetMapping("/leaderboard")
    public String leaderboard(Model model, Principal principal) {
        model.addAttribute("leaderboard", scoringService.getLeaderboard());
        return "leaderboard";
    }
}
