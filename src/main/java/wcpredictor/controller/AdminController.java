package wcpredictor.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import wcpredictor.entity.*;
import wcpredictor.service.*;

import java.util.*;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final TeamService teamService;
    private final MatchService matchService;
    private final SettingService settingService;

    public AdminController(TeamService teamService, MatchService matchService,
                            SettingService settingService) {
        this.teamService = teamService;
        this.matchService = matchService;
        this.settingService = settingService;
    }

    @GetMapping
    public String adminDashboard(Model model) {
        model.addAttribute("teams", teamService.getGroupedTeams());
        model.addAttribute("roundTypes", matchService.getAllRoundTypes());
        model.addAttribute("openRounds", matchService.getOpenRounds());
        model.addAttribute("matchesByRound", matchService.getAllMatchesGroupedByRound());
        return "admin/dashboard";
    }

    @GetMapping("/teams")
    public String manageTeams(Model model) {
        model.addAttribute("teams", teamService.getGroupedTeams());
        return "admin/teams";
    }

    @PostMapping("/teams/add")
    public String addTeam(@RequestParam String name, @RequestParam String fifaCode,
                          @RequestParam(required = false) String groupLetter) {
        Team team = new Team();
        team.setName(name);
        team.setFifaCode(fifaCode);
        team.setGroupLetter(groupLetter != null && !groupLetter.isBlank() ? groupLetter : null);
        teamService.save(team);
        return "redirect:/admin/teams";
    }

    @PostMapping("/teams/delete")
    public String deleteTeam(@RequestParam UUID id) {
        teamService.delete(id);
        return "redirect:/admin/teams";
    }

    @GetMapping("/matches")
    public String manageMatches(Model model) {
        model.addAttribute("matchesByRound", matchService.getAllMatchesGroupedByRound());
        model.addAttribute("teams", teamService.getAllTeams());
        model.addAttribute("roundTypes", matchService.getAllRoundTypes());
        return "admin/matches";
    }

    @PostMapping("/matches/set-result")
    public String setMatchResult(@RequestParam UUID matchId,
                                  @RequestParam Integer team1Score,
                                  @RequestParam Integer team2Score) {
        Match match = matchService.findById(matchId).orElseThrow();
        match.setTeam1Score(team1Score);
        match.setTeam2Score(team2Score);
        matchService.save(match);
        return "redirect:/admin/matches";
    }

    @PostMapping("/matches/lock-round")
    public String lockRound(@RequestParam String roundType) {
        matchService.lockRound(RoundType.valueOf(roundType));
        return "redirect:/admin/matches";
    }

    @PostMapping("/matches/unlock-round")
    public String unlockRound(@RequestParam String roundType) {
        matchService.unlockRound(RoundType.valueOf(roundType));
        return "redirect:/admin/matches";
    }

    @GetMapping("/settings")
    public String manageSettings(Model model) {
        List<Setting> settings = settingService.getAllSettings();
        Map<String, Setting> settingsMap = new LinkedHashMap<>();
        for (Setting s : settings) {
            settingsMap.put(s.getName(), s);
        }
        model.addAttribute("settings", settingsMap);
        return "admin/settings";
    }

    @PostMapping("/settings")
    public String saveSettings(@RequestParam Map<String, String> params) {
        for (var entry : params.entrySet()) {
            if (entry.getKey().startsWith("setting_")) {
                String name = entry.getKey().substring(8);
                var setting = settingService.findByName(name).orElse(new Setting());
                setting.setName(name);
                setting.setValue(entry.getValue());
                setting.setValueType(name.startsWith("points_draw_") ? "DOUBLE" : "INTEGER");
                settingService.save(setting);
            }
        }
        return "redirect:/admin/settings?updated";
    }
}
