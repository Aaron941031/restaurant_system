package group19.restaurant_system.repository;

import group19.restaurant_system.model.Rating;
import group19.restaurant_system.model.Restaurant;
import group19.restaurant_system.model.User;
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
public class RecommendRepository {

   
    private JdbcTemplate jdbcTemplate;

    public RecommendRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Restaurant> getPersonalRecommendations(int userId) {
        String sql = 
            "SELECT r.*\n" +
            "FROM restaurants r\n" +
            "WHERE\n" +
            "    r.restaurantId NOT IN (\n" +
            "        SELECT DISTINCT rd.restaurantId\n" +
            "        FROM restaurant_dishes rd\n" +
            "        JOIN user_dish_exclusions ude ON rd.dishId = ude.dishId\n" +
            "        WHERE ude.userId = ?\n" +
            "    )\n" +
            "    AND r.restaurantId NOT IN (\n" +
            "        SELECT DISTINCT rd.restaurantId\n" +
            "        FROM restaurant_dishes rd\n" +
            "        JOIN dish_ingredients di ON di.dishId = rd.dishId\n" +
            "        JOIN user_ingredient_exclusions uie ON uie.ingredientId = di.ingredientId\n" +
            "        WHERE uie.userId = ?\n" +
            "    )\n" +
            "ORDER BY r.avgScore DESC";

        return jdbcTemplate.query(sql,
            (rs, rowNum) -> {
                Restaurant restaurant = new Restaurant();
                restaurant.setRestaurantId(rs.getInt("restaurantId"));
                restaurant.setName(rs.getString("name"));
                restaurant.setCategory(rs.getString("category"));
                restaurant.setPriceRange(rs.getString("priceRange"));
                restaurant.setAvgScore(rs.getDouble("avgScore"));
                restaurant.setRatingCount(rs.getInt("ratingCount"));
                restaurant.setLocationAt(rs.getString("locationAt"));
                return restaurant;
            },
            userId, userId  // 兩個 ? 各對應一次
        );
    }
}