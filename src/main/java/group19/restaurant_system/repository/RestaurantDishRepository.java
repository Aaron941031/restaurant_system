package group19.restaurant_system.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class RestaurantDishRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public RestaurantDishRepository(JdbcTemplate jdbcTemplate) {
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    public List<Integer> findRestaurantIdsByDishIds(List<Integer> dishIds) {
        if (dishIds == null || dishIds.isEmpty()) {
            return Collections.emptyList();
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("dishIds", dishIds);

        return namedParameterJdbcTemplate.queryForList(
                "SELECT DISTINCT restaurantId FROM restaurant_dishes WHERE dishId IN (:dishIds)",
                params,
                Integer.class
        );
    }

    public List<Integer> findRestaurantIdsByIngredientIds(List<Integer> ingredientIds) {
        if (ingredientIds == null || ingredientIds.isEmpty()) {
            return Collections.emptyList();
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ingredientIds", ingredientIds);

        return namedParameterJdbcTemplate.queryForList(
                "SELECT DISTINCT rd.restaurantId " +
                        "FROM restaurant_dishes rd " +
                        "JOIN dish_ingredients di ON di.dishId = rd.dishId " +
                        "WHERE di.ingredientId IN (:ingredientIds)",
                params,
                Integer.class
        );
    }
}
