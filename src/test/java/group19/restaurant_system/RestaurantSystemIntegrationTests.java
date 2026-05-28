package group19.restaurant_system;

import group19.restaurant_system.model.*;
import group19.restaurant_system.service.*;
import group19.restaurant_system.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class RestaurantSystemIntegrationTests {

    @Autowired
    private UserService userService;
    
    @Autowired
    private RestaurantService restaurantService;
    
    @Autowired
    private RatingService ratingService;
    
    @Autowired
    private DishService dishService;
    
    @Autowired
    private UserExclusionService userExclusionService;
    
    @Autowired
    private GroupSessionService groupSessionService;
    
    @Autowired
    private RecordService recordService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RestaurantRepository restaurantRepository;
    
    @Autowired
    private DishRepository dishRepository;
    
    @Autowired
    private RatingRepository ratingRepository;
    
    @Autowired
    private RecordRepository recordRepository;
    
    @Autowired
    private UserExclusionRepository userExclusionRepository;
    
    @Autowired
    private GroupSessionRepository groupSessionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User testUser;
    private Restaurant testRestaurant;
    private Dish testDish;

    @BeforeEach
    public void setup() throws Exception {
        // Clear existing data in FK-safe order
        ratingRepository.deleteAllInBatch();
        recordRepository.deleteAllInBatch();
        userExclusionRepository.deleteAllInBatch();
        groupSessionRepository.deleteAllInBatch();
        restaurantRepository.deleteAllInBatch();
        dishRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        
        // Create test user
        testUser = new User("testuser", "test@example.com", "hashed_password");
        testUser = userRepository.save(testUser);
        
        // Create test dish
        testDish = new Dish("日式");
        testDish = dishRepository.save(testDish);
        
        // Create test restaurant
        testRestaurant = new Restaurant("日本料理餐廳", "日式", "$$$", "台北市");
        testRestaurant = restaurantRepository.save(testRestaurant);

        // Seed dish/restaurant relation for recommendation tests
        jdbcTemplate.update(
            "INSERT INTO restaurant_dishes (restaurantId, dishId) VALUES (?, ?)",
            testRestaurant.getRestaurantId(),
            testDish.getDishId()
        );
    }

    @Test
    public void testUserRegistration() throws Exception {
        String testUserName = "newuser";
        String testEmail = "newuser@example.com";
        String testPassword = "password123";
        
        User user = new User(testUserName, testEmail, testPassword);
        user = userRepository.save(user);
        
        assertNotNull(user.getUserId());
        assertEquals(testUserName, user.getName());
        assertEquals(testEmail, user.getEmail());
    }

    @Test
    public void testRatingCreation() throws Exception {
        Rating rating = new Rating(testUser, testRestaurant, 5, "Very good!");
        rating = ratingService.addRating(testUser.getUserId(), testRestaurant.getRestaurantId(), 5, "Very good!");
        
        assertNotNull(rating.getRatingId());
        assertEquals(5, rating.getScore());
        assertEquals("Very good!", rating.getComment());
    }

    @Test
    public void testAverageScoreUpdate() throws Exception {
        // Add first rating
        ratingService.addRating(testUser.getUserId(), testRestaurant.getRestaurantId(), 4, "Good");
        
        // Verify restaurant updated
        Restaurant updated = restaurantRepository.findById(testRestaurant.getRestaurantId()).get();
        assertEquals(4.0, updated.getAvgScore());
        assertEquals(1, updated.getRatingCount());
        
        // Add second rating
        User user2 = new User("user2", "user2@example.com", "password");
        user2 = userRepository.save(user2);
        ratingService.addRating(user2.getUserId(), testRestaurant.getRestaurantId(), 5, "Excellent");
        
        // Verify average updated
        updated = restaurantRepository.findById(testRestaurant.getRestaurantId()).get();
        assertEquals(4.5, updated.getAvgScore());
        assertEquals(2, updated.getRatingCount());
    }

    @Test
    public void testUserExclusion() throws Exception {
        userExclusionService.addDishExclusion(testUser.getUserId(), testDish.getDishId());
        
        var exclusions = userExclusionService.getUserExclusions(testUser.getUserId());
        assertEquals(1, exclusions.size());
        assertEquals(testDish.getDishId(), exclusions.get(0).getDish().getDishId());
    }

    @Test
    public void testGroupCreation() throws Exception {
        GroupSession session = groupSessionService.createGroup(testUser.getUserId());
        
        assertNotNull(session.getSessionId());
        assertNotNull(session.getInviteCode());
        assertEquals("揪團中", session.getStatus());
    }

    @Test
    public void testGroupJoin() throws Exception {
        GroupSession session = groupSessionService.createGroup(testUser.getUserId());
        String inviteCode = session.getInviteCode();
        
        User user2 = new User("user2", "user2@example.com", "password");
        user2 = userRepository.save(user2);
        
        GroupSession joinedSession = groupSessionService.joinGroup(inviteCode, user2.getUserId());
        assertEquals(session.getSessionId(), joinedSession.getSessionId());
    }

    @Test
    public void testRecordHistory() throws Exception {
        java.time.LocalDateTime visitDate = java.time.LocalDateTime.now();
        group19.restaurant_system.model.Record record = recordService.saveRecord(testUser.getUserId(), testRestaurant.getRestaurantId(),
                visitDate, "Dinner", "Good experience", null, null);
        
        assertNotNull(record.getRecordId());
        
        var history = recordService.getUserHistory(testUser.getUserId());
        assertEquals(1, history.size());
        assertEquals("Good experience", history.get(0).getNote());
    }

    @Test
    public void testRecommendationWithExclusion() throws Exception {
        // Create multiple restaurants
        Restaurant restaurant2 = new Restaurant("中式餐廳", "中式", "$$", "台北市");
        restaurant2.setAvgScore(4.8);
        restaurantRepository.save(restaurant2);
        
        // Add exclusion for Japanese
        userExclusionService.addDishExclusion(testUser.getUserId(), testDish.getDishId());
        
        // Sanity check: the restaurant is linked to the excluded dish
        assertEquals(1, jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM restaurant_dishes WHERE restaurantId = ? AND dishId = ?",
            Integer.class,
            testRestaurant.getRestaurantId(),
            testDish.getDishId()
        ));
        
        // Get recommendations
        var recommendations = restaurantService.getRecommendedRestaurants(testUser.getUserId());
        
        // Should not contain Japanese restaurant
        boolean hasJapanese = recommendations.stream()
                .anyMatch(r -> "日式".equals(r.getCategory()));
        assertFalse(hasJapanese);
    }

    @Test
    public void testInvalidRatingScore() {
        assertThrows(Exception.class, () -> {
            ratingService.addRating(testUser.getUserId(), testRestaurant.getRestaurantId(), 6, "Invalid");
        });
        
        assertThrows(Exception.class, () -> {
            ratingService.addRating(testUser.getUserId(), testRestaurant.getRestaurantId(), 0, "Invalid");
        });
    }

    @Test
    public void testNonExistentUser() {
        assertThrows(Exception.class, () -> {
            ratingService.addRating(99999, testRestaurant.getRestaurantId(), 5, "Test");
        });
    }

    @Test
    public void testNonExistentRestaurant() {
        assertThrows(Exception.class, () -> {
            ratingService.addRating(testUser.getUserId(), 99999, 5, "Test");
        });
    }
}
