package wcpredictor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import wcpredictor.entity.User;
import wcpredictor.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Controller
public class PasswordResetController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email, Model model) {
        var userOpt = userService.findByEmailAddress(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPasswordResetToken(UUID.randomUUID().toString());
            user.setPasswordResetTokenExpiry(Instant.now().plus(1, ChronoUnit.HOURS));
            userService.save(user);
            model.addAttribute("message", "Password reset link: /reset-password?token=" + user.getPasswordResetToken());
            model.addAttribute("success", true);
        } else {
            model.addAttribute("message", "If that email exists, a reset link has been generated.");
            model.addAttribute("success", true);
        }
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam String token, Model model) {
        var userOpt = userService.findByPasswordResetToken(token);
        if (userOpt.isEmpty()) {
            model.addAttribute("message", "Invalid token.");
            model.addAttribute("error", true);
            return "reset-password";
        }
        var user = userOpt.get();
        if (user.getPasswordResetTokenExpiry().isBefore(Instant.now())) {
            model.addAttribute("message", "Token has expired.");
            model.addAttribute("error", true);
            return "reset-password";
        }
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                                 @RequestParam String password,
                                 @RequestParam String confirmPassword,
                                 Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("message", "Passwords do not match.");
            model.addAttribute("error", true);
            return "reset-password";
        }

        var userOpt = userService.findByPasswordResetToken(token);
        if (userOpt.isEmpty()) {
            model.addAttribute("message", "Invalid token.");
            model.addAttribute("error", true);
            return "reset-password";
        }
        var user = userOpt.get();
        if (user.getPasswordResetTokenExpiry().isBefore(Instant.now())) {
            model.addAttribute("message", "Token has expired.");
            model.addAttribute("error", true);
            return "reset-password";
        }
        user.setEncryptedPassword(passwordEncoder.encode(password));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userService.save(user);
        model.addAttribute("message", "Password reset successful. You can now log in.");
        model.addAttribute("success", true);
        return "reset-password";
    }
}
