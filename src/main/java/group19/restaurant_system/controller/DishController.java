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
@RequestMapping("/dish")
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
}