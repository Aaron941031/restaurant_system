package group19.restaurant_system.repository;

import group19.restaurant_system.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Integer> {
    List<Rating> findByRestaurantRestaurantId(Integer restaurantId);
    List<Rating> findByUserUserId(Integer userId);
    boolean existsByUserUserIdAndRestaurantRestaurantId(Integer userId, Integer restaurantId);
}
