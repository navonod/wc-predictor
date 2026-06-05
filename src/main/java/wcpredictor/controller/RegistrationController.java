package wcpredictor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import wcpredictor.service.UserRegistrationService;

@Controller
public class RegistrationController {

    private final UserRegistrationService registrationService;

    public RegistrationController(UserRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           Model model) {
        try {
            registrationService.register(email, password, confirmPassword);
            return "redirect:/login?registered";
        } catch (Exception e) {
            model.addAttribute("message", e.getMessage());
            model.addAttribute("error", true);
        }
        return "register";
    }

    @GetMapping("/confirm")
    public String confirm(@RequestParam String token, Model model) {
        boolean confirmed = registrationService.confirmRegistration(token);
        if (confirmed) {
            return "redirect:/login?confirmed";
        }
        model.addAttribute("message", "Invalid or expired confirmation token.");
        model.addAttribute("error", true);
        return "confirm";
    }
}
