package group19.restaurant_system.service;

import group19.restaurant_system.dto.ReviewResponse;
import group19.restaurant_system.model.Rating;
import group19.restaurant_system.model.Restaurant;
import group19.restaurant_system.model.User;
import group19.restaurant_system.repository.RatingRepository;
import group19.restaurant_system.repository.RestaurantRepository;
import group19.restaurant_system.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
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

    // 新增方法 A：根據用戶 ID 取得所有評論紀錄
    public List<ReviewResponse> getReviewsByUserId(Integer userId) { // ⚠️ 參數改為 Integer
        List<Rating> ratings = ratingRepository.findByUserUserId(userId);
        
        return ratings.stream().map(rating -> {
            ReviewResponse dto = new ReviewResponse();
            // 這裡就不會報錯了，因為兩邊都是 Integer
            dto.setId(rating.getRatingId()); 
            dto.setRating(rating.getScore());
            dto.setComment(rating.getComment());
            dto.setCreatedAt(rating.getRatedAt());
            dto.setRestaurantName(rating.getRestaurant() != null ? rating.getRestaurant().getName() : "未知餐廳");
            return dto;
        }).collect(java.util.stream.Collectors.toList());
    }

    // 新增方法 B：刪除評論（含安全檢查）
    public void deleteReview(Integer ratingId, Integer userId) { // ⚠️ 參數改為 Integer
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該評論"));
        
        // 檢查是否為本人
        if (!rating.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("無權刪除此評論");
        }
        
        // 你們原本就有 delete 方法了，直接用這個即可
        ratingRepository.delete(rating); 
    }
}
