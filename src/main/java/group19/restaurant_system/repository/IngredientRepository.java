package group19.restaurant_system.repository;

import group19.restaurant_system.model.Ingredient;
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
public class IngredientRepository {

    private final JdbcTemplate jdbcTemplate;

    public IngredientRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Ingredient> rowMapper = (rs, rowNum) -> {
        Ingredient ingredient = new Ingredient();
        ingredient.setIngredientId(rs.getInt("ingredient_id"));
        ingredient.setName(rs.getString("name"));
        return ingredient;
    };

    public List<Ingredient> findAll() {
        return jdbcTemplate.query("SELECT ingredient_id, name FROM ingredients ORDER BY ingredient_id", rowMapper);
    }

    public Optional<Ingredient> findById(Integer ingredientId) {
        return queryForOptional("SELECT ingredient_id, name FROM ingredients WHERE ingredient_id = ?", ingredientId);
    }

    public Optional<Ingredient> findByName(String name) {
        return queryForOptional("SELECT ingredient_id, name FROM ingredients WHERE name = ?", name);
    }

    public boolean existsByName(String name) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM ingredients WHERE name = ?", Integer.class, name);
        return count != null && count > 0;
    }

    public Ingredient save(Ingredient ingredient) {
        if (ingredient.getIngredientId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO ingredients (name) VALUES (?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, ingredient.getName());
                return ps;
            }, keyHolder);

            Number key = keyHolder.getKey();
            if (key != null) {
                return findById(key.intValue()).orElseGet(() -> {
                    ingredient.setIngredientId(key.intValue());
                    return ingredient;
                });
            }
            return ingredient;
        }

        jdbcTemplate.update("UPDATE ingredients SET name = ? WHERE ingredient_id = ?", ingredient.getName(), ingredient.getIngredientId());
        return findById(ingredient.getIngredientId()).orElse(ingredient);
    }

    public void deleteById(Integer ingredientId) {
        jdbcTemplate.update("DELETE FROM ingredients WHERE ingredient_id = ?", ingredientId);
    }

    public void deleteAllInBatch() {
        jdbcTemplate.update("DELETE FROM ingredients");
    }

    private Optional<Ingredient> queryForOptional(String sql, Object... params) {
        List<Ingredient> results = jdbcTemplate.query(sql, rowMapper, params);
        return results.stream().findFirst();
    }
}
