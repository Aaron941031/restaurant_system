package group19.restaurant_system.controller;

import group19.restaurant_system.model.Dish;
import group19.restaurant_system.service.DishService;
import group19.restaurant_system.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dish")
@CrossOrigin(origins = "*")
public class DishController {

    @Autowired
    private DishService dishService;

    @GetMapping("/all")
    public ResponseEntity<?> getAllDishes() {
        try {
            List<Dish> dishes = dishService.getAllDishes();
            return ResponseEntity.ok(new ApiResponse<>(true, "Dishes retrieved successfully", dishes));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to retrieve dishes: " + e.getMessage(), null));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchDishes(@RequestParam String q,
                                          @RequestParam(required = false, defaultValue = "10") Integer limit) {
        try {
                List<Dish> filtered = dishService.searchDishes(q, limit);
                return ResponseEntity.ok(new ApiResponse<>(true, "Dishes search results", filtered));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to search dishes: " + e.getMessage(), null));
        }
    }
}