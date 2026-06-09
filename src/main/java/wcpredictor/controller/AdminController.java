package wcpredictor.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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
    private final PoolService poolService;
    private final UserService userService;
    private final TournamentService tournamentService;
    private final PredictionService predictionService;

    public AdminController(TeamService teamService, MatchService matchService,
                            SettingService settingService, PoolService poolService,
                            UserService userService, TournamentService tournamentService,
                            PredictionService predictionService) {
        this.teamService = teamService;
        this.matchService = matchService;
        this.settingService = settingService;
        this.poolService = poolService;
        this.userService = userService;
        this.tournamentService = tournamentService;
        this.predictionService = predictionService;
    }

    @GetMapping
    public String adminDashboard(Model model) {
        UUID tid = tournamentService.findAll().stream().findFirst().map(Tournament::getId).orElse(null);
        model.addAttribute("teams", tid != null ? teamService.getTeamsByTournament(tid) : List.of());
        model.addAttribute("roundTypes", matchService.getAllRoundTypes());
        model.addAttribute("openRounds", matchService.getOpenRounds());
        model.addAttribute("matchesByRound", matchService.getAllMatchesGroupedByRound());
        return "admin/dashboard";
    }

    @GetMapping("/teams")
    public String manageTeams(Model model) {
        UUID tid = tournamentService.findAll().stream().findFirst().map(Tournament::getId).orElse(null);
        model.addAttribute("teams", tid != null ? teamService.getTeamsByTournament(tid) : List.of());
        return "admin/teams";
    }

    @PostMapping("/teams/add")
    public String addTeam(@RequestParam String name, @RequestParam String fifaCode) {
        Team team = new Team();
        team.setName(name);
        team.setFifaCode(fifaCode);
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

    @GetMapping("/pools")
    public String managePools(Model model) {
        model.addAttribute("pools", poolService.findAll());
        return "admin/pools";
    }

    @PostMapping("/pools/create")
    public String createPool(@RequestParam String name, @RequestParam String description) {
        poolService.create(name, description);
        return "redirect:/admin/pools";
    }

    @PostMapping("/pools/delete")
    public String deletePool(@RequestParam UUID id) {
        poolService.delete(id);
        return "redirect:/admin/pools";
    }

    @GetMapping("/pools/{id}/users")
    public String managePoolUsers(@PathVariable UUID id, Model model) {
        var pool = poolService.findById(id).orElseThrow();
        var allUsers = userService.findAll();
        var poolUserIds = pool.getUsers().stream().map(User::getId).collect(java.util.stream.Collectors.toSet());
        model.addAttribute("pool", pool);
        model.addAttribute("allUsers", allUsers);
        model.addAttribute("poolUserIds", poolUserIds);
        return "admin/pool-users";
    }

    @PostMapping("/pools/{id}/users/add")
    public String addUserToPool(@PathVariable UUID id, @RequestParam UUID userId) {
        var pool = poolService.findById(id).orElseThrow();
        var user = userService.findById(userId).orElseThrow();
        poolService.addUser(pool, user);
        return "redirect:/admin/pools/" + id + "/users";
    }

    @PostMapping("/pools/{id}/users/remove")
    public String removeUserFromPool(@PathVariable UUID id, @RequestParam UUID userId) {
        var pool = poolService.findById(id).orElseThrow();
        var user = userService.findById(userId).orElseThrow();
        poolService.removeUser(pool, user);
        return "redirect:/admin/pools/" + id + "/users";
    }

    @GetMapping("/tournaments")
    public String manageTournaments(Model model) {
        model.addAttribute("tournaments", tournamentService.findAll());
        return "admin/tournaments";
    }

    @PostMapping("/tournaments/create")
    public String createTournament(@RequestParam String name, @RequestParam String description) {
        tournamentService.create(name, description);
        return "redirect:/admin/tournaments";
    }

    @PostMapping("/tournaments/delete")
    public String deleteTournament(@RequestParam UUID id, RedirectAttributes ra) {
        try {
            tournamentService.delete(id);
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/tournaments";
        }
        return "redirect:/admin/tournaments";
    }

    @GetMapping("/tournaments/{id}/import")
    public String importForm(@PathVariable UUID id, Model model) {
        model.addAttribute("tournament", tournamentService.findById(id).orElseThrow());
        return "admin/tournament-import";
    }

    @PostMapping("/tournaments/{id}/import")
    public String importSchedule(@PathVariable UUID id,
                                  @RequestParam("file") MultipartFile file,
                                  Model model) {
        if (file.isEmpty()) {
            model.addAttribute("error", "No file selected.");
            model.addAttribute("tournament", tournamentService.findById(id).orElseThrow());
            return "admin/tournament-import";
        }
        try {
            int count = tournamentService.importSchedule(id, file.getInputStream());
            model.addAttribute("success", "Imported " + count + " matches.");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        model.addAttribute("tournament", tournamentService.findById(id).orElseThrow());
        return "admin/tournament-import";
    }

    @GetMapping("/users")
    public String userStatus(Model model) {
        List<User> allUsers = userService.findAll();

        List<User> unconfirmed = new ArrayList<>();
        List<User> noProfile = new ArrayList<>();
        List<User> noPredictions = new ArrayList<>();

        for (User u : allUsers) {
            if (u.getConfirmed() == null || !u.getConfirmed()) {
                unconfirmed.add(u);
                continue;
            }
            if (u.getFirstName() == null || u.getFirstName().isBlank()
                    || u.getLastName() == null || u.getLastName().isBlank()
                    || u.getNickname() == null || u.getNickname().isBlank()) {
                noProfile.add(u);
                continue;
            }
            var matchPreds = predictionService.getUserMatchPredictionScores(u.getId());
            if (matchPreds.isEmpty()) {
                noPredictions.add(u);
            }
        }

        model.addAttribute("unconfirmed", unconfirmed);
        model.addAttribute("noProfile", noProfile);
        model.addAttribute("noPredictions", noPredictions);
        return "admin/users";
    }
}
