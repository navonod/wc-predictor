package wcpredictor.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
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
        return "player";
    }
}
