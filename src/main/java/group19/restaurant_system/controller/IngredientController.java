package group19.restaurant_system.controller;

import group19.restaurant_system.dto.ApiResponse;
import group19.restaurant_system.model.Ingredient;
import group19.restaurant_system.repository.IngredientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ingredient")
@CrossOrigin(origins = "*")
public class IngredientController {

    @Autowired
    private IngredientRepository ingredientRepository;

    @GetMapping("/all")
    public ResponseEntity<?> getAllIngredients() {
        try {
            List<Ingredient> ingredients = ingredientRepository.findAll();
            return ResponseEntity.ok(new ApiResponse<>(true, "Ingredients retrieved", ingredients));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to retrieve ingredients: " + e.getMessage(), null));
        }
    }
}
