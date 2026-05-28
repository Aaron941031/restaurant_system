package group19.restaurant_system.controller;

import group19.restaurant_system.dto.ApiResponse;
import group19.restaurant_system.dto.RatingRequest;
import group19.restaurant_system.model.Rating;
import group19.restaurant_system.model.Restaurant;
import group19.restaurant_system.service.RatingService;
import group19.restaurant_system.service.RestaurantService;
import group19.restaurant_system.util.JwtTokenProvider;
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
}
