package group19.restaurant_system.controller;

import group19.restaurant_system.dto.ApiResponse;
import group19.restaurant_system.model.Restaurant;
import group19.restaurant_system.service.RestaurantService;
import group19.restaurant_system.util.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recommend")
@CrossOrigin(origins = "*")
public class RecommendationController {
    
    @Autowired
    private RestaurantService restaurantService;
    
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

    @GetMapping("/personal")
    public ResponseEntity<?> getPersonalRecommendations(@RequestHeader("Authorization") String authHeader) {
        try {
            Integer userId = getUserIdFromHeader(authHeader);
            List<Restaurant> recommendations = restaurantService.getRecommendedRestaurants(userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Recommendations retrieved", recommendations));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @GetMapping("/group/{sessionId}")
    public ResponseEntity<?> getGroupRecommendations(@RequestHeader("Authorization") String authHeader,
                                                     @PathVariable Integer sessionId) {
        try {
            Integer userId = getUserIdFromHeader(authHeader);
            // For now, get personal recommendations
            // In a production system, you would fetch all members and their exclusions
            List<Restaurant> recommendations = restaurantService.getRecommendedRestaurants(userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Group recommendations retrieved", recommendations));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
        }
    }
}
