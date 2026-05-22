package group19.restaurant_system.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class RestaurantIngredientRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public RestaurantIngredientRepository(JdbcTemplate jdbcTemplate) {
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    public List<Integer> findRestaurantIdsByIngredientIds(List<Integer> ingredientIds) {
        if (ingredientIds == null || ingredientIds.isEmpty()) {
            return Collections.emptyList();
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ingredientIds", ingredientIds);

        return namedParameterJdbcTemplate.queryForList(
                "SELECT DISTINCT restaurantId FROM restaurant_ingredients WHERE ingredientId IN (:ingredientIds)",
                params,
                Integer.class
        );
    }
}
