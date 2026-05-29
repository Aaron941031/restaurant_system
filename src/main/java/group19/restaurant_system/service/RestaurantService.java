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
import java.util.HashMap;
import java.util.Comparator;

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
        // Get user's excluded dishes/ingredients/restaurants
        List<UserExclusion> exclusions = userExclusionRepository.findByUserUserId(userId);
        List<Integer> excludedDishIds = exclusions.stream()
            .filter(ue -> ue.getDish() != null)
            .map(ue -> ue.getDish().getDishId())
            .distinct()
            .collect(Collectors.toList());
        List<Integer> excludedRestaurantIds = exclusions.stream()
            .filter(ue -> ue.getRestaurant() != null)
            .map(ue -> ue.getRestaurant().getRestaurantId())
            .distinct()
            .collect(Collectors.toList());
        List<Integer> excludedIngredientIds = exclusions.stream()
            .filter(ue -> ue.getIngredient() != null)
            .map(ue -> ue.getIngredient().getIngredientId())
            .distinct()
            .collect(Collectors.toList());

        // Determine how many non-excluded dishes remain per restaurant
        List<Map<String, Object>> rows = restaurantDishRepository
            .findRemainingDishCountsByExclusions(excludedDishIds, excludedIngredientIds, 1000);

        Map<Integer, Integer> remainingMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Integer restaurantId = ((Number) row.get("restaurantId")).intValue();
            Integer remaining = ((Number) row.get("remainingCount")).intValue();
            remainingMap.put(restaurantId, remaining);
        }

        // Start from all restaurants ordered by avgScore
        List<Restaurant> allByScore = restaurantRepository.findAllByOrderByAvgScoreDesc();

        // Filter out explicitly excluded restaurants and those with zero remaining dishes
        List<Restaurant> candidates = allByScore.stream()
            .filter(r -> !excludedRestaurantIds.contains(r.getRestaurantId()))
            .filter(r -> remainingMap.containsKey(r.getRestaurantId()) && remainingMap.get(r.getRestaurantId()) > 0)
            .sorted(Comparator
                .comparing((Restaurant r) -> remainingMap.get(r.getRestaurantId()))
                .reversed()
                .thenComparing(Restaurant::getAvgScore, Comparator.reverseOrder())
            )
            .limit(5)
            .collect(Collectors.toList());

        // Fallback: if no candidate after applying exclusions, fall back to top-rated non-excluded restaurants
        if (candidates.isEmpty()) {
            candidates = allByScore.stream()
                .filter(r -> !excludedRestaurantIds.contains(r.getRestaurantId()))
                .limit(5)
                .collect(Collectors.toList());
        }

        return candidates;
    }

    public List<Restaurant> searchRestaurants(String q, int limit) {
        return restaurantRepository.findByNameLike(q, limit);
    }

    private List<Integer> collectExcludedDishIds(List<Integer> memberIds) {
        return memberIds.stream()
            .flatMap(userId -> this.userExclusionRepository.findByUserUserId(userId).stream())
            .filter(ue -> ue.getDish() != null)
            .map(ue -> ue.getDish().getDishId())
            .distinct()
            .collect(Collectors.toList());
    }

    private List<Integer> collectExcludedIngredientIds(List<Integer> memberIds) {
        return memberIds.stream()
            .flatMap(userId -> this.userExclusionRepository.findByUserUserId(userId).stream())
            .filter(ue -> ue.getIngredient() != null)
            .map(ue -> ue.getIngredient().getIngredientId())
            .distinct()
            .collect(Collectors.toList());
    }

    private List<Integer> collectExcludedRestaurantIds(List<Integer> memberIds) {
        return memberIds.stream()
            .flatMap(userId -> this.userExclusionRepository.findByUserUserId(userId).stream())
            .filter(ue -> ue.getRestaurant() != null)
            .map(ue -> ue.getRestaurant().getRestaurantId())
            .distinct()
            .collect(Collectors.toList());
    }

    private Map<Integer, Integer> buildRemainingCountMap(List<Integer> excludedDishIds,
                                                         List<Integer> excludedIngredientIds,
                                                         int limit) {
        List<Map<String, Object>> rows = this.restaurantDishRepository
            .findRemainingDishCountsByExclusions(excludedDishIds, excludedIngredientIds, limit);

        Map<Integer, Integer> remainingMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Integer restaurantId = ((Number) row.get("restaurantId")).intValue();
            Integer remaining = ((Number) row.get("remainingCount")).intValue();
            remainingMap.put(restaurantId, remaining);
        }
        return remainingMap;
    }

    @Transactional
        public List<Restaurant> getGroupRecommendations(Integer sessionId,
                                                        List<Integer> memberIds) throws Exception {
        List<Integer> excludedDishIds = collectExcludedDishIds(memberIds);
        List<Integer> excludedIngredientIds = collectExcludedIngredientIds(memberIds);
        List<Integer> excludedRestaurantIds = collectExcludedRestaurantIds(memberIds);

        Map<Integer, Integer> remainingMap = buildRemainingCountMap(excludedDishIds, excludedIngredientIds, 1000);

        List<Restaurant> allByScore = restaurantRepository.findAllByOrderByAvgScoreDesc();
        List<Restaurant> candidates = allByScore.stream()
            .filter(r -> !excludedRestaurantIds.contains(r.getRestaurantId()))
            .filter(r -> remainingMap.containsKey(r.getRestaurantId()) && remainingMap.get(r.getRestaurantId()) > 0)
            .sorted(Comparator
                .comparing((Restaurant r) -> remainingMap.get(r.getRestaurantId()))
                .reversed()
                .thenComparing(Restaurant::getAvgScore, Comparator.reverseOrder())
            )
            .limit(5)
            .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            candidates = allByScore.stream()
                .filter(r -> !excludedRestaurantIds.contains(r.getRestaurantId()))
                .limit(5)
                .collect(Collectors.toList());
        }

        return candidates;
    }

    @Transactional
    public List<RestaurantRemainingDto> getGroupRestaurantsByRemainingDishes(Integer sessionId,
                                                                              List<Integer> memberIds,
                                                                              int limit) throws Exception {
        List<Integer> allExcludedDishIds = collectExcludedDishIds(memberIds);
        List<Integer> allExcludedIngredientIds = collectExcludedIngredientIds(memberIds);

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
