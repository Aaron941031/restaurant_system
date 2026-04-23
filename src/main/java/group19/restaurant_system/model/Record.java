package group19.restaurant_system.model;

import java.sql.Timestamp;
import java.sql.Date;

public class Record {
    private Integer recordId;
    private Integer userId;
    private Integer restaurantId;
    private Date visitDate;
    private String mealName;
    private String note;
    private Timestamp createdAt;

    public Record() {}

    public Integer getRecordId() { return recordId; }
    public void setRecordId(Integer recordId) { this.recordId = recordId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Integer restaurantId) { this.restaurantId = restaurantId; }

    public Date getVisitDate() { return visitDate; }
    public void setVisitDate(Date visitDate) { this.visitDate = visitDate; }

    public String getMealName() { return mealName; }
    public void setMealName(String mealName) { this.mealName = mealName; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Record{" +
                "recordId=" + recordId +
                ", userId=" + userId +
                ", restaurantId=" + restaurantId +
                ", visitDate=" + visitDate +
                ", mealName='" + mealName + '\'' +
                ", note='" + note + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}