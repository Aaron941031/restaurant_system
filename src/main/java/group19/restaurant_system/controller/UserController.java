package group19.restaurant_system.controller;

import group19.restaurant_system.dto.ApiResponse;
import group19.restaurant_system.dto.ExclusionBatchResult;
import group19.restaurant_system.dto.IngredientExclusionRequest;
import group19.restaurant_system.model.User;
import group19.restaurant_system.model.UserExclusion;
import group19.restaurant_system.service.UserService;
import group19.restaurant_system.service.UserExclusionService;
import group19.restaurant_system.util.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserExclusionService userExclusionService;
    
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

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            Integer userId = getUserIdFromHeader(authHeader);
            User user = userService.getUserProfile(userId);
            List<UserExclusion> exclusions = userExclusionService.getUserExclusions(userId);
            
            return ResponseEntity.ok(new ApiResponse<>(true, "Profile retrieved", user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @PostMapping("/exclusion")
    public ResponseEntity<?> addExclusion(@RequestHeader("Authorization") String authHeader, 
                                          @RequestParam Integer categoryId) {
        try {
            Integer userId = getUserIdFromHeader(authHeader);
            UserExclusion exclusion = userExclusionService.addExclusion(userId, categoryId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Exclusion saved", exclusion));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @PostMapping("/exclusion/category")
    public ResponseEntity<?> addCategoryExclusion(@RequestHeader("Authorization") String authHeader,
                                                  @RequestParam Integer categoryId) {
        try {
            Integer userId = getUserIdFromHeader(authHeader);
            UserExclusion exclusion = userExclusionService.addExclusion(userId, categoryId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Exclusion saved", exclusion));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @PostMapping("/exclusion/ingredient")
    public ResponseEntity<?> addIngredientExclusions(@RequestHeader("Authorization") String authHeader,
                                                     @RequestBody IngredientExclusionRequest request) {
        try {
            Integer userId = getUserIdFromHeader(authHeader);
            ExclusionBatchResult result = userExclusionService.addIngredientExclusions(userId, request.getIngredients());
            return ResponseEntity.ok(new ApiResponse<>(true, "Exclusions saved", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @GetMapping("/exclusions")
    public ResponseEntity<?> getExclusions(@RequestHeader("Authorization") String authHeader) {
        try {
            Integer userId = getUserIdFromHeader(authHeader);
            List<UserExclusion> exclusions = userExclusionService.getUserExclusions(userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Exclusions retrieved", exclusions));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @DeleteMapping("/exclusion/{categoryId}")
    public ResponseEntity<?> removeExclusion(@RequestHeader("Authorization") String authHeader, 
                                             @PathVariable Integer categoryId) {
        try {
            Integer userId = getUserIdFromHeader(authHeader);
            userExclusionService.removeExclusion(userId, categoryId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Exclusion removed"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage()));
        }
    }
}
