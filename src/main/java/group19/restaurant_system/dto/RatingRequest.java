package group19.restaurant_system.dto;

public class RatingRequest {
    private Integer restaurantId;
    private Integer score;
    private String comment;

    public RatingRequest() {}

    public Integer getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Integer restaurantId) { this.restaurantId = restaurantId; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
