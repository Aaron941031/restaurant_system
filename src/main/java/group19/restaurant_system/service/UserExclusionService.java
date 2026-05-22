package group19.restaurant_system.service;

import group19.restaurant_system.dto.ExclusionBatchResult;
import group19.restaurant_system.model.UserExclusion;
import group19.restaurant_system.model.User;
import group19.restaurant_system.model.Dish;
import group19.restaurant_system.model.Ingredient;
import group19.restaurant_system.model.Restaurant;
import group19.restaurant_system.repository.UserExclusionRepository;
import group19.restaurant_system.repository.UserRepository;
import group19.restaurant_system.repository.DishRepository;
import group19.restaurant_system.repository.IngredientRepository;
import group19.restaurant_system.repository.RestaurantRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserExclusionService {
    
    @Autowired
    private UserExclusionRepository userExclusionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DishRepository dishRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    public List<UserExclusion> getUserExclusions(Integer userId) {
        return userExclusionRepository.findByUserUserId(userId);
    }

    @Transactional
    public UserExclusion addExclusion(Integer userId, Integer categoryId) throws Exception {
        // Get user
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new Exception("User not found");
        }
        
        // Get dish
        Optional<Dish> dishOpt = dishRepository.findById(categoryId);
        if (!dishOpt.isPresent()) {
            throw new Exception("Dish category not found");
        }

        // Return existing record if already stored
        Optional<UserExclusion> existing = userExclusionRepository.findByUserUserIdAndDishCategoryId(userId, categoryId);
        if (existing.isPresent()) {
            return existing.get();
        }

        UserExclusion exclusion = new UserExclusion(userOpt.get(), dishOpt.get());
        return userExclusionRepository.save(exclusion);
    }

    @Transactional
    public ExclusionBatchResult addIngredientExclusions(Integer userId, String ingredientsText) throws Exception {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new Exception("User not found");
        }

        List<String> ingredientNames = parseIngredientNames(ingredientsText);
        if (ingredientNames.isEmpty()) {
            throw new Exception("No ingredients provided");
        }

        List<String> added = new ArrayList<>();
        List<String> existing = new ArrayList<>();
        List<String> notFound = new ArrayList<>();

        for (String name : ingredientNames) {
            Optional<Ingredient> ingredientOpt = ingredientRepository.findByName(name);
            if (!ingredientOpt.isPresent()) {
                notFound.add(name);
                continue;
            }

            Ingredient ingredient = ingredientOpt.get();
            if (userExclusionRepository.existsByUserUserIdAndIngredientId(userId, ingredient.getIngredientId())) {
                existing.add(name);
                continue;
            }

            UserExclusion exclusion = new UserExclusion(userOpt.get(), ingredient);
            userExclusionRepository.save(exclusion);
            added.add(name);
        }

        return new ExclusionBatchResult(added, existing, notFound);
    }

    @Transactional
    public UserExclusion addRestaurantExclusion(Integer userId, Integer restaurantId) throws Exception {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new Exception("User not found");
        }

        Optional<Restaurant> restaurantOpt = restaurantRepository.findById(restaurantId);
        if (!restaurantOpt.isPresent()) {
            throw new Exception("Restaurant not found");
        }

        Optional<UserExclusion> existing = userExclusionRepository.findByUserUserIdAndRestaurantId(userId, restaurantId);
        if (existing.isPresent()) {
            return existing.get();
        }

        UserExclusion exclusion = new UserExclusion(userOpt.get(), restaurantOpt.get());
        return userExclusionRepository.save(exclusion);
    }

    private List<String> parseIngredientNames(String ingredientsText) {
        if (ingredientsText == null) {
            return new ArrayList<>();
        }

        String normalized = ingredientsText
                .replace("\n", ",")
                .replace("，", ",")
                .replace(";", ",")
                .replace("；", ",");

        String[] rawItems = normalized.split(",");
        Set<String> names = new LinkedHashSet<>();
        for (String item : rawItems) {
            String value = item.trim();
            if (!value.isEmpty()) {
                names.add(value);
            }
        }

        return names.stream().collect(Collectors.toList());
    }

    @Transactional
    public void removeExclusion(Integer userId, Integer categoryId) throws Exception {
        if (!userExclusionRepository.existsByUserUserIdAndDishCategoryId(userId, categoryId)) {
            throw new Exception("Exclusion not found");
        }
        
        userExclusionRepository.deleteByUserUserIdAndDishCategoryId(userId, categoryId);
    }

    @Transactional
    public void removeIngredientExclusion(Integer userId, Integer ingredientId) throws Exception {
        if (!userExclusionRepository.existsByUserUserIdAndIngredientId(userId, ingredientId)) {
            throw new Exception("Exclusion not found");
        }

        userExclusionRepository.deleteByUserUserIdAndIngredientId(userId, ingredientId);
    }

    @Transactional
    public void removeRestaurantExclusion(Integer userId, Integer restaurantId) throws Exception {
        if (!userExclusionRepository.existsByUserUserIdAndRestaurantId(userId, restaurantId)) {
            throw new Exception("Exclusion not found");
        }

        userExclusionRepository.deleteByUserUserIdAndRestaurantId(userId, restaurantId);
    }
}
