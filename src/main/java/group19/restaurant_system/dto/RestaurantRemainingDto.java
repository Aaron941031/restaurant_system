package group19.restaurant_system.dto;

import group19.restaurant_system.model.Restaurant;

public class RestaurantRemainingDto {
    private Restaurant restaurant;
    private Integer remainingDishCount;

    public RestaurantRemainingDto() {}

    public RestaurantRemainingDto(Restaurant restaurant, Integer remainingDishCount) {
        this.restaurant = restaurant;
        this.remainingDishCount = remainingDishCount;
    }

    public Restaurant getRestaurant() { return restaurant; }
    public void setRestaurant(Restaurant restaurant) { this.restaurant = restaurant; }

    public Integer getRemainingDishCount() { return remainingDishCount; }
    public void setRemainingDishCount(Integer remainingDishCount) { this.remainingDishCount = remainingDishCount; }
}
