package wcpredictor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import wcpredictor.entity.RoundType;
import wcpredictor.entity.User;
import wcpredictor.service.SimulationService;
import wcpredictor.service.MatchService;

import java.util.List;
import java.util.UUID;

@Controller
public class SimulationController {

    private final SimulationService simulationService;
    private final MatchService matchService;

    public SimulationController(SimulationService simulationService,
                                 MatchService matchService) {
        this.simulationService = simulationService;
        this.matchService = matchService;
    }

    @GetMapping("/simulate")
    public String simulationPage(@ModelAttribute("currentUser") User user, Model model) {
        model.addAttribute("standings", simulationService.getUserStandings(user));
        model.addAttribute("bestThirds", simulationService.getUserBestThirds(user));
        model.addAttribute("bracket", simulationService.getKnockoutBracket(user));
        model.addAttribute("allRounds", simulationService.getAllKnockoutRounds(user));
        model.addAttribute("hasSimulation", simulationService.hasSimulation(user));
        return "simulate";
    }

    @GetMapping("/simulate/group/{group}")
    public String groupPage(@ModelAttribute("currentUser") User user,
                            @PathVariable char group,
                            @RequestParam(required = false, defaultValue = "GROUP_MD1") String round,
                            Model model) {
        var matches = matchService.getMatchesByGroup(String.valueOf(group));
        model.addAttribute("group", group);
        model.addAttribute("matches", matches);
        model.addAttribute("selectedRound", round);
        model.addAttribute("rounds", List.of("GROUP_MD1", "GROUP_MD2", "GROUP_MD3"));
        model.addAttribute("scores", simulationService.getUserScores(user));
        return "simulate-group";
    }

    @PostMapping("/simulate/randomize")
    public String randomize(@ModelAttribute("currentUser") User user, RedirectAttributes ra) {
        simulationService.randomize(user);
        ra.addFlashAttribute("message", "Simulation randomized!");
        return "redirect:/simulate";
    }

    @PostMapping("/simulate/save")
    public String saveScore(@ModelAttribute("currentUser") User user,
                            @RequestParam UUID matchId,
                            @RequestParam int team1Score,
                            @RequestParam int team2Score,
                            @RequestParam String redirect) {
        simulationService.saveScore(user, matchId, team1Score, team2Score);
        return "redirect:" + redirect;
    }

    @PostMapping("/simulate/clear")
    public String clear(@ModelAttribute("currentUser") User user, RedirectAttributes ra) {
        simulationService.clearSimulation(user);
        ra.addFlashAttribute("message", "Simulation cleared.");
        return "redirect:/simulate";
    }
}
