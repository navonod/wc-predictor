package wcpredictor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import wcpredictor.entity.User;
import wcpredictor.service.PoolService;
import wcpredictor.service.ScoringService;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class LeaderboardController {

    private final ScoringService scoringService;
    private final PoolService poolService;

    public LeaderboardController(ScoringService scoringService, PoolService poolService) {
        this.scoringService = scoringService;
        this.poolService = poolService;
    }

    @GetMapping("/leaderboard")
    public String leaderboard(@RequestParam(required = false) UUID poolId,
                               Model model, @ModelAttribute("currentUser") User currentUser) {
        Set<UUID> poolUserIds = Set.of();
        if (poolId != null) {
            var pool = poolService.findById(poolId);
            if (pool.isPresent()) {
                poolUserIds = pool.get().getUsers().stream()
                        .map(User::getId).collect(Collectors.toSet());
                model.addAttribute("selectedPool", pool.get());
            }
        }
        model.addAttribute("leaderboard", scoringService.getLeaderboard(poolUserIds));
        model.addAttribute("pools", currentUser.getPools());
        return "leaderboard";
    }
}
