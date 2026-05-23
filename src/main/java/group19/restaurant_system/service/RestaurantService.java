package group19.restaurant_system.service;

import group19.restaurant_system.model.*;
import group19.restaurant_system.repository.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Map;

import group19.restaurant_system.dto.RestaurantRemainingDto;

@Service
public class RestaurantService {
    
    @Autowired
    private RestaurantRepository restaurantRepository;
    
    @Autowired
    private RatingRepository ratingRepository;
    
    @Autowired
    private UserExclusionRepository userExclusionRepository;

    @Autowired
    private RestaurantDishRepository restaurantDishRepository;

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
        // Get user's excluded dishes
        List<UserExclusion> exclusions = userExclusionRepository.findByUserUserId(userId);
        List<Integer> excludedDishIds = exclusions.stream()
            .filter(ue -> ue.getDish() != null)
            .map(ue -> ue.getDish().getDishId())
            .distinct()
            .collect(Collectors.toList());
        List<Integer> excludedRestaurantIds = exclusions.stream()
            .filter(ue -> ue.getRestaurant() != null)
            .map(ue -> ue.getRestaurant().getRestaurantId())
            .collect(Collectors.toList());
        List<Integer> excludedIngredientIds = exclusions.stream()
            .filter(ue -> ue.getIngredient() != null)
            .map(ue -> ue.getIngredient().getIngredientId())
            .distinct()
            .collect(Collectors.toList());
        List<Integer> dishRestaurantIds = restaurantDishRepository
            .findRestaurantIdsByDishIds(excludedDishIds);
        List<Integer> ingredientRestaurantIds = restaurantDishRepository
            .findRestaurantIdsByIngredientIds(excludedIngredientIds);
        
        // Get recommended restaurants
        List<Restaurant> recommended = restaurantRepository.findAllByOrderByAvgScoreDesc();
        
        if (!dishRestaurantIds.isEmpty()) {
            excludedRestaurantIds.addAll(dishRestaurantIds);
        }
        if (!ingredientRestaurantIds.isEmpty()) {
            excludedRestaurantIds.addAll(ingredientRestaurantIds);
        }

        if (!excludedRestaurantIds.isEmpty()) {
            recommended = recommended.stream()
                .filter(r -> !excludedRestaurantIds.contains(r.getRestaurantId()))
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

    public List<Restaurant> searchRestaurants(String q, int limit) {
        return restaurantRepository.findByNameLike(q, limit);
    }

    @Transactional
        public List<Restaurant> getGroupRecommendations(Integer sessionId,
                                                        List<Integer> memberIds) throws Exception {
        // Collect all excluded dishes from all members
        List<Integer> allExcludedDishIds = memberIds.stream()
            .flatMap(userId -> this.userExclusionRepository.findByUserUserId(userId).stream())
            .filter(ue -> ue.getDish() != null)
            .map(ue -> ue.getDish().getDishId())
            .distinct()
            .collect(Collectors.toList());
        List<Integer> allExcludedRestaurantIds = memberIds.stream()
            .flatMap(userId -> this.userExclusionRepository.findByUserUserId(userId).stream())
            .filter(ue -> ue.getRestaurant() != null)
            .map(ue -> ue.getRestaurant().getRestaurantId())
            .distinct()
            .collect(Collectors.toList());
        List<Integer> allExcludedIngredientIds = memberIds.stream()
            .flatMap(userId -> this.userExclusionRepository.findByUserUserId(userId).stream())
            .filter(ue -> ue.getIngredient() != null)
            .map(ue -> ue.getIngredient().getIngredientId())
            .distinct()
            .collect(Collectors.toList());
        List<Integer> groupDishRestaurantIds = restaurantDishRepository
            .findRestaurantIdsByDishIds(allExcludedDishIds);
        List<Integer> groupIngredientRestaurantIds = restaurantDishRepository
            .findRestaurantIdsByIngredientIds(allExcludedIngredientIds);
        
        List<Restaurant> recommended = restaurantRepository.findAllByOrderByAvgScoreDesc();
        
        if (!groupDishRestaurantIds.isEmpty()) {
            allExcludedRestaurantIds.addAll(groupDishRestaurantIds);
        }
        if (!groupIngredientRestaurantIds.isEmpty()) {
            allExcludedRestaurantIds.addAll(groupIngredientRestaurantIds);
        }

        if (!allExcludedRestaurantIds.isEmpty()) {
            recommended = recommended.stream()
                .filter(r -> !allExcludedRestaurantIds.contains(r.getRestaurantId()))
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

    @Transactional
    public List<RestaurantRemainingDto> getGroupRestaurantsByRemainingDishes(Integer sessionId,
                                                                              List<Integer> memberIds,
                                                                              int limit) throws Exception {
        // Collect all excluded dishes/ingredients from members
        List<Integer> allExcludedDishIds = memberIds.stream()
            .flatMap(userId -> this.userExclusionRepository.findByUserUserId(userId).stream())
            .filter(ue -> ue.getDish() != null)
            .map(ue -> ue.getDish().getDishId())
            .distinct()
            .collect(Collectors.toList());
        List<Integer> allExcludedIngredientIds = memberIds.stream()
            .flatMap(userId -> this.userExclusionRepository.findByUserUserId(userId).stream())
            .filter(ue -> ue.getIngredient() != null)
            .map(ue -> ue.getIngredient().getIngredientId())
            .distinct()
            .collect(Collectors.toList());

        List<Map<String, Object>> rows = this.restaurantDishRepository
            .findRemainingDishCountsByExclusions(allExcludedDishIds, allExcludedIngredientIds, limit);

        List<RestaurantRemainingDto> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Integer restaurantId = ((Number) row.get("restaurantId")).intValue();
            Integer remaining = ((Number) row.get("remainingCount")).intValue();
            Optional<Restaurant> restOpt = this.restaurantRepository.findById(restaurantId);
            restOpt.ifPresent(r -> result.add(new RestaurantRemainingDto(r, remaining)));
        }

        return result;
    }
}
