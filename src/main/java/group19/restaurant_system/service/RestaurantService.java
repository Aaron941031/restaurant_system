package group19.restaurant_system.service;

import group19.restaurant_system.model.*;
import group19.restaurant_system.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RestaurantService {
    
    @Autowired
    private RestaurantRepository restaurantRepository;
    
    @Autowired
    private RatingRepository ratingRepository;
    
    @Autowired
    private UserExclusionRepository userExclusionRepository;

    public Optional<Restaurant> getRestaurantById(Integer restaurantId) {
        return restaurantRepository.findById(restaurantId);
    }

    public List<Restaurant> getAllRestaurants() {
        return restaurantRepository.findAll();
    }

    @Transactional
    public Restaurant saveRestaurant(Restaurant restaurant) {
        return restaurantRepository.save(restaurant);
    }

    @Transactional
    public void updateRestaurantRating(Integer restaurantId) throws Exception {
        Optional<Restaurant> restaurantOpt = restaurantRepository.findById(restaurantId);
        if (!restaurantOpt.isPresent()) {
            throw new Exception("Restaurant not found");
        }
        
        Restaurant restaurant = restaurantOpt.get();
        List<Rating> ratings = ratingRepository.findByRestaurantRestaurantId(restaurantId);
        
        if (ratings.isEmpty()) {
            restaurant.setAvgScore(0.0);
            restaurant.setRatingCount(0);
        } else {
            double avgScore = ratings.stream()
                    .mapToDouble(Rating::getScore)
                    .average()
                    .orElse(0.0);
            
            restaurant.setAvgScore(Math.round(avgScore * 10.0) / 10.0);
            restaurant.setRatingCount(ratings.size());
        }
        
        restaurantRepository.save(restaurant);
    }

    @Transactional
    public List<Restaurant> getRecommendedRestaurants(Integer userId) throws Exception {
        // Get user's excluded categories
        List<UserExclusion> exclusions = userExclusionRepository.findByUserUserId(userId);
        List<String> excludedCategories = exclusions.stream()
                .map(ue -> ue.getDish().getName())
                .collect(Collectors.toList());
        
        // Get recommended restaurants
        List<Restaurant> recommended = restaurantRepository.findAllByOrderByAvgScoreDesc();
        
        if (!excludedCategories.isEmpty()) {
            recommended = recommended.stream()
                    .filter(r -> !excludedCategories.contains(r.getCategory()))
                    .collect(Collectors.toList());
        }
        
        // If no results, use fallback: return top-rated restaurants
        if (recommended.isEmpty()) {
            recommended = restaurantRepository.findAllByOrderByAvgScoreDesc();
        }
        
        // Return top 5
        return recommended.stream()
                .limit(5)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<Restaurant> getGroupRecommendations(Integer sessionId,
                                                    List<Integer> memberIds,
                                                    UserExclusionRepository userExclusionRepository) throws Exception {
        // Collect all excluded categories from all members
        List<String> allExcludedCategories = memberIds.stream()
                .flatMap(userId -> userExclusionRepository.findByUserUserId(userId).stream())
                .map(ue -> ue.getDish().getName())
                .distinct()
                .collect(Collectors.toList());
        
        List<Restaurant> recommended = restaurantRepository.findAllByOrderByAvgScoreDesc();
        
        if (!allExcludedCategories.isEmpty()) {
            recommended = recommended.stream()
                    .filter(r -> !allExcludedCategories.contains(r.getCategory()))
                    .collect(Collectors.toList());
        }
        
        // If no results, use fallback
        if (recommended.isEmpty()) {
            recommended = restaurantRepository.findAllByOrderByAvgScoreDesc();
        }
        
        return recommended.stream()
                .limit(5)
                .collect(Collectors.toList());
    }
}
