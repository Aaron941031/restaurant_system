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

    public List<Restaurant> getPersonalRecommendations(int userId) {
        String sql = """
            SELECT r.*
            FROM restaurants r
            WHERE
                -- 排除菜系
                r.category NOT IN (
                    SELECT d.name
                    FROM user_exclusions ue
                    JOIN dishes d ON ue.categoryId = d.categoryId
                    WHERE ue.userId = ? AND ue.categoryId IS NOT NULL
                )
                -- 排除含有指定食材的餐廳
                AND r.restaurantId NOT IN (
                    SELECT ri.restaurantId
                    FROM restaurant_ingredients ri
                    JOIN user_exclusions ue ON ri.ingredientId = ue.ingredientId
                    WHERE ue.userId = ? AND ue.ingredientId IS NOT NULL
                )
            ORDER BY r.avgScore DESC
            """;

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