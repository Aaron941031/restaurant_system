package group19.restaurant_system.repository;

import group19.restaurant_system.model.Dish;
import group19.restaurant_system.model.User;
import group19.restaurant_system.model.UserExclusion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class UserExclusionRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserExclusionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String BASE_SELECT =
            "SELECT ue.exclusion_id, " +
                    "u.user_id AS u_userId, u.name AS u_name, u.email AS u_email, u.password AS u_password, u.created_at AS u_createdAt, u.updated_at AS u_updatedAt, " +
                    "d.category_id AS d_categoryId, d.name AS d_name " +
                    "FROM user_exclusions ue " +
                    "JOIN users u ON u.user_id = ue.user_id " +
                    "JOIN dishes d ON d.category_id = ue.category_id ";

    private final RowMapper<UserExclusion> rowMapper = (rs, rowNum) -> {
        UserExclusion exclusion = new UserExclusion();
        exclusion.setExclusionId(rs.getInt("exclusion_id"));

        User user = new User();
        user.setUserId(rs.getInt("u_userId"));
        user.setName(rs.getString("u_name"));
        user.setEmail(rs.getString("u_email"));
        user.setPassword(rs.getString("u_password"));
        Timestamp userCreatedAt = rs.getTimestamp("u_createdAt");
        if (userCreatedAt != null) {
            user.setCreatedAt(userCreatedAt.toLocalDateTime());
        }
        Timestamp userUpdatedAt = rs.getTimestamp("u_updatedAt");
        if (userUpdatedAt != null) {
            user.setUpdatedAt(userUpdatedAt.toLocalDateTime());
        }
        exclusion.setUser(user);

        Dish dish = new Dish();
        dish.setCategoryId(rs.getInt("d_categoryId"));
        dish.setName(rs.getString("d_name"));
        exclusion.setDish(dish);

        return exclusion;
    };

    public Optional<UserExclusion> findById(Integer exclusionId) {
        List<UserExclusion> results = jdbcTemplate.query(BASE_SELECT + "WHERE ue.exclusion_id = ?", rowMapper, exclusionId);
        return results.stream().findFirst();
    }

    public List<UserExclusion> findByUserUserId(Integer userId) {
        return jdbcTemplate.query(BASE_SELECT + "WHERE ue.user_id = ?", rowMapper, userId);
    }

    public boolean existsByUserUserIdAndDishCategoryId(Integer userId, Integer categoryId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM user_exclusions WHERE user_id = ? AND category_id = ?",
                Integer.class,
                userId,
                categoryId
        );
        return count != null && count > 0;
    }

    public UserExclusion save(UserExclusion exclusion) {
        if (exclusion.getExclusionId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO user_exclusions (user_id, category_id) VALUES (?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setInt(1, exclusion.getUser().getUserId());
                ps.setInt(2, exclusion.getDish().getCategoryId());
                return ps;
            }, keyHolder);

            Number key = keyHolder.getKey();
            if (key != null) {
                return findById(key.intValue()).orElseGet(() -> {
                    exclusion.setExclusionId(key.intValue());
                    return exclusion;
                });
            }
            return exclusion;
        }

        jdbcTemplate.update(
                "UPDATE user_exclusions SET user_id = ?, category_id = ? WHERE exclusion_id = ?",
                exclusion.getUser().getUserId(),
                exclusion.getDish().getCategoryId(),
                exclusion.getExclusionId()
        );
        return findById(exclusion.getExclusionId()).orElse(exclusion);
    }

    public void deleteByUserUserIdAndDishCategoryId(Integer userId, Integer categoryId) {
        jdbcTemplate.update("DELETE FROM user_exclusions WHERE user_id = ? AND category_id = ?", userId, categoryId);
    }

    public void deleteAllInBatch() {
        jdbcTemplate.update("DELETE FROM user_exclusions");
    }
}
