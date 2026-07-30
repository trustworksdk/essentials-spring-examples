package dk.trustworks.essentials.spring.examples.taskmanagement.config;

import dk.trustworks.essentials.spring.examples.taskmanagement.readmodel.UserReadModels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AggregateUserDetailsService implements UserDetailsService {
    private static final Logger log = LoggerFactory.getLogger(AggregateUserDetailsService.class);
    private final UserReadModels userReadModels;

    public AggregateUserDetailsService(UserReadModels userReadModels) {
        this.userReadModels = userReadModels;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var auth = userReadModels.findAuthByUserId(username).orElse(null);
        if (auth == null) {
            log.warn("Authentication failed: user '{}' not found", username);
            throw new UsernameNotFoundException("User not found: " + username);
        }
        if (!auth.registered()) {
            log.warn("Authentication failed: user '{}' exists but is not registered", username);
            throw new UsernameNotFoundException("User not registered: " + username);
        }
        if (auth.passwordHash() == null || auth.passwordHash().isBlank()) {
            log.warn("Authentication failed: user '{}' has no password hash", username);
            throw new UsernameNotFoundException("User has no password hash: " + username);
        }
        log.info("Authentication lookup succeeded for user '{}'", username);
        return new User(auth.userId(),
                        auth.passwordHash(),
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
