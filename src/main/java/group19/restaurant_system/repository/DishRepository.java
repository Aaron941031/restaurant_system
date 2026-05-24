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
        dish.setDishId(rs.getInt("dishId"));
        dish.setName(rs.getString("name"));
        dish.setPrice(rs.getInt("price"));
        return dish;
    };

    public List<Dish> findAll() {
        return jdbcTemplate.query("SELECT dishId, name FROM dishes ORDER BY dishId", rowMapper);
    }

    public List<Dish> findByNameLike(String q, int limit) {
        String pattern = "%" + q.toLowerCase() + "%";
        return jdbcTemplate.query("SELECT dishId, name FROM dishes WHERE LOWER(name) LIKE ? ORDER BY name LIMIT ?",
                rowMapper, pattern, limit);
    }

    public Optional<Dish> findById(Integer dishId) {
        return queryForOptional("SELECT dishId, name FROM dishes WHERE dishId = ?", dishId);
    }

    public Optional<Dish> findByName(String name) {
        return queryForOptional("SELECT dishId, name FROM dishes WHERE name = ?", name);
    }

    public boolean existsByName(String name) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM dishes WHERE name = ?", Integer.class, name);
        return count != null && count > 0;
    }

    // ================= 新增：用餐廳ID找出關聯的所有菜單 =================
    public List<Dish> findByRestaurantId(Integer restaurantId) {
        // 透過 restaurant_dishes 中介表進行 JOIN 查詢
        String sql = "SELECT d.dishId, d.price, d.name FROM dishes d " +
                     "JOIN restaurant_dishes rd ON d.dishId = rd.dishId " +
                     "WHERE rd.restaurantId = ?";
        return jdbcTemplate.query(sql, rowMapper, restaurantId);
    }

    public Dish save(Dish dish) {
        if (dish.getDishId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO dishes (name,price) VALUES (?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, dish.getName());
                return ps;
            }, keyHolder);

            Number key = keyHolder.getKey();
            if (key != null) {
                return findById(key.intValue()).orElseGet(() -> {
                    dish.setDishId(key.intValue());
                    return dish;
                });
            }
            return dish;
        }

        jdbcTemplate.update("UPDATE dishes SET name = ? , price = ? WHERE dishId = ?", dish.getName(), dish.getDishId());
        return findById(dish.getDishId()).orElse(dish);
    }

    public void deleteById(Integer dishId) {
        jdbcTemplate.update("DELETE FROM dishes WHERE dishId = ?", dishId);
    }

    public void deleteAllInBatch() {
        jdbcTemplate.update("DELETE FROM dishes");
    }

    private Optional<Dish> queryForOptional(String sql, Object... params) {
        List<Dish> results = jdbcTemplate.query(sql, rowMapper, params);
        return results.stream().findFirst();
    }
    
}
