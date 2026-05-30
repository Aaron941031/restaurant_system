package group19.restaurant_system.service;

import group19.restaurant_system.dto.LoginRequest;
import group19.restaurant_system.dto.RegisterRequest;
import group19.restaurant_system.dto.AuthResponse;
import group19.restaurant_system.model.User;
import group19.restaurant_system.repository.UserRepository;
import group19.restaurant_system.util.JwtTokenProvider;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private static final java.util.regex.Pattern EMAIL_PATTERN =
        java.util.regex.Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.(com|tw|com\\.tw)$");

    @Transactional
    public AuthResponse register(RegisterRequest request) throws Exception {
        if (!EMAIL_PATTERN.matcher(request.getEmail()).matches())
            throw new Exception("Email 格式不正確，須為 xxx@xxx.com");

        // Check if user already exists
        if (userRepository.existsByName(request.getName())) {
            throw new Exception("User with name '" + request.getName() + "' already exists");
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new Exception("User with email '" + request.getEmail() + "' already exists");
        }
        
        // Create new user
        User user = new User(request.getName(), request.getEmail(), passwordEncoder.encode(request.getPassword()));
        user = userRepository.save(user);
        
        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user.getUserId(), user.getName());
        
        return new AuthResponse(token, user.getUserId(), user.getName(), "Registration successful");
    }

    public AuthResponse login(LoginRequest request) throws Exception {
        // Find user by name
        Optional<User> userOpt = userRepository.findByName(request.getName());
        if (!userOpt.isPresent()) {
            throw new Exception("User not found");
        }
        
        User user = userOpt.get();
        
        // Check password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new Exception("Invalid password");
        }
        
        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user.getUserId(), user.getName());
        
        return new AuthResponse(token, user.getUserId(), user.getName(), "Login successful");
    }

    public Optional<User> getUserById(Integer userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> getUserByName(String name) {
        return userRepository.findByName(name);
    }

    public User getUserProfile(Integer userId) throws Exception {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new Exception("User not found");
        }
        return userOpt.get();
    }

    @Transactional
    public void deleteUser(Integer userId) throws Exception {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) throw new Exception("User not found");
        userRepository.deleteById(userId);
    }

    @Transactional
    public void updateUser(Integer userId, String email) throws Exception {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new Exception("User not found");
        }
        
        User user = userOpt.get();
        
        if (email != null && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new Exception("Email already exists");
            }
            user.setEmail(email);
        }
        
        userRepository.save(user);
    }
}
