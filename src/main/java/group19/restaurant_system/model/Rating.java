package group19.restaurant_system.model;

import java.time.LocalDateTime;
public class Rating {
    
    private Integer ratingId;
    
    private User user;
    
    private Restaurant restaurant;
    
    private Integer score;
    
    private String comment;
    
    private LocalDateTime ratedAt;

    public Rating() {
        this.ratedAt = LocalDateTime.now();
    }

    public Rating(User user, Restaurant restaurant, Integer score, String comment) {
        this.user = user;
        this.restaurant = restaurant;
        this.score = score;
        this.comment = comment;
        this.ratedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getRatingId() { return ratingId; }
    public void setRatingId(Integer ratingId) { this.ratingId = ratingId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Restaurant getRestaurant() { return restaurant; }
    public void setRestaurant(Restaurant restaurant) { this.restaurant = restaurant; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getRatedAt() { return ratedAt; }
    public void setRatedAt(LocalDateTime ratedAt) { this.ratedAt = ratedAt; }

    @Override
    public String toString() {
        return "Rating{" +
                "ratingId=" + ratingId +
                ", userId=" + user.getUserId() +
                ", restaurantId=" + restaurant.getRestaurantId() +
                ", score=" + score +
                ", comment='" + comment + '\'' +
                ", ratedAt=" + ratedAt +
                '}';
    }
}