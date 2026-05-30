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
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recommend")
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

    @GetMapping("/personal/random")
    public ResponseEntity<?> getRandomPersonalRecommendations(@RequestHeader("Authorization") String authHeader,
                                                              @RequestParam(required = false, defaultValue = "") String excludeIds,
                                                              @RequestParam(required = false, defaultValue = "5") Integer limit) {
        try {
            Integer userId = getUserIdFromHeader(authHeader);
            List<Integer> excluded = Arrays.stream(excludeIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::valueOf)
                    .collect(Collectors.toList());
            List<Restaurant> recommendations = restaurantService.getRandomRecommendedRestaurants(userId, excluded, limit);
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
