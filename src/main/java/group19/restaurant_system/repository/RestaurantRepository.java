package group19.restaurant_system.repository;

import group19.restaurant_system.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Integer> {
    List<Restaurant> findByCategoryOrderByAvgScoreDesc(String category);
    
    @Query("SELECT r FROM Restaurant r WHERE r.category NOT IN :excludedCategories ORDER BY r.avgScore DESC LIMIT :limit")
    List<Restaurant> findRecommendedRestaurants(@Param("excludedCategories") List<String> excludedCategories, @Param("limit") int limit);
    
    List<Restaurant> findAllByOrderByAvgScoreDesc();
}
