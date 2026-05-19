package group19.restaurant_system.repository;

import group19.restaurant_system.model.Restaurant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class RestaurantRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public RestaurantRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    private final RowMapper<Restaurant> rowMapper = (rs, rowNum) -> {
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId(rs.getInt("restaurant_id"));
        restaurant.setName(rs.getString("name"));
        restaurant.setCategory(rs.getString("category"));
        restaurant.setPriceRange(rs.getString("price_range"));
        restaurant.setAvgScore(rs.getDouble("avg_score"));
        restaurant.setRatingCount(rs.getInt("rating_count"));
        restaurant.setLocationAt(rs.getString("location_at"));
        return restaurant;
    };

    public Optional<Restaurant> findById(Integer restaurantId) {
        return queryForOptional("SELECT restaurant_id, name, category, price_range, avg_score, rating_count, location_at FROM restaurants WHERE restaurant_id = ?", restaurantId);
    }

    public List<Restaurant> findAll() {
        return jdbcTemplate.query("SELECT restaurant_id, name, category, price_range, avg_score, rating_count, location_at FROM restaurants", rowMapper);
    }

    public List<Restaurant> findByCategoryOrderByAvgScoreDesc(String category) {
        return jdbcTemplate.query(
                "SELECT restaurant_id, name, category, price_range, avg_score, rating_count, location_at FROM restaurants WHERE category = ? ORDER BY avg_score DESC",
                rowMapper,
                category
        );
    }

    public List<Restaurant> findRecommendedRestaurants(List<String> excludedCategories, int limit) {
        if (excludedCategories == null) {
            excludedCategories = Collections.emptyList();
        }
        if (excludedCategories.isEmpty()) {
            return jdbcTemplate.query(
                    "SELECT restaurant_id, name, category, price_range, avg_score, rating_count, location_at FROM restaurants ORDER BY avg_score DESC LIMIT ?",
                    rowMapper,
                    limit
            );
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("excludedCategories", excludedCategories);
        params.addValue("limit", limit);

        return namedParameterJdbcTemplate.query(
                "SELECT restaurant_id, name, category, price_range, avg_score, rating_count, location_at FROM restaurants " +
                        "WHERE category NOT IN (:excludedCategories) ORDER BY avg_score DESC LIMIT :limit",
                params,
                rowMapper
        );
    }

    public List<Restaurant> findAllByOrderByAvgScoreDesc() {
        return jdbcTemplate.query(
                "SELECT restaurant_id, name, category, price_range, avg_score, rating_count, location_at FROM restaurants ORDER BY avg_score DESC",
                rowMapper
        );
    }

    public Restaurant save(Restaurant restaurant) {
        if (restaurant.getRestaurantId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO restaurants (name, category, price_range, avg_score, rating_count, location_at) VALUES (?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, restaurant.getName());
                ps.setString(2, restaurant.getCategory());
                ps.setString(3, restaurant.getPriceRange());
                ps.setDouble(4, restaurant.getAvgScore());
                ps.setInt(5, restaurant.getRatingCount());
                ps.setString(6, restaurant.getLocationAt());
                return ps;
            }, keyHolder);

            Number key = keyHolder.getKey();
            if (key != null) {
                return findById(key.intValue()).orElseGet(() -> {
                    restaurant.setRestaurantId(key.intValue());
                    return restaurant;
                });
            }
            return restaurant;
        }

        jdbcTemplate.update(
                "UPDATE restaurants SET name = ?, category = ?, price_range = ?, avg_score = ?, rating_count = ?, location_at = ? WHERE restaurant_id = ?",
                restaurant.getName(),
                restaurant.getCategory(),
                restaurant.getPriceRange(),
                restaurant.getAvgScore(),
                restaurant.getRatingCount(),
                restaurant.getLocationAt(),
                restaurant.getRestaurantId()
        );
        return findById(restaurant.getRestaurantId()).orElse(restaurant);
    }

    public void deleteById(Integer restaurantId) {
        jdbcTemplate.update("DELETE FROM restaurants WHERE restaurant_id = ?", restaurantId);
    }

    public void deleteAllInBatch() {
        jdbcTemplate.update("DELETE FROM restaurants");
    }

    private Optional<Restaurant> queryForOptional(String sql, Object... params) {
        List<Restaurant> results = jdbcTemplate.query(sql, rowMapper, params);
        return results.stream().findFirst();
    }
}
