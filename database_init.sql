-- Create database
CREATE DATABASE IF NOT EXISTS restaurant_db;
USE restaurant_db;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    userId INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create dishes table (menu items)
CREATE TABLE IF NOT EXISTS dishes (
    dishId INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) UNIQUE NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create restaurants table
CREATE TABLE IF NOT EXISTS restaurants (
    restaurantId INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    priceRange VARCHAR(20) NOT NULL,
    avgScore DOUBLE DEFAULT 0,
    ratingCount INT DEFAULT 0,
    locationAt VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create ratings table
CREATE TABLE IF NOT EXISTS ratings (
    ratingId INT PRIMARY KEY AUTO_INCREMENT,
    userId INT NOT NULL,
    restaurantId INT NOT NULL,
    score INT NOT NULL CHECK (score >= 1 AND score <= 5),
    comment LONGTEXT,
    ratedAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (userId) REFERENCES users(userId) ON DELETE CASCADE,
    FOREIGN KEY (restaurantId) REFERENCES restaurants(restaurantId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create user_exclusions table
CREATE TABLE IF NOT EXISTS user_exclusions (
    exclusionId INT PRIMARY KEY AUTO_INCREMENT,
    userId INT NOT NULL,
    dishId INT DEFAULT NULL,
    ingredientId INT DEFAULT NULL,
    restaurantId INT DEFAULT NULL,
    UNIQUE KEY unique_exclusion_dish (userId, dishId),
    UNIQUE KEY unique_exclusion_ingredient (userId, ingredientId),
    UNIQUE KEY unique_exclusion_restaurant (userId, restaurantId),
    FOREIGN KEY (userId) REFERENCES users(userId) ON DELETE CASCADE,
    FOREIGN KEY (dishId) REFERENCES dishes(dishId) ON DELETE CASCADE,
    FOREIGN KEY (ingredientId) REFERENCES ingredients(ingredientId) ON DELETE CASCADE,
    FOREIGN KEY (restaurantId) REFERENCES restaurants(restaurantId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create group_sessions table
CREATE TABLE IF NOT EXISTS group_sessions (
    sessionId INT PRIMARY KEY AUTO_INCREMENT,
    creatorId INT NOT NULL,
    inviteCode VARCHAR(20) UNIQUE NOT NULL,
    createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT '揪團中',
    FOREIGN KEY (creatorId) REFERENCES users(userId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create group_members table
CREATE TABLE IF NOT EXISTS group_members (
    memberId INT PRIMARY KEY AUTO_INCREMENT,
    sessionId INT NOT NULL,
    userId INT NOT NULL,
    joinedAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_group_member (sessionId, userId),
    FOREIGN KEY (sessionId) REFERENCES group_sessions(sessionId) ON DELETE CASCADE,
    FOREIGN KEY (userId) REFERENCES users(userId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create records table (history)
CREATE TABLE IF NOT EXISTS records (
    recordId INT PRIMARY KEY AUTO_INCREMENT,
    userId INT NOT NULL,
    restaurantId INT NOT NULL,
    visitDate DATE NOT NULL,
    mealName VARCHAR(100) NOT NULL,
    note LONGTEXT,
    createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (userId) REFERENCES users(userId) ON DELETE CASCADE,
    FOREIGN KEY (restaurantId) REFERENCES restaurants(restaurantId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create ingredients table
CREATE TABLE IF NOT EXISTS ingredients (
    ingredientId INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) UNIQUE NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert sample dishes
INSERT IGNORE INTO dishes (name) VALUES 
('壽司'), ('拉麵'), ('炸雞'), ('披薩'), ('義大利麵'),
('漢堡'), ('牛排'), ('咖哩飯'), ('泰式炒河粉'), ('火鍋');

-- Insert sample restaurants
INSERT IGNORE INTO restaurants (name, category, priceRange, avgScore, ratingCount, locationAt) VALUES
('三號亭日本料理', '日式', '$$$', 4.5, 10, '台北市信義區'),
('老北京人家', '中式', '$$', 4.2, 15, '台北市中正區'),
('韓炭焼肉', '韓式', '$$$', 4.7, 8, '台北市東區'),
('Trattoria Roma', '義大利', '$$$', 4.3, 12, '台北市大安區'),
('五星漢堡', '美式', '$$', 3.9, 20, '台北市西門町'),
('泰灣小館', '泰式', '$$', 4.4, 18, '台北市南港'),
('河粉仔', '越南', '$', 4.1, 25, '台北市萬華'),
('Curry House', '印度', '$$', 4.0, 14, '台北市中山'),
('La Taperia', '西班牙', '$$$', 4.6, 9, '台北市大安區'),
('Petit Bistro', '法式', '$$$$', 4.8, 7, '台北市信義區');

-- Insert sample ingredients
INSERT IGNORE INTO ingredients (name) VALUES
('雞肉'), ('豬肉'), ('牛肉'), ('羊肉'), ('海鮮'), ('魚'), ('蝦'), ('貝類'), ('香菇'), ('茄子');

-- Create restaurant_dishes relation table
CREATE TABLE IF NOT EXISTS restaurant_dishes (
    restaurant_dish_id INT PRIMARY KEY AUTO_INCREMENT,
    restaurantId INT NOT NULL,
    dishId INT NOT NULL,
    UNIQUE KEY unique_restaurant_dish (restaurantId, dishId),
    FOREIGN KEY (restaurantId) REFERENCES restaurants(restaurantId) ON DELETE CASCADE,
    FOREIGN KEY (dishId) REFERENCES dishes(dishId) ON DELETE CASCADE
);

-- Create dish_ingredients relation table
CREATE TABLE IF NOT EXISTS dish_ingredients (
    dish_ingredient_id INT PRIMARY KEY AUTO_INCREMENT,
    dishId INT NOT NULL,
    ingredientId INT NOT NULL,
    UNIQUE KEY unique_dish_ingredient (dishId, ingredientId),
    FOREIGN KEY (dishId) REFERENCES dishes(dishId) ON DELETE CASCADE,
    FOREIGN KEY (ingredientId) REFERENCES ingredients(ingredientId) ON DELETE CASCADE
);

-- Sample dish-ingredient links
INSERT IGNORE INTO dish_ingredients (dishId, ingredientId) VALUES
(1, 6), (1, 7), (1, 8),   -- 壽司：魚、蝦、貝類
(2, 2), (2, 9),           -- 拉麵：豬肉、香菇
(3, 1),                   -- 炸雞：雞肉
(4, 9),                   -- 披薩：香菇
(5, 3), (5, 9),           -- 義大利麵：牛肉、香菇
(6, 3),                   -- 漢堡：牛肉
(7, 3),                   -- 牛排：牛肉
(8, 4),                   -- 咖哩飯：羊肉
(9, 7),                   -- 泰式炒河粉：蝦
(10, 5);                  -- 火鍋：海鮮

-- Sample restaurant-dish links
INSERT IGNORE INTO restaurant_dishes (restaurantId, dishId) VALUES
(1, 1), (1, 2),
(2, 10),
(3, 7), (3, 3),
(4, 4), (4, 5),
(5, 6),
(6, 9),
(7, 2),
(8, 8),
(9, 1),
(10, 7);
-- Create indexes
CREATE INDEX idx_user_name ON users(name);
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_rating_user ON ratings(userId);
CREATE INDEX idx_rating_restaurant ON ratings(restaurantId);
CREATE INDEX idx_exclusion_user ON user_exclusions(userId);
CREATE INDEX idx_session_code ON group_sessions(inviteCode);
CREATE INDEX idx_record_user ON records(userId);
CREATE INDEX idx_record_restaurant ON records(restaurantId);

COMMIT;
