package wcpredictor.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wcpredictor.entity.ConfirmationToken;
import wcpredictor.entity.User;
import wcpredictor.repository.ConfirmationTokenRepository;
import wcpredictor.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(UserRegistrationService.class);

    private final UserRepository userRepository;
    private final ConfirmationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.properties.mail.smtp.from:noreply@example.com}")
    private String fromAddress;

    @Value("${spring.mail.reply-to:}")
    private String replyTo;

    public UserRegistrationService(UserRepository userRepository,
                                    ConfirmationTokenRepository tokenRepository,
                                    PasswordEncoder passwordEncoder,
                                    JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    @Transactional
    public void register(String email, String password, String confirmPassword) throws Exception {
        if (email == null || email.isBlank()) throw new Exception("Email is required.");
        if (password == null || password.length() < 6) throw new Exception("Password must be at least 6 characters.");
        if (!password.equals(confirmPassword)) throw new Exception("Passwords do not match.");

        Optional<User> existing = userRepository.findByEmailAddress(email);
        if (existing.isPresent()) {
            if (existing.get().getConfirmed() != null && existing.get().getConfirmed()) {
                throw new Exception("Email already registered.");
            }
            tokenRepository.deleteByUserId(existing.get().getId());
        }

        User user = existing.orElseGet(() -> {
            User u = new User();
            u.setConfirmed(false);
            return u;
        });
        user.setEmailAddress(email);
        user.setEncryptedPassword(passwordEncoder.encode(password));
        user = userRepository.save(user);

        ConfirmationToken token = new ConfirmationToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        tokenRepository.save(token);

        log.info("User {} registered. Confirmation token: {}", email, token.getToken());
        sendConfirmationEmail(email, token.getToken());
    }

    private void sendConfirmationEmail(String email, String token) {
        String link = baseUrl + "/confirm?token=" + token;
        log.info("Confirmation link: {}", link);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromAddress);
            if (replyTo != null && !replyTo.isBlank()) helper.setReplyTo(replyTo);
            helper.setTo(email);
            helper.setSubject("Confirm your WC Predictor account");
            helper.setText("<p>Click the link below to confirm your account:</p><p><a href=\"" + link + "\">" + link + "</a></p>", true);
            mailSender.send(message);
            log.info("Confirmation email sent to {}", email);
        } catch (Exception e) {
            log.warn("Failed to send confirmation email: {}", e.getMessage());
            log.info("Confirmation link (use this): {}", link);
        }
    }

    @Transactional
    public boolean confirmRegistration(String tokenStr) {
        var tokenOpt = tokenRepository.findByToken(tokenStr);
        if (tokenOpt.isEmpty()) return false;
        ConfirmationToken token = tokenOpt.get();
        if (token.getExpiresAt().isBefore(Instant.now())) {
            tokenRepository.delete(token);
            return false;
        }
        User user = token.getUser();
        user.setConfirmed(true);
        userRepository.save(user);
        tokenRepository.delete(token);
        return true;
    }
}
