package group19.restaurant_system.repository;

import group19.restaurant_system.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<User> rowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setUserId(rs.getInt("userId"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        Timestamp createdAt = rs.getTimestamp("createdAt");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updatedAt");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return user;
    };

    public Optional<User> findById(Integer userId) {
        return queryForOptional("SELECT userId, name, email, password, createdAt, updatedAt FROM users WHERE userId = ?", userId);
    }

    public Optional<User> findByName(String name) {
        return queryForOptional("SELECT userId, name, email, password, createdAt, updatedAt FROM users WHERE name = ?", name);
    }

    public Optional<User> findByEmail(String email) {
        return queryForOptional("SELECT userId, name, email, password, createdAt, updatedAt FROM users WHERE email = ?", email);
    }

    public boolean existsByName(String name) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM users WHERE name = ?", Integer.class, name);
        return count != null && count > 0;
    }

    public boolean existsByEmail(String email) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM users WHERE email = ?", Integer.class, email);
        return count != null && count > 0;
    }

    public User save(User user) {
        if (user.getUserId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO users (name, email, password, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, user.getName());
                ps.setString(2, user.getEmail());
                ps.setString(3, user.getPassword());
                LocalDateTime createdAt = user.getCreatedAt() != null ? user.getCreatedAt() : LocalDateTime.now();
                LocalDateTime updatedAt = user.getUpdatedAt() != null ? user.getUpdatedAt() : LocalDateTime.now();
                ps.setTimestamp(4, Timestamp.valueOf(createdAt));
                ps.setTimestamp(5, Timestamp.valueOf(updatedAt));
                return ps;
            }, keyHolder);

            Number key = keyHolder.getKey();
            if (key != null) {
                return findById(key.intValue()).orElseGet(() -> {
                    user.setUserId(key.intValue());
                    return user;
                });
            }
            return user;
        }

        jdbcTemplate.update(
                "UPDATE users SET name = ?, email = ?, password = ?, updatedAt = CURRENT_TIMESTAMP WHERE userId = ?",
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.getUserId()
        );
        return findById(user.getUserId()).orElse(user);
    }

    public void deleteById(Integer userId) {
        jdbcTemplate.update("DELETE FROM users WHERE userId = ?", userId);
    }

    public void deleteAllInBatch() {
        jdbcTemplate.update("DELETE FROM users");
    }

    private Optional<User> queryForOptional(String sql, Object... params) {
        List<User> results = jdbcTemplate.query(sql, rowMapper, params);
        return results.stream().findFirst();
    }
}
