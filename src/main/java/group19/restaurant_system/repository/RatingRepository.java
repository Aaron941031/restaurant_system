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
public class RatingRepository {

    private final JdbcTemplate jdbcTemplate;

    public RatingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String BASE_SELECT =
            "SELECT r.ratingId, r.score, r.comment, r.ratedAt, " +
                    "u.userId AS u_userId, u.name AS u_name, u.email AS u_email, u.password AS u_password, u.createdAt AS u_createdAt, u.updatedAt AS u_updatedAt, " +
                    "res.restaurantId AS res_restaurantId, res.name AS res_name, res.category AS res_category, res.priceRange AS res_priceRange, " +
                    "res.avgScore AS res_avgScore, res.ratingCount AS res_ratingCount, res.locationAt AS res_locationAt " +
                    "FROM ratings r " +
                    "JOIN users u ON u.userId = r.userId " +
                    "JOIN restaurants res ON res.restaurantId = r.restaurantId ";

    private final RowMapper<Rating> rowMapper = (rs, rowNum) -> {
        Rating rating = new Rating();
        rating.setRatingId(rs.getInt("ratingId"));
        rating.setScore(rs.getInt("score"));
        rating.setComment(rs.getString("comment"));
        Timestamp ratedAt = rs.getTimestamp("ratedAt");
        if (ratedAt != null) {
            rating.setRatedAt(ratedAt.toLocalDateTime());
        }

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
        rating.setUser(user);

        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId(rs.getInt("res_restaurantId"));
        restaurant.setName(rs.getString("res_name"));
        restaurant.setCategory(rs.getString("res_category"));
        restaurant.setPriceRange(rs.getString("res_priceRange"));
        restaurant.setAvgScore(rs.getDouble("res_avgScore"));
        restaurant.setRatingCount(rs.getInt("res_ratingCount"));
        restaurant.setLocationAt(rs.getString("res_locationAt"));
        rating.setRestaurant(restaurant);

        return rating;
    };

    public Optional<Rating> findById(Integer ratingId) {
        List<Rating> results = jdbcTemplate.query(BASE_SELECT + "WHERE r.ratingId = ?", rowMapper, ratingId);
        return results.stream().findFirst();
    }

    public List<Rating> findByRestaurantRestaurantId(Integer restaurantId) {
        return jdbcTemplate.query(BASE_SELECT + "WHERE r.restaurantId = ? ORDER BY r.ratedAt DESC", rowMapper, restaurantId);
    }

    public List<Rating> findByUserUserId(Integer userId) {
        return jdbcTemplate.query(BASE_SELECT + "WHERE r.userId = ? ORDER BY r.ratedAt DESC", rowMapper, userId);
    }

    public boolean existsByUserUserIdAndRestaurantRestaurantId(Integer userId, Integer restaurantId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ratings WHERE userId = ? AND restaurantId = ?",
                Integer.class,
                userId,
                restaurantId
        );
        return count != null && count > 0;
    }

    public Rating save(Rating rating) {
        if (rating.getRatingId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO ratings (userId, restaurantId, score, comment) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setInt(1, rating.getUser().getUserId());
                ps.setInt(2, rating.getRestaurant().getRestaurantId());
                ps.setInt(3, rating.getScore());
                ps.setString(4, rating.getComment());
                return ps;
            }, keyHolder);

            Number key = keyHolder.getKey();
            if (key != null) {
                return findById(key.intValue()).orElseGet(() -> {
                    rating.setRatingId(key.intValue());
                    return rating;
                });
            }
            return rating;
        }

        jdbcTemplate.update(
                "UPDATE ratings SET userId = ?, restaurantId = ?, score = ?, comment = ? WHERE ratingId = ?",
                rating.getUser().getUserId(),
                rating.getRestaurant().getRestaurantId(),
                rating.getScore(),
                rating.getComment(),
                rating.getRatingId()
        );
        return findById(rating.getRatingId()).orElse(rating);
    }

    public void delete(Rating rating) {
        if (rating != null && rating.getRatingId() != null) {
            jdbcTemplate.update("DELETE FROM ratings WHERE ratingId = ?", rating.getRatingId());
        }
    }

    public void deleteAllInBatch() {
        jdbcTemplate.update("DELETE FROM ratings");
    }
}
