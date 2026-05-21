package group19.restaurant_system.repository;

import group19.restaurant_system.model.Dish;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class DishRepository {

    private final JdbcTemplate jdbcTemplate;

    public DishRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Dish> rowMapper = (rs, rowNum) -> {
        Dish dish = new Dish();
        dish.setCategoryId(rs.getInt("categoryId"));
        dish.setName(rs.getString("name"));
        return dish;
    };

    public List<Dish> findAll() {
        return jdbcTemplate.query("SELECT categoryId, name FROM dishes ORDER BY categoryId", rowMapper);
    }

    public Optional<Dish> findById(Integer categoryId) {
        return queryForOptional("SELECT categoryId, name FROM dishes WHERE categoryId = ?", categoryId);
    }

    public Optional<Dish> findByName(String name) {
        return queryForOptional("SELECT categoryId, name FROM dishes WHERE name = ?", name);
    }

    public boolean existsByName(String name) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM dishes WHERE name = ?", Integer.class, name);
        return count != null && count > 0;
    }

    public Dish save(Dish dish) {
        if (dish.getCategoryId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO dishes (name) VALUES (?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, dish.getName());
                return ps;
            }, keyHolder);

            Number key = keyHolder.getKey();
            if (key != null) {
                return findById(key.intValue()).orElseGet(() -> {
                    dish.setCategoryId(key.intValue());
                    return dish;
                });
            }
            return dish;
        }

        jdbcTemplate.update("UPDATE dishes SET name = ? WHERE categoryId = ?", dish.getName(), dish.getCategoryId());
        return findById(dish.getCategoryId()).orElse(dish);
    }

    public void deleteById(Integer categoryId) {
        jdbcTemplate.update("DELETE FROM dishes WHERE categoryId = ?", categoryId);
    }

    public void deleteAllInBatch() {
        jdbcTemplate.update("DELETE FROM dishes");
    }

    private Optional<Dish> queryForOptional(String sql, Object... params) {
        List<Dish> results = jdbcTemplate.query(sql, rowMapper, params);
        return results.stream().findFirst();
    }
}
