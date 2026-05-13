package group19.restaurant_system.model;

import java.time.LocalDateTime;
public class GroupSession {
    
    private Integer sessionId;
    
    private User creator;
    
    private String inviteCode;
    
    private LocalDateTime createdAt;
    
    private String status; // "揪團中" or "已結束"

    public GroupSession() {
        this.status = "揪團中";
        this.createdAt = LocalDateTime.now();
    }

    public GroupSession(User creator, String inviteCode) {
        this.creator = creator;
        this.inviteCode = inviteCode;
        this.status = "揪團中";
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getSessionId() { return sessionId; }
    public void setSessionId(Integer sessionId) { this.sessionId = sessionId; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "GroupSession{" +
                "sessionId=" + sessionId +
                ", creatorId=" + creator.getUserId() +
                ", inviteCode='" + inviteCode + '\'' +
                ", createdAt=" + createdAt +
                ", status='" + status + '\'' +
                '}';
    }
}