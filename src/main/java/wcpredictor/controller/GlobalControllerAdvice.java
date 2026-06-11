package wcpredictor.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import wcpredictor.entity.User;
import wcpredictor.service.TimeService;
import wcpredictor.service.UserService;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UserService userService;
    private final TimeService timeService;

    public GlobalControllerAdvice(UserService userService, TimeService timeService) {
        this.userService = userService;
        this.timeService = timeService;
    }

    @ModelAttribute("currentUser")
    public User currentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        return userService.findByEmailAddress(auth.getName()).orElse(null);
    }

    @ModelAttribute("dummyTimeActive")
    public boolean dummyTimeActive() {
        return timeService.isOverrideEnabled();
    }
}
