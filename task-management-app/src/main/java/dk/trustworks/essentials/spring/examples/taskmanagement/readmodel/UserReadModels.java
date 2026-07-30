package dk.trustworks.essentials.spring.examples.taskmanagement.readmodel;

import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.UserCreated;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.UserRegistered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Component
public class UserReadModels {
    private final JdbcTemplate jdbcTemplate;

    public UserReadModels(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserView> findByUserId(String userId) {
        var result = jdbcTemplate.query("SELECT user_id, display_name, email, registered, created_at, updated_at FROM user_view WHERE user_id = ?",
                                        (rs, rowNum) -> mapUserView(rs),
                                        userId);
        return result.stream().findFirst();
    }

    public Optional<UserAuthView> findAuthByUserId(String userId) {
        var result = jdbcTemplate.query("SELECT user_id, password_hash, registered FROM user_view WHERE user_id = ?",
                                        (rs, rowNum) -> new UserAuthView(rs.getString("user_id"),
                                                                         rs.getString("password_hash"),
                                                                         rs.getBoolean("registered")),
                                        userId);
        return result.stream().findFirst();
    }

    public boolean exists(String userId) {
        var count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM user_view WHERE user_id = ?", Integer.class, userId);
        return count != null && count > 0;
    }

    public void on(UserCreated event) {
        jdbcTemplate.update("INSERT INTO user_view(user_id, display_name, email, password_hash, registered, created_at, updated_at) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                                    "ON CONFLICT (user_id) DO NOTHING",
                            event.userId().toString(),
                            event.displayName(),
                            null,
                            null,
                            false,
                            ts(event.createdAt()),
                            ts(event.createdAt()));
    }

    public void on(UserRegistered event) {
        jdbcTemplate.update("UPDATE user_view SET email = ?, password_hash = ?, registered = ?, updated_at = ? WHERE user_id = ?",
                            event.email(),
                            event.passwordHash(),
                            true,
                            ts(event.registeredAt()),
                            event.userId().toString());
    }

    private UserView mapUserView(ResultSet rs) throws SQLException {
        return new UserView(rs.getString("user_id"),
                            rs.getString("display_name"),
                            rs.getString("email"),
                            rs.getBoolean("registered"),
                            rs.getTimestamp("created_at").toInstant(),
                            rs.getTimestamp("updated_at").toInstant());
    }

    private static Timestamp ts(Instant instant) {
        return Timestamp.from(instant);
    }
}
