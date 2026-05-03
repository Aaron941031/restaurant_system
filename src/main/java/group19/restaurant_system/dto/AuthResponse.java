package group19.restaurant_system.dto;

public class AuthResponse {
    private String token;
    private Integer userId;
    private String name;
    private String message;

    public AuthResponse() {}

    public AuthResponse(String token, Integer userId, String name, String message) {
        this.token = token;
        this.userId = userId;
        this.name = name;
        this.message = message;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
