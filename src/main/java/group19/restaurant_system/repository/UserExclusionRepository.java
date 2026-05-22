package group19.restaurant_system.repository;

import group19.restaurant_system.model.Dish;
import group19.restaurant_system.model.Ingredient;
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
import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Repository
public class UserExclusionRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserExclusionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String BASE_SELECT =
            "SELECT ue.exclusionId, " +
                    "u.userId AS u_userId, u.name AS u_name, u.email AS u_email, u.password AS u_password, u.createdAt AS u_createdAt, u.updatedAt AS u_updatedAt, " +
                "d.categoryId AS d_categoryId, d.name AS d_name, " +
                "i.ingredientId AS i_ingredientId, i.name AS i_name " +
                    "FROM user_exclusions ue " +
                    "JOIN users u ON u.userId = ue.userId " +
                "LEFT JOIN dishes d ON d.categoryId = ue.categoryId " +
                "LEFT JOIN ingredients i ON i.ingredientId = ue.ingredientId ";

    private final RowMapper<UserExclusion> rowMapper = (rs, rowNum) -> {
        UserExclusion exclusion = new UserExclusion();
        exclusion.setExclusionId(rs.getInt("exclusionId"));

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

        Integer dishId = (Integer) rs.getObject("d_categoryId");
        if (dishId != null) {
            Dish dish = new Dish();
            dish.setCategoryId(dishId);
            dish.setName(rs.getString("d_name"));
            exclusion.setDish(dish);
        }

        Integer ingredientId = (Integer) rs.getObject("i_ingredientId");
        if (ingredientId != null) {
            Ingredient ingredient = new Ingredient();
            ingredient.setIngredientId(ingredientId);
            ingredient.setName(rs.getString("i_name"));
            exclusion.setIngredient(ingredient);
        }

        return exclusion;
    };

    public Optional<UserExclusion> findById(Integer exclusionId) {
        List<UserExclusion> results = jdbcTemplate.query(BASE_SELECT + "WHERE ue.exclusionId = ?", rowMapper, exclusionId);
        return results.stream().findFirst();
    }

    public List<UserExclusion> findByUserUserId(Integer userId) {
        return jdbcTemplate.query(BASE_SELECT + "WHERE ue.userId = ?", rowMapper, userId);
    }

    public Optional<UserExclusion> findByUserUserIdAndDishCategoryId(Integer userId, Integer categoryId) {
        List<UserExclusion> results = jdbcTemplate.query(
                BASE_SELECT + "WHERE ue.userId = ? AND ue.categoryId = ?",
                rowMapper,
                userId,
                categoryId
        );
        return results.stream().findFirst();
    }

    public Optional<UserExclusion> findByUserUserIdAndIngredientId(Integer userId, Integer ingredientId) {
        List<UserExclusion> results = jdbcTemplate.query(
                BASE_SELECT + "WHERE ue.userId = ? AND ue.ingredientId = ?",
                rowMapper,
                userId,
                ingredientId
        );
        return results.stream().findFirst();
    }

    public boolean existsByUserUserIdAndDishCategoryId(Integer userId, Integer categoryId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM user_exclusions WHERE userId = ? AND categoryId = ?",
                Integer.class,
                userId,
                categoryId
        );
        return count != null && count > 0;
    }

    public boolean existsByUserUserIdAndIngredientId(Integer userId, Integer ingredientId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM user_exclusions WHERE userId = ? AND ingredientId = ?",
                Integer.class,
                userId,
                ingredientId
        );
        return count != null && count > 0;
    }

    public UserExclusion save(UserExclusion exclusion) {
        if (exclusion.getExclusionId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO user_exclusions (userId, categoryId, ingredientId) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setInt(1, exclusion.getUser().getUserId());
                if (exclusion.getDish() != null) {
                    ps.setInt(2, exclusion.getDish().getCategoryId());
                } else {
                    ps.setNull(2, Types.INTEGER);
                }
                if (exclusion.getIngredient() != null) {
                    ps.setInt(3, exclusion.getIngredient().getIngredientId());
                } else {
                    ps.setNull(3, Types.INTEGER);
                }
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

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE user_exclusions SET userId = ?, categoryId = ?, ingredientId = ? WHERE exclusionId = ?"
            );
            ps.setInt(1, exclusion.getUser().getUserId());
            if (exclusion.getDish() != null) {
                ps.setInt(2, exclusion.getDish().getCategoryId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            if (exclusion.getIngredient() != null) {
                ps.setInt(3, exclusion.getIngredient().getIngredientId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setInt(4, exclusion.getExclusionId());
            return ps;
        });
        return findById(exclusion.getExclusionId()).orElse(exclusion);
    }

    public void deleteByUserUserIdAndDishCategoryId(Integer userId, Integer categoryId) {
        jdbcTemplate.update("DELETE FROM user_exclusions WHERE userId = ? AND categoryId = ?", userId, categoryId);
    }

    public void deleteAllInBatch() {
        jdbcTemplate.update("DELETE FROM user_exclusions");
    }
}
