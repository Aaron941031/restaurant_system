package group19.restaurant_system.model;

public class Dish {
    private Integer categoryId;
    private String name;

    public Dish() {}

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return "Dish{" +
                "categoryId=" + categoryId +
                ", name='" + name + '\'' +
                '}';
    }
}