package wcpredictor.controller;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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

    private static final Logger log = LoggerFactory.getLogger(PasswordResetController.class);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8090}")
    private String baseUrl;

    @Value("${spring.mail.properties.mail.smtp.from:noreply@example.com}")
    private String fromAddress;

    public PasswordResetController(UserService userService, PasswordEncoder passwordEncoder,
                                    JavaMailSender mailSender) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
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
            sendResetEmail(user);
        }
        model.addAttribute("message", "If that email is registered, a password reset link has been sent.");
        model.addAttribute("success", true);
        return "forgot-password";
    }

    private void sendResetEmail(User user) {
        String link = baseUrl + "/reset-password?token=" + user.getPasswordResetToken();
        log.info("Password reset link: {}", link);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromAddress);
            helper.setTo(user.getEmailAddress());
            helper.setSubject("Reset your WC Predictor password");
            helper.setText("<p>Click the link below to reset your password (expires in 1 hour):</p>"
                    + "<p><a href=\"" + link + "\">" + link + "</a></p>", true);
            mailSender.send(message);
            log.info("Password reset email sent to {}", user.getEmailAddress());
        } catch (Exception e) {
            log.warn("Failed to send password reset email: {}", e.getMessage());
            log.info("Password reset link (use this): {}", link);
        }
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
        log.info("POST /reset-password received token: {}", token);
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
