package wcpredictor.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import wcpredictor.entity.User;
import wcpredictor.service.UserService;

import java.util.UUID;

@Controller
public class PlayerController {

    private static final Logger log = LoggerFactory.getLogger(PlayerController.class);

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

        boolean isSelf = viewer != null && viewer.getId().equals(id);
        boolean isAdmin = viewer != null && viewer.isAdmin();
        log.info("Player page: viewer={}, target={}, isSelf={}, isAdmin={}",
                viewer != null ? viewer.getEmailAddress() : "null",
                user.getEmailAddress(), isSelf, isAdmin);

        model.addAttribute("player", user);
        model.addAttribute("isSelf", isSelf);
        model.addAttribute("isAdmin", isAdmin);
        return "player";
    }
}
