package wcpredictor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import wcpredictor.entity.User;
import wcpredictor.service.UserService;

import java.util.UUID;

@Controller
public class PlayerController {

    private final UserService userService;

    public PlayerController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/player/{id}")
    public String viewPlayer(@PathVariable UUID id, Model model,
                              @ModelAttribute("currentUser") User viewer) {
        var userOpt = userService.findById(id);
        if (userOpt.isEmpty()) return "redirect:/leaderboard";
        var user = userOpt.get();

        model.addAttribute("player", user);
        model.addAttribute("isSelf", viewer.getId().equals(id));
        model.addAttribute("isAdmin", viewer.isAdmin());
        return "player";
    }
}
