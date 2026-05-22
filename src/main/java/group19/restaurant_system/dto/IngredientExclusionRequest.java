package group19.restaurant_system.dto;

public class IngredientExclusionRequest {
    private String ingredients;

    public IngredientExclusionRequest() {}

    public IngredientExclusionRequest(String ingredients) {
        this.ingredients = ingredients;
    }

    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }
}
