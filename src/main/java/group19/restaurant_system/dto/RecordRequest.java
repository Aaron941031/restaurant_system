package group19.restaurant_system.dto;

import java.time.LocalDateTime;
import java.util.List;

public class RecordRequest {
    private Integer restaurantId;
    private LocalDateTime visitDate;
    private String mealName;
    private String note;
    private List<Integer> participantIds;

    public RecordRequest() {}

    public Integer getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Integer restaurantId) { this.restaurantId = restaurantId; }

    public LocalDateTime getVisitDate() { return visitDate; }
    public void setVisitDate(LocalDateTime visitDate) { this.visitDate = visitDate; }

    public String getMealName() { return mealName; }
    public void setMealName(String mealName) { this.mealName = mealName; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public List<Integer> getParticipantIds() { return participantIds; }
    public void setParticipantIds(List<Integer> participantIds) { this.participantIds = participantIds; }
}
