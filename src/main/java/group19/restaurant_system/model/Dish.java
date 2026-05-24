package group19.restaurant_system.model;

import jakarta.persistence.*;

@Entity
@Table(name = "dishes")
public class Dish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer dishId;
    private Integer price;

    @Column(nullable = false, unique = true)
    private String name;

    public Dish() {}

    public Dish(String name) {
        this.name = name;
    }

    // Getters and Setters
    public Integer getDishId() { return dishId; }
    public void setDishId(Integer dishId) { this.dishId = dishId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getPrice() {
    return price;
    }

    public void setPrice(Integer price) {
    this.price = price;
    }

    @Override
    public String toString() {
        return "Dish{" +
            "dishId=" + dishId +
                ", name='" + name + '\'' +
                '}';
    }
}