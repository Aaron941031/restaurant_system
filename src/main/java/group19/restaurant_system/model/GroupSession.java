package group19.restaurant_system.model;

import java.sql.Timestamp;

public class GroupSession {
    private Integer sessionId;
    private Integer creatorId;
    private String inviteCode;
    private Timestamp createdAt;
    private String status;

    public GroupSession() {}

    public Integer getSessionId() { return sessionId; }
    public void setSessionId(Integer sessionId) { this.sessionId = sessionId; }

    public Integer getCreatorId() { return creatorId; }
    public void setCreatorId(Integer creatorId) { this.creatorId = creatorId; }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "GroupSession{" +
                "sessionId=" + sessionId +
                ", creatorId=" + creatorId +
                ", inviteCode='" + inviteCode + '\'' +
                ", createdAt=" + createdAt +
                ", status='" + status + '\'' +
                '}';
    }
}