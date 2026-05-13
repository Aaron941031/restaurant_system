package group19.restaurant_system.service;

import group19.restaurant_system.model.UserExclusion;
import group19.restaurant_system.model.User;
import group19.restaurant_system.model.Dish;
import group19.restaurant_system.repository.UserExclusionRepository;
import group19.restaurant_system.repository.UserRepository;
import group19.restaurant_system.repository.DishRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserExclusionService {
    
    @Autowired
    private UserExclusionRepository userExclusionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DishRepository dishRepository;

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
        
        // Check if already exists
        if (userExclusionRepository.existsByUserUserIdAndDishCategoryId(userId, categoryId)) {
            throw new Exception("This exclusion already exists");
        }
        
        UserExclusion exclusion = new UserExclusion(userOpt.get(), dishOpt.get());
        return userExclusionRepository.save(exclusion);
    }

    @Transactional
    public void removeExclusion(Integer userId, Integer categoryId) throws Exception {
        if (!userExclusionRepository.existsByUserUserIdAndDishCategoryId(userId, categoryId)) {
            throw new Exception("Exclusion not found");
        }
        
        userExclusionRepository.deleteByUserUserIdAndDishCategoryId(userId, categoryId);
    }
}
