package wcpredictor.config;

import wcpredictor.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserService userService;

    public SecurityConfig(UserService userService) {
        this.userService = userService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, PersistentTokenRepository tokenRepository) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/confirm", "/forgot-password", "/reset-password", "/css/**", "/js/**", "/images/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(successHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .rememberMe(remember -> remember
                .tokenRepository(tokenRepository)
                .tokenValiditySeconds(14 * 24 * 60 * 60)
            );
        return http.build();
    }

    @Bean
    public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS persistent_logins (username VARCHAR(64) NOT NULL, series VARCHAR(64) PRIMARY KEY, token VARCHAR(64) NOT NULL, last_used TIMESTAMP NOT NULL)");
        } catch (Exception ignored) {}
        JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
        repo.setDataSource(dataSource);
        return repo;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            var user = userService.findByEmailAddress(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

            if (!userService.isActive(user)) {
                throw new UsernameNotFoundException("User account is not active");
            }

            if (user.getEncryptedPassword() == null) {
                throw new UsernameNotFoundException("User has no password set");
            }

            String[] roles = user.isAdmin() ? new String[]{"USER", "ADMIN"} : new String[]{"USER"};

            return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmailAddress())
                .password(user.getEncryptedPassword())
                .roles(roles)
                .build();
        };
    }

    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                    Authentication authentication) throws IOException, ServletException {
                String redirect = request.getParameter("redirect");
                if (redirect != null && !redirect.isBlank() && redirect.startsWith("/")) {
                    response.sendRedirect(redirect);
                    return;
                }
                response.sendRedirect("/");
            }
        };
    }
}
