package group19.restaurant_system.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
public class Record {
    
    private Integer recordId;
    
    private User user;
    
    private Restaurant restaurant;
    
    private LocalDate visitDate;
    
    private String mealName;
    
    private String note;
    
    private LocalDateTime createdAt;

    public Record() {
        this.createdAt = LocalDateTime.now();
    }

    public Record(User user, Restaurant restaurant, LocalDate visitDate, String mealName, String note) {
        this.user = user;
        this.restaurant = restaurant;
        this.visitDate = visitDate;
        this.mealName = mealName;
        this.note = note;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getRecordId() { return recordId; }
    public void setRecordId(Integer recordId) { this.recordId = recordId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Restaurant getRestaurant() { return restaurant; }
    public void setRestaurant(Restaurant restaurant) { this.restaurant = restaurant; }

    public LocalDate getVisitDate() { return visitDate; }
    public void setVisitDate(LocalDate visitDate) { this.visitDate = visitDate; }

    public String getMealName() { return mealName; }
    public void setMealName(String mealName) { this.mealName = mealName; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Record{" +
                "recordId=" + recordId +
                ", userId=" + user.getUserId() +
                ", restaurantId=" + restaurant.getRestaurantId() +
                ", visitDate=" + visitDate +
                ", mealName='" + mealName + '\'' +
                ", note='" + note + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}