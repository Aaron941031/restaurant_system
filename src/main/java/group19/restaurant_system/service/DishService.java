package group19.restaurant_system.service;

import group19.restaurant_system.model.Dish;
import group19.restaurant_system.repository.DishRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DishService {
    
    @Autowired
    private DishRepository dishRepository;

    public List<Dish> getAllDishes() {
        return dishRepository.findAll();
    }

    public Optional<Dish> getDishById(Integer categoryId) {
        return dishRepository.findById(categoryId);
    }

    public Optional<Dish> getDishByName(String name) {
        return dishRepository.findByName(name);
    }

    public Dish saveDish(Dish dish) {
        return dishRepository.save(dish);
    }

    public void deleteDish(Integer categoryId) {
        dishRepository.deleteById(categoryId);
    }
}
