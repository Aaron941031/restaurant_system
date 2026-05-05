package group19.restaurant_system.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_exclusions")
public class UserExclusion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer exclusionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoryId", nullable = false)
    private Dish dish;

    public UserExclusion() {}

    public UserExclusion(User user, Dish dish) {
        this.user = user;
        this.dish = dish;
    }

    // Getters and Setters
    public Integer getExclusionId() { return exclusionId; }
    public void setExclusionId(Integer exclusionId) { this.exclusionId = exclusionId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Dish getDish() { return dish; }
    public void setDish(Dish dish) { this.dish = dish; }

    @Override
    public String toString() {
        return "UserExclusion{" +
                "exclusionId=" + exclusionId +
                ", userId=" + user.getUserId() +
                ", categoryId=" + dish.getCategoryId() +
                '}';
    }
}