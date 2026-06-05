package wcpredictor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import wcpredictor.entity.User;
import wcpredictor.service.UserService;

import java.security.Principal;

@Controller
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile/setup")
    public String setupForm(@ModelAttribute("currentUser") User user, Model model) {
        if (user.getFirstName() != null && user.getLastName() != null && user.getNickname() != null) {
            return "redirect:/";
        }
        return "profile-setup";
    }

    @PostMapping("/profile/setup")
    public String saveProfile(@ModelAttribute("currentUser") User currentUser,
                               @RequestParam String firstName,
                               @RequestParam String lastName,
                               @RequestParam String nickname) {
        currentUser.setFirstName(firstName);
        currentUser.setLastName(lastName);
        currentUser.setNickname(nickname);
        userService.save(currentUser);
        return "redirect:/";
    }
}
