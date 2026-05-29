package group19.restaurant_system.repository;

import group19.restaurant_system.model.Record;
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
public class RecordRepository {

    private final JdbcTemplate jdbcTemplate;

    public RecordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String BASE_SELECT =
            "SELECT r.recordId, r.visitDate, r.mealName, r.note, r.groupSessionId, r.createdAt, r.isEdited, " +
                    "u.userId AS u_userId, u.name AS u_name, u.email AS u_email, u.password AS u_password, " +
                    "res.restaurantId AS res_restaurantId, res.name AS res_name, res.category AS res_category, res.priceRange AS res_priceRange, " +
                    "res.avgScore AS res_avgScore, res.ratingCount AS res_ratingCount, res.locationAt AS res_locationAt " +
                    "FROM records r " +
                    "JOIN users u ON u.userId = r.userId " +
                    "JOIN restaurants res ON res.restaurantId = r.restaurantId ";

    private final RowMapper<Record> rowMapper = (rs, rowNum) -> {
        Record record = new Record();
        record.setRecordId(rs.getInt("recordId"));
        Timestamp visitDate = rs.getTimestamp("visitDate");
        if (visitDate != null) {
            record.setVisitDate(visitDate.toLocalDateTime());
        }
        record.setMealName(rs.getString("mealName"));
        record.setNote(rs.getString("note"));
        record.setGroupSessionId((Integer) rs.getObject("groupSessionId"));
        record.setIsEdited(rs.getBoolean("isEdited"));
        Timestamp createdAt = rs.getTimestamp("createdAt");
        if (createdAt != null) {
            record.setCreatedAt(createdAt.toLocalDateTime());
        }

        User user = new User();
        user.setUserId(rs.getInt("u_userId"));
        user.setName(rs.getString("u_name"));
        user.setEmail(rs.getString("u_email"));
        user.setPassword(rs.getString("u_password"));
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
        List<Record> results = jdbcTemplate.query(BASE_SELECT + "WHERE r.recordId = ?", rowMapper, recordId);
        return results.stream().findFirst();
    }

    public List<Record> findByUserUserIdOrderByCreatedAtDesc(Integer userId) {
        return jdbcTemplate.query(
                BASE_SELECT + "WHERE r.userId = ? ORDER BY r.createdAt DESC",
                rowMapper,
                userId
        );
    }

    public List<Record> findByGroupSessionIdOrderByCreatedAtDesc(Integer groupSessionId) {
        return jdbcTemplate.query(
                BASE_SELECT + "WHERE r.groupSessionId = ? ORDER BY r.createdAt DESC",
                rowMapper,
                groupSessionId
        );
    }

    public Record save(Record record) {
        if (record.getRecordId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO records (userId, restaurantId, visitDate, mealName, note, groupSessionId, createdAt, isEdited) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setInt(1, record.getUser().getUserId());
                ps.setInt(2, record.getRestaurant().getRestaurantId());
                ps.setTimestamp(3, Timestamp.valueOf(record.getVisitDate()));
                ps.setString(4, record.getMealName());
                ps.setString(5, record.getNote());
                ps.setObject(6, record.getGroupSessionId());
                ps.setTimestamp(7, Timestamp.valueOf(record.getCreatedAt()));
                ps.setBoolean(8, Boolean.TRUE.equals(record.getIsEdited()));
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
                "UPDATE records SET userId = ?, restaurantId = ?, visitDate = ?, mealName = ?, note = ?, isEdited = ? WHERE recordId = ?",
                record.getUser().getUserId(),
                record.getRestaurant().getRestaurantId(),
                Timestamp.valueOf(record.getVisitDate()),
                record.getMealName(),
                record.getNote(),
                Boolean.TRUE.equals(record.getIsEdited()),
                record.getRecordId()
        );
        return findById(record.getRecordId()).orElse(record);
    }

    public void delete(Record record) {
        if (record != null && record.getRecordId() != null) {
            jdbcTemplate.update("DELETE FROM records WHERE recordId = ?", record.getRecordId());
        }
    }

    public void deleteAllInBatch() {
        jdbcTemplate.update("DELETE FROM records");
    }
}
