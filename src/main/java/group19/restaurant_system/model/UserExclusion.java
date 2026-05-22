package group19.restaurant_system.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_exclusions")
public class UserExclusion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer exclusionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoryId")
    private Dish dish;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredientId")
    private Ingredient ingredient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurantId")
    private Restaurant restaurant;

    public UserExclusion() {}

    public UserExclusion(User user, Dish dish) {
        this.user = user;
        this.dish = dish;
    }

    public UserExclusion(User user, Ingredient ingredient) {
        this.user = user;
        this.ingredient = ingredient;
    }

    public UserExclusion(User user, Restaurant restaurant) {
        this.user = user;
        this.restaurant = restaurant;
    }

    // Getters and Setters
    public Integer getExclusionId() { return exclusionId; }
    public void setExclusionId(Integer exclusionId) { this.exclusionId = exclusionId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Dish getDish() { return dish; }
    public void setDish(Dish dish) { this.dish = dish; }

    public Ingredient getIngredient() { return ingredient; }
    public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }

    public Restaurant getRestaurant() { return restaurant; }
    public void setRestaurant(Restaurant restaurant) { this.restaurant = restaurant; }

    @Override
    public String toString() {
        Integer categoryId = dish != null ? dish.getCategoryId() : null;
        Integer ingredientId = ingredient != null ? ingredient.getIngredientId() : null;
        Integer restaurantId = restaurant != null ? restaurant.getRestaurantId() : null;
        return "UserExclusion{" +
                "exclusionId=" + exclusionId +
            ", userId=" + (user != null ? user.getUserId() : null) +
            ", categoryId=" + categoryId +
            ", ingredientId=" + ingredientId +
            ", restaurantId=" + restaurantId +
                '}';
    }
}