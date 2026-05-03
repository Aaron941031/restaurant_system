package group19.restaurant_system.dto;

import java.time.LocalDate;

public class RecordRequest {
    private Integer restaurantId;
    private LocalDate visitDate;
    private String mealName;
    private String note;

    public RecordRequest() {}

    public Integer getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Integer restaurantId) { this.restaurantId = restaurantId; }

    public LocalDate getVisitDate() { return visitDate; }
    public void setVisitDate(LocalDate visitDate) { this.visitDate = visitDate; }

    public String getMealName() { return mealName; }
    public void setMealName(String mealName) { this.mealName = mealName; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
