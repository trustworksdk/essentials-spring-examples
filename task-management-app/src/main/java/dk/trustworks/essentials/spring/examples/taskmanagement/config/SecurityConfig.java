package dk.trustworks.essentials.spring.examples.taskmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize.requestMatchers("/login",
                                                                          "/register",
                                                                          "/css/**",
                                                                          "/js/**",
                                                                          "/actuator/health")
                                                         .permitAll()
                                                         .requestMatchers(HttpMethod.POST, "/users", "/users/*/register")
                                                         .permitAll()
                                                         .anyRequest()
                                                         .authenticated())
            .formLogin(form -> form.loginPage("/login")
                                   .successHandler((request, response, authentication) -> {
                                       log.info("Login success for user='{}'", authentication.getName());
                                       response.sendRedirect("/");
                                   })
                                   .failureHandler((request, response, exception) -> {
                                       var user = request.getParameter("username");
                                       log.warn("Login failure for user='{}': {}", user, exception.getMessage());
                                       response.sendRedirect("/login?error");
                                   })
                                   .permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
            .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                              .ignoringRequestMatchers("/users", "/users/*/register"));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
