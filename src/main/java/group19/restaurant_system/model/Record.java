package group19.restaurant_system.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Record {

    private Integer recordId;

    private User user;

    private Restaurant restaurant;

    private LocalDateTime visitDate;

    private String mealName;

    private String note;

    private Integer groupSessionId;

    private LocalDateTime createdAt;

    private Boolean isEdited = false;

    // 不存在 DB，查詢時由 record_participants 填入
    private List<ParticipantInfo> participants = new ArrayList<>();

    public static class ParticipantInfo {
        private Integer userId;
        private String name;

        public ParticipantInfo() {}
        public ParticipantInfo(Integer userId, String name) {
            this.userId = userId;
            this.name = name;
        }

        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public Record() {
        this.createdAt = LocalDateTime.now();
    }

    public Record(User user, Restaurant restaurant, LocalDateTime visitDate, String mealName, String note) {
        this.user = user;
        this.restaurant = restaurant;
        this.visitDate = visitDate;
        this.mealName = mealName;
        this.note = note;
        this.createdAt = LocalDateTime.now();
    }

    public Integer getRecordId() { return recordId; }
    public void setRecordId(Integer recordId) { this.recordId = recordId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Restaurant getRestaurant() { return restaurant; }
    public void setRestaurant(Restaurant restaurant) { this.restaurant = restaurant; }

    public LocalDateTime getVisitDate() { return visitDate; }
    public void setVisitDate(LocalDateTime visitDate) { this.visitDate = visitDate; }

    public String getMealName() { return mealName; }
    public void setMealName(String mealName) { this.mealName = mealName; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Integer getGroupSessionId() { return groupSessionId; }
    public void setGroupSessionId(Integer groupSessionId) { this.groupSessionId = groupSessionId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Boolean getIsEdited() { return isEdited; }
    public void setIsEdited(Boolean isEdited) { this.isEdited = isEdited; }

    public List<ParticipantInfo> getParticipants() { return participants; }
    public void setParticipants(List<ParticipantInfo> participants) { this.participants = participants; }

    @Override
    public String toString() {
        return "Record{recordId=" + recordId +
                ", visitDate=" + visitDate +
                ", mealName='" + mealName + '\'' +
                ", createdAt=" + createdAt + '}';
    }
}
