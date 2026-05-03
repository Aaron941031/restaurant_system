package group19.restaurant_system.service;

import group19.restaurant_system.model.Rating;
import group19.restaurant_system.model.Restaurant;
import group19.restaurant_system.model.User;
import group19.restaurant_system.repository.RatingRepository;
import group19.restaurant_system.repository.RestaurantRepository;
import group19.restaurant_system.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RatingService {
    
    @Autowired
    private RatingRepository ratingRepository;
    
    @Autowired
    private RestaurantRepository restaurantRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RestaurantService restaurantService;

    public List<Rating> getRestaurantRatings(Integer restaurantId) {
        return ratingRepository.findByRestaurantRestaurantId(restaurantId);
    }

    public List<Rating> getUserRatings(Integer userId) {
        return ratingRepository.findByUserUserId(userId);
    }

    @Transactional
    public Rating addRating(Integer userId, Integer restaurantId, Integer score, String comment) throws Exception {
        // Validate score
        if (score < 1 || score > 5) {
            throw new Exception("Score must be between 1 and 5");
        }
        
        // Get user
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new Exception("User not found");
        }
        
        // Get restaurant
        Optional<Restaurant> restaurantOpt = restaurantRepository.findById(restaurantId);
        if (!restaurantOpt.isPresent()) {
            throw new Exception("Restaurant not found");
        }
        
        // Create rating
        Rating rating = new Rating(userOpt.get(), restaurantOpt.get(), score, comment);
        rating = ratingRepository.save(rating);
        
        // Update restaurant average score
        restaurantService.updateRestaurantRating(restaurantId);
        
        return rating;
    }

    public Optional<Rating> getRatingById(Integer ratingId) {
        return ratingRepository.findById(ratingId);
    }

    @Transactional
    public void deleteRating(Integer ratingId) throws Exception {
        Optional<Rating> ratingOpt = ratingRepository.findById(ratingId);
        if (!ratingOpt.isPresent()) {
            throw new Exception("Rating not found");
        }
        
        Rating rating = ratingOpt.get();
        Integer restaurantId = rating.getRestaurant().getRestaurantId();
        
        ratingRepository.delete(rating);
        
        // Update restaurant average score
        restaurantService.updateRestaurantRating(restaurantId);
    }
}
