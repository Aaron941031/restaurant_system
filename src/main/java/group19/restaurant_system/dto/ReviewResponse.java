package group19.restaurant_system.dto;

import java.time.LocalDateTime;

public class ReviewResponse {
    private Integer id;
    private String restaurantName;
    private String userName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private Boolean isEdited;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Boolean getIsEdited() { return isEdited; }
    public void setIsEdited(Boolean isEdited) { this.isEdited = isEdited; }
}