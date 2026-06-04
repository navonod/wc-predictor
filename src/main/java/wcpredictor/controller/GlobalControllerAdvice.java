package wcpredictor.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import wcpredictor.entity.User;
import wcpredictor.service.UserService;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UserService userService;

    public GlobalControllerAdvice(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("currentUser")
    public User currentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        return userService.findByEmailAddress(auth.getName()).orElse(null);
    }
}
