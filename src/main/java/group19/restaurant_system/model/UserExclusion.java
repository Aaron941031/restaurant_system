package group19.restaurant_system.model;

public class UserExclusion {
    private Integer exclusionId;
    private Integer userId;
    private Integer categoryId;

    public UserExclusion() {}

    public Integer getExclusionId() { return exclusionId; }
    public void setExclusionId(Integer exclusionId) { this.exclusionId = exclusionId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    @Override
    public String toString() {
        return "UserExclusion{" +
                "exclusionId=" + exclusionId +
                ", userId=" + userId +
                ", categoryId=" + categoryId +
                '}';
    }
}