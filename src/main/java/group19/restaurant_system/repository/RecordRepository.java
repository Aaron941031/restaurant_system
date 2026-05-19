package group19.restaurant_system.repository;

import group19.restaurant_system.model.Record;
import group19.restaurant_system.model.Restaurant;
import group19.restaurant_system.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class RecordRepository {

    private final JdbcTemplate jdbcTemplate;

    public RecordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String BASE_SELECT =
            "SELECT r.record_id, r.visit_date, r.meal_name, r.note, r.created_at, " +
                    "u.user_id AS u_userId, u.name AS u_name, u.email AS u_email, u.password AS u_password, u.created_at AS u_createdAt, u.updated_at AS u_updatedAt, " +
                    "res.restaurant_id AS res_restaurantId, res.name AS res_name, res.category AS res_category, res.price_range AS res_priceRange, " +
                    "res.avg_score AS res_avgScore, res.rating_count AS res_ratingCount, res.location_at AS res_locationAt " +
                    "FROM records r " +
                    "JOIN users u ON u.user_id = r.user_id " +
                    "JOIN restaurants res ON res.restaurant_id = r.restaurant_id ";

    private final RowMapper<Record> rowMapper = (rs, rowNum) -> {
        Record record = new Record();
        record.setRecordId(rs.getInt("record_id"));
        Date visitDate = rs.getDate("visit_date");
        if (visitDate != null) {
            record.setVisitDate(visitDate.toLocalDate());
        }
        record.setMealName(rs.getString("meal_name"));
        record.setNote(rs.getString("note"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            record.setCreatedAt(createdAt.toLocalDateTime());
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
        record.setUser(user);

        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId(rs.getInt("res_restaurantId"));
        restaurant.setName(rs.getString("res_name"));
        restaurant.setCategory(rs.getString("res_category"));
        restaurant.setPriceRange(rs.getString("res_priceRange"));
        restaurant.setAvgScore(rs.getDouble("res_avgScore"));
        restaurant.setRatingCount(rs.getInt("res_ratingCount"));
        restaurant.setLocationAt(rs.getString("res_locationAt"));
        record.setRestaurant(restaurant);

        return record;
    };

    public Optional<Record> findById(Integer recordId) {
        List<Record> results = jdbcTemplate.query(BASE_SELECT + "WHERE r.record_id = ?", rowMapper, recordId);
        return results.stream().findFirst();
    }

    public List<Record> findByUserUserIdOrderByCreatedAtDesc(Integer userId) {
        return jdbcTemplate.query(
                BASE_SELECT + "WHERE r.user_id = ? ORDER BY r.created_at DESC",
                rowMapper,
                userId
        );
    }

    public Record save(Record record) {
        if (record.getRecordId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO records (user_id, restaurant_id, visit_date, meal_name, note) VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setInt(1, record.getUser().getUserId());
                ps.setInt(2, record.getRestaurant().getRestaurantId());
                ps.setDate(3, Date.valueOf(record.getVisitDate()));
                ps.setString(4, record.getMealName());
                ps.setString(5, record.getNote());
                return ps;
            }, keyHolder);

            Number key = keyHolder.getKey();
            if (key != null) {
                return findById(key.intValue()).orElseGet(() -> {
                    record.setRecordId(key.intValue());
                    return record;
                });
            }
            return record;
        }

        jdbcTemplate.update(
                "UPDATE records SET user_id = ?, restaurant_id = ?, visit_date = ?, meal_name = ?, note = ? WHERE record_id = ?",
                record.getUser().getUserId(),
                record.getRestaurant().getRestaurantId(),
                Date.valueOf(record.getVisitDate()),
                record.getMealName(),
                record.getNote(),
                record.getRecordId()
        );
        return findById(record.getRecordId()).orElse(record);
    }

    public void delete(Record record) {
        if (record != null && record.getRecordId() != null) {
            jdbcTemplate.update("DELETE FROM records WHERE record_id = ?", record.getRecordId());
        }
    }

    public void deleteAllInBatch() {
        jdbcTemplate.update("DELETE FROM records");
    }
}
