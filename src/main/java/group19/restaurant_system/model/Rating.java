package group19.restaurant_system.model;

import java.sql.Timestamp;

public class Rating {
    private Integer ratingId;
    private Integer userId;
    private Integer restaurantId;
    private Integer score;
    private String comment;
    private Timestamp ratedAt;

    public Rating() {}

    public Integer getRatingId() { return ratingId; }
    public void setRatingId(Integer ratingId) { this.ratingId = ratingId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Integer restaurantId) { this.restaurantId = restaurantId; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Timestamp getRatedAt() { return ratedAt; }
    public void setRatedAt(Timestamp ratedAt) { this.ratedAt = ratedAt; }

    @Override
    public String toString() {
        return "Rating{" +
                "ratingId=" + ratingId +
                ", userId=" + userId +
                ", restaurantId=" + restaurantId +
                ", score=" + score +
                ", comment='" + comment + '\'' +
                ", ratedAt=" + ratedAt +
                '}';
    }
}