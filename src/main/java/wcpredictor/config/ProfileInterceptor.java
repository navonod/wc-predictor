package wcpredictor.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import wcpredictor.service.UserService;

@Component
public class ProfileInterceptor implements HandlerInterceptor {

    private final UserService userService;

    public ProfileInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                              Object handler) throws Exception {
        var auth = request.getUserPrincipal();
        if (auth == null) return true;

        String path = request.getRequestURI();
        if (path.startsWith("/profile/") || path.startsWith("/logout") ||
            path.startsWith("/css") || path.startsWith("/js") || path.startsWith("/images")) {
            return true;
        }

        var user = userService.findByEmailAddress(auth.getName()).orElse(null);
        if (user != null && (user.getFirstName() == null || user.getFirstName().isBlank()
                || user.getLastName() == null || user.getLastName().isBlank()
                || user.getNickname() == null || user.getNickname().isBlank())) {
            response.sendRedirect("/profile/setup");
            return false;
        }
        return true;
    }
}
