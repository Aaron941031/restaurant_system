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

    public List<java.util.Map<String, Object>> findRemainingDishCountsByExclusions(List<Integer> excludedDishIds, List<Integer> excludedIngredientIds, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource();

        // Use sentinel values when lists are empty to keep SQL simple
        if (excludedDishIds == null || excludedDishIds.isEmpty()) {
            params.addValue("excludedDishIds", java.util.List.of(-1));
        } else {
            params.addValue("excludedDishIds", excludedDishIds);
        }

        if (excludedIngredientIds == null || excludedIngredientIds.isEmpty()) {
            params.addValue("excludedIngredientIds", java.util.List.of(-1));
        } else {
            params.addValue("excludedIngredientIds", excludedIngredientIds);
        }
        params.addValue("limit", limit);

        String sql = "SELECT rd.restaurantId, COUNT(DISTINCT rd.dishId) AS remainingCount " +
                "FROM restaurant_dishes rd " +
                "LEFT JOIN dish_ingredients di ON di.dishId = rd.dishId " +
                "WHERE rd.dishId NOT IN (:excludedDishIds) " +
                "AND rd.dishId NOT IN (SELECT dishId FROM dish_ingredients WHERE ingredientId IN (:excludedIngredientIds)) " +
                "GROUP BY rd.restaurantId " +
                "HAVING remainingCount > 0 " +
                "ORDER BY remainingCount DESC " +
                "LIMIT :limit";

        return namedParameterJdbcTemplate.queryForList(sql, params);
    }
}
