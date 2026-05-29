package group19.restaurant_system.controller;

import group19.restaurant_system.dto.ApiResponse;
import group19.restaurant_system.dto.RatingRequest;
import group19.restaurant_system.model.Rating;
import group19.restaurant_system.model.Restaurant;
import group19.restaurant_system.service.RatingService;
import group19.restaurant_system.service.RestaurantService;
import group19.restaurant_system.util.JwtTokenProvider;
import group19.restaurant_system.dto.ReviewResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurant")
@CrossOrigin(origins = "*")
public class RestaurantController {
    
    @Autowired
    private RestaurantService restaurantService;
    
    @Autowired
    private RatingService ratingService;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Integer getUserIdFromHeader(String authHeader) throws Exception {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new Exception("Missing or invalid authorization header");
        }
        String jwt = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(jwt)) {
            throw new Exception("Invalid token");
        }
        return jwtTokenProvider.getUserIdFromToken(jwt);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllRestaurants() {
        try {
            List<Restaurant> restaurants = restaurantService.getAllRestaurants();
            return ResponseEntity.ok(new ApiResponse<>(true, "Restaurants retrieved", restaurants));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchRestaurants(@RequestParam String q,
                                               @RequestParam(required = false, defaultValue = "10") Integer limit) {
        try {
            List<Restaurant> restaurants = restaurantService.searchRestaurants(q, limit);
            return ResponseEntity.ok(new ApiResponse<>(true, "Restaurants search results", restaurants));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to search restaurants: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRestaurant(@PathVariable Integer id) {
        try {
            var restaurant = restaurantService.getRestaurantById(id);
            if (restaurant.isPresent()) {
                return ResponseEntity.ok(new ApiResponse<>(true, "Restaurant retrieved", restaurant.get()));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(false, "Restaurant not found"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @PostMapping("/rate")
    public ResponseEntity<?> rateRestaurant(@RequestHeader("Authorization") String authHeader,
                                            @RequestBody RatingRequest request) {
        try {
            Integer userId = getUserIdFromHeader(authHeader);
            Rating rating = ratingService.addRating(userId, request.getRestaurantId(), request.getScore(), request.getComment());
            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, "Rating added", rating));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @GetMapping("/{id}/ratings")
    public ResponseEntity<?> getRestaurantRatings(@PathVariable Integer id) {
        try {
            List<Rating> ratings = ratingService.getRestaurantRatings(id);
            return ResponseEntity.ok(new ApiResponse<>(true, "Ratings retrieved", ratings));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    // 2. 新增「取得當前登入使用者的所有評論」API
    @GetMapping("/reviews/me")
    public ResponseEntity<?> getMyReviews(@RequestHeader("Authorization") String authHeader) {
        try {
            // 透過你們寫好的方法解析 Token，取得 Integer 型別的 userId
            Integer userId = getUserIdFromHeader(authHeader);
            
            // ⚠️ 如果你的 DTO 檔案叫做 ReviewResponseDTO，這裡的 ReviewResponse 要改名
            List<ReviewResponse> myReviews = ratingService.getReviewsByUserId(userId);
            
            return ResponseEntity.ok(new ApiResponse<>(true, "獲取評論成功", myReviews));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "無法獲取評論：" + e.getMessage()));
        }
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<?> deleteReview(@PathVariable Integer reviewId, @RequestHeader("Authorization") String authHeader) {
        try {
            Integer userId = getUserIdFromHeader(authHeader);
            ratingService.deleteReview(reviewId, userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "刪除成功", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, "刪除失敗：" + e.getMessage()));
        }
    }

    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<?> updateReview(@PathVariable Integer reviewId,
                                          @RequestHeader("Authorization") String authHeader,
                                          @RequestBody java.util.Map<String, Object> body) {
        try {
            Integer userId = getUserIdFromHeader(authHeader);
            Integer score = (Integer) body.get("score");
            String comment = (String) body.get("comment");
            ratingService.updateReview(reviewId, userId, score, comment);
            return ResponseEntity.ok(new ApiResponse<>(true, "編輯成功", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, "編輯失敗：" + e.getMessage()));
        }
    }
}
