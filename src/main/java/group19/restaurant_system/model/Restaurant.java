package group19.restaurant_system.model;

public class Restaurant {
    private Integer restaurantId;
    private String name;
    private String category;
    private String priceRange;
    private Double avgScore;
    private Integer ratingCount;
    private String locationAt;

    public Restaurant() {}

    public Integer getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Integer restaurantId) { this.restaurantId = restaurantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPriceRange() { return priceRange; }
    public void setPriceRange(String priceRange) { this.priceRange = priceRange; }

    public Double getAvgScore() { return avgScore; }
    public void setAvgScore(Double avgScore) { this.avgScore = avgScore; }

    public Integer getRatingCount() { return ratingCount; }
    public void setRatingCount(Integer ratingCount) { this.ratingCount = ratingCount; }

    public String getLocationAt() { return locationAt; }
    public void setLocationAt(String locationAt) { this.locationAt = locationAt; }

    @Override
    public String toString() {
        return "Restaurant{" +
                "restaurantId=" + restaurantId +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", priceRange='" + priceRange + '\'' +
                ", avgScore=" + avgScore +
                ", ratingCount=" + ratingCount +
                ", locationAt='" + locationAt + '\'' +
                '}';
    }
}