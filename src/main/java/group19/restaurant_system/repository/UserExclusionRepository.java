package group19.restaurant_system.repository;

import group19.restaurant_system.model.Dish;
import group19.restaurant_system.model.Ingredient;
import group19.restaurant_system.model.User;
import group19.restaurant_system.model.UserExclusion;
import group19.restaurant_system.model.Restaurant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class UserExclusionRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserExclusionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<UserExclusion> dishRowMapper = (rs, rowNum) -> {
        UserExclusion exclusion = new UserExclusion();
        User user = new User();
        user.setUserId(rs.getInt("u_userId"));
        user.setName(rs.getString("u_name"));
        user.setEmail(rs.getString("u_email"));
        user.setPassword(rs.getString("u_password"));
        Timestamp userCreatedAt = rs.getTimestamp("u_createdAt");
        if (userCreatedAt != null) user.setCreatedAt(userCreatedAt.toLocalDateTime());
        Timestamp userUpdatedAt = rs.getTimestamp("u_updatedAt");
        if (userUpdatedAt != null) user.setUpdatedAt(userUpdatedAt.toLocalDateTime());
        exclusion.setUser(user);

        Dish dish = new Dish();
        dish.setDishId(rs.getInt("d_dishId"));
        dish.setName(rs.getString("d_name"));
        exclusion.setDish(dish);
        return exclusion;
    };

    private final RowMapper<UserExclusion> ingredientRowMapper = (rs, rowNum) -> {
        UserExclusion exclusion = new UserExclusion();
        User user = new User();
        user.setUserId(rs.getInt("u_userId"));
        user.setName(rs.getString("u_name"));
        user.setEmail(rs.getString("u_email"));
        user.setPassword(rs.getString("u_password"));
        Timestamp userCreatedAt = rs.getTimestamp("u_createdAt");
        if (userCreatedAt != null) user.setCreatedAt(userCreatedAt.toLocalDateTime());
        Timestamp userUpdatedAt = rs.getTimestamp("u_updatedAt");
        if (userUpdatedAt != null) user.setUpdatedAt(userUpdatedAt.toLocalDateTime());
        exclusion.setUser(user);

        Ingredient ingredient = new Ingredient();
        ingredient.setIngredientId(rs.getInt("i_ingredientId"));
        ingredient.setName(rs.getString("i_name"));
        exclusion.setIngredient(ingredient);
        return exclusion;
    };

    private final RowMapper<UserExclusion> restaurantRowMapper = (rs, rowNum) -> {
        UserExclusion exclusion = new UserExclusion();
        User user = new User();
        user.setUserId(rs.getInt("u_userId"));
        user.setName(rs.getString("u_name"));
        user.setEmail(rs.getString("u_email"));
        user.setPassword(rs.getString("u_password"));
        Timestamp userCreatedAt = rs.getTimestamp("u_createdAt");
        if (userCreatedAt != null) user.setCreatedAt(userCreatedAt.toLocalDateTime());
        Timestamp userUpdatedAt = rs.getTimestamp("u_updatedAt");
        if (userUpdatedAt != null) user.setUpdatedAt(userUpdatedAt.toLocalDateTime());
        exclusion.setUser(user);

        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId(rs.getInt("r_restaurantId"));
        restaurant.setName(rs.getString("r_name"));
        restaurant.setCategory(rs.getString("r_category"));
        restaurant.setPriceRange(rs.getString("r_priceRange"));
        restaurant.setAvgScore(rs.getDouble("r_avgScore"));
        restaurant.setRatingCount(rs.getInt("r_ratingCount"));
        restaurant.setLocationAt(rs.getString("r_locationAt"));
        exclusion.setRestaurant(restaurant);
        return exclusion;
    };

    public Optional<UserExclusion> findById(Integer exclusionId) {
        // Old auto-increment exclusionId no longer exists after DB split.
        return Optional.empty();
    }

    public List<UserExclusion> findByUserUserId(Integer userId) {
        List<UserExclusion> combined = new ArrayList<>();

        // dish exclusions
        String sqlDish = "SELECT u.userId AS u_userId, u.name AS u_name, u.email AS u_email, u.password AS u_password, u.createdAt AS u_createdAt, u.updatedAt AS u_updatedAt, d.dishId AS d_dishId, d.name AS d_name " +
            "FROM user_dish_exclusions ude JOIN users u ON u.userId = ude.userId JOIN dishes d ON d.dishId = ude.dishId WHERE ude.userId = ?";
        combined.addAll(jdbcTemplate.query(sqlDish, dishRowMapper, userId));

        // ingredient exclusions
        String sqlIng = "SELECT u.userId AS u_userId, u.name AS u_name, u.email AS u_email, u.password AS u_password, u.createdAt AS u_createdAt, u.updatedAt AS u_updatedAt, i.ingredientId AS i_ingredientId, i.name AS i_name " +
            "FROM user_ingredient_exclusions uie JOIN users u ON u.userId = uie.userId JOIN ingredients i ON i.ingredientId = uie.ingredientId WHERE uie.userId = ?";
        combined.addAll(jdbcTemplate.query(sqlIng, ingredientRowMapper, userId));

        // restaurant exclusions
        String sqlRest = "SELECT u.userId AS u_userId, u.name AS u_name, u.email AS u_email, u.password AS u_password, u.createdAt AS u_createdAt, u.updatedAt AS u_updatedAt, r.restaurantId AS r_restaurantId, r.name AS r_name, r.category AS r_category, r.priceRange AS r_priceRange, r.avgScore AS r_avgScore, r.ratingCount AS r_ratingCount, r.locationAt AS r_locationAt " +
            "FROM user_restaurant_exclusions ure JOIN users u ON u.userId = ure.userId JOIN restaurants r ON r.restaurantId = ure.restaurantId WHERE ure.userId = ?";
        combined.addAll(jdbcTemplate.query(sqlRest, restaurantRowMapper, userId));

        return combined;
    }

    public Optional<UserExclusion> findByUserUserIdAndDishId(Integer userId, Integer dishId) {
        String sql = "SELECT u.userId AS u_userId, u.name AS u_name, u.email AS u_email, u.password AS u_password, u.createdAt AS u_createdAt, u.updatedAt AS u_updatedAt, d.dishId AS d_dishId, d.name AS d_name " +
            "FROM user_dish_exclusions ude JOIN users u ON u.userId = ude.userId JOIN dishes d ON d.dishId = ude.dishId WHERE ude.userId = ? AND ude.dishId = ?";
        List<UserExclusion> results = jdbcTemplate.query(sql, dishRowMapper, userId, dishId);
        return results.stream().findFirst();
    }

    public Optional<UserExclusion> findByUserUserIdAndIngredientId(Integer userId, Integer ingredientId) {
        String sql = "SELECT u.userId AS u_userId, u.name AS u_name, u.email AS u_email, u.password AS u_password, u.createdAt AS u_createdAt, u.updatedAt AS u_updatedAt, i.ingredientId AS i_ingredientId, i.name AS i_name " +
            "FROM user_ingredient_exclusions uie JOIN users u ON u.userId = uie.userId JOIN ingredients i ON i.ingredientId = uie.ingredientId WHERE uie.userId = ? AND uie.ingredientId = ?";
        List<UserExclusion> results = jdbcTemplate.query(sql, ingredientRowMapper, userId, ingredientId);
        return results.stream().findFirst();
    }

    public Optional<UserExclusion> findByUserUserIdAndRestaurantId(Integer userId, Integer restaurantId) {
        String sql = "SELECT u.userId AS u_userId, u.name AS u_name, u.email AS u_email, u.password AS u_password, u.createdAt AS u_createdAt, u.updatedAt AS u_updatedAt, r.restaurantId AS r_restaurantId, r.name AS r_name, r.category AS r_category, r.priceRange AS r_priceRange, r.avgScore AS r_avgScore, r.ratingCount AS r_ratingCount, r.locationAt AS r_locationAt " +
            "FROM user_restaurant_exclusions ure JOIN users u ON u.userId = ure.userId JOIN restaurants r ON r.restaurantId = ure.restaurantId WHERE ure.userId = ? AND ure.restaurantId = ?";
        List<UserExclusion> results = jdbcTemplate.query(sql, restaurantRowMapper, userId, restaurantId);
        return results.stream().findFirst();
    }

    public boolean existsByUserUserIdAndDishId(Integer userId, Integer dishId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM user_dish_exclusions WHERE userId = ? AND dishId = ?",
            Integer.class,
            userId,
            dishId
        );
        return count != null && count > 0;
    }

    public boolean existsByUserUserIdAndIngredientId(Integer userId, Integer ingredientId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM user_ingredient_exclusions WHERE userId = ? AND ingredientId = ?",
            Integer.class,
            userId,
            ingredientId
        );
        return count != null && count > 0;
    }

    public boolean existsByUserUserIdAndRestaurantId(Integer userId, Integer restaurantId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM user_restaurant_exclusions WHERE userId = ? AND restaurantId = ?",
            Integer.class,
            userId,
            restaurantId
        );
        return count != null && count > 0;
    }

    public UserExclusion save(UserExclusion exclusion) {
        // Insert into the appropriate new table depending on the exclusion type.
        if (exclusion.getDish() != null) {
            // avoid duplicates
            if (!existsByUserUserIdAndDishId(exclusion.getUser().getUserId(), exclusion.getDish().getDishId())) {
                jdbcTemplate.update("INSERT INTO user_dish_exclusions (userId, dishId) VALUES (?, ?)",
                        exclusion.getUser().getUserId(), exclusion.getDish().getDishId());
            }
            return findByUserUserIdAndDishId(exclusion.getUser().getUserId(), exclusion.getDish().getDishId()).orElse(exclusion);
        }

        if (exclusion.getIngredient() != null) {
            if (!existsByUserUserIdAndIngredientId(exclusion.getUser().getUserId(), exclusion.getIngredient().getIngredientId())) {
                jdbcTemplate.update("INSERT INTO user_ingredient_exclusions (userId, ingredientId) VALUES (?, ?)",
                        exclusion.getUser().getUserId(), exclusion.getIngredient().getIngredientId());
            }
            return findByUserUserIdAndIngredientId(exclusion.getUser().getUserId(), exclusion.getIngredient().getIngredientId()).orElse(exclusion);
        }

        if (exclusion.getRestaurant() != null) {
            if (!existsByUserUserIdAndRestaurantId(exclusion.getUser().getUserId(), exclusion.getRestaurant().getRestaurantId())) {
                jdbcTemplate.update("INSERT INTO user_restaurant_exclusions (userId, restaurantId) VALUES (?, ?)",
                        exclusion.getUser().getUserId(), exclusion.getRestaurant().getRestaurantId());
            }
            return findByUserUserIdAndRestaurantId(exclusion.getUser().getUserId(), exclusion.getRestaurant().getRestaurantId()).orElse(exclusion);
        }

        return exclusion;
    }

    public void deleteByUserUserIdAndDishId(Integer userId, Integer dishId) {
        jdbcTemplate.update("DELETE FROM user_dish_exclusions WHERE userId = ? AND dishId = ?", userId, dishId);
    }

    public void deleteByUserUserIdAndIngredientId(Integer userId, Integer ingredientId) {
        jdbcTemplate.update("DELETE FROM user_ingredient_exclusions WHERE userId = ? AND ingredientId = ?", userId, ingredientId);
    }

    public void deleteByUserUserIdAndRestaurantId(Integer userId, Integer restaurantId) {
        jdbcTemplate.update("DELETE FROM user_restaurant_exclusions WHERE userId = ? AND restaurantId = ?", userId, restaurantId);
    }

    public void deleteAllInBatch() {
        jdbcTemplate.update("DELETE FROM user_dish_exclusions");
        jdbcTemplate.update("DELETE FROM user_ingredient_exclusions");
        jdbcTemplate.update("DELETE FROM user_restaurant_exclusions");
    }
}
