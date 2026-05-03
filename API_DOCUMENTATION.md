# 餐廳推薦與揪團系統 - 後端服務

本項目是一個完整的 Spring Boot 後端應用，實現餐廳推薦與揪團系統的所有核心功能。

## 功能特性

### 用戶管理
- ✅ 用戶註冊與登入 (使用 bcrypt 密碼加密)
- ✅ JWT Token 認證
- ✅ 個人資料管理
- ✅ 排除料理類別功能

### 餐廳評分
- ✅ 新增餐廳評分 (1-5 分)
- ✅ 自動計算平均分數
- ✅ 查看餐廳評論
- ✅ 實時更新評分統計

### 推薦機制
- ✅ 個人推薦 (根據排除類別篩選)
- ✅ 群組推薦 (合併所有成員排除條件)
- ✅ Fallback 機制 (查無結果時推薦高評分餐廳)
- ✅ 預設回傳前 5 筆結果

### 揪團系統
- ✅ 建立群組 (生成唯一邀請碼)
- ✅ 加入群組 (透過邀請碼)
- ✅ 群組推薦
- ✅ 群組狀態管理 (揪團中/已結束)

### 歷史記錄
- ✅ 保存用餐決策
- ✅ 查詢個人歷史
- ✅ 群組決策追蹤

## 技術棧

- **框架**: Spring Boot 4.0.5
- **Java 版本**: 21
- **資料庫**: MySQL 8.0+
- **ORM**: Spring Data JPA + Hibernate
- **認證**: JWT (JSON Web Token)
- **密碼加密**: bcrypt
- **構建工具**: Maven

## 快速開始

### 1. 環境要求
- JDK 21+
- MySQL 8.0+
- Maven 3.6+

### 2. 資料庫設置

```bash
# 使用 MySQL 命令行
mysql -u root -p < database_init.sql
```

或在 MySQL Workbench 中執行 `database_init.sql` 文件。

### 3. 配置應用

編輯 `src/main/resources/application.properties`:

```properties
# 資料庫連接
spring.datasource.url=jdbc:mysql://localhost:3306/restaurant_db
spring.datasource.username=root
spring.datasource.password=your_password

# JWT 配置
jwt.secret=your-secret-key-change-this-in-production
jwt.expiration=86400000
```

### 4. 啟動應用

```bash
# 使用 Maven
mvn clean spring-boot:run

# 或編譯後運行
mvn clean package
java -jar target/restaurant_system-0.0.1-SNAPSHOT.jar
```

應用將在 `http://localhost:8080/api` 啟動

## API 文檔

### 認證 API

#### 1. 用戶註冊
```http
POST /api/auth/register
Content-Type: application/json

{
    "name": "user123",
    "email": "user@example.com",
    "password": "password123"
}

Response:
{
    "success": true,
    "message": "Registration successful",
    "data": {
        "token": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
        "userId": 1,
        "name": "user123"
    }
}
```

#### 2. 用戶登入
```http
POST /api/auth/login
Content-Type: application/json

{
    "name": "user123",
    "password": "password123"
}

Response:
{
    "success": true,
    "message": "Login successful",
    "data": {
        "token": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
        "userId": 1,
        "name": "user123"
    }
}
```

### 用戶管理 API

#### 3. 取得個人資料
```http
GET /api/user/profile
Authorization: Bearer {token}

Response:
{
    "success": true,
    "message": "Profile retrieved",
    "data": {
        "userId": 1,
        "name": "user123",
        "email": "user@example.com",
        "createdAt": "2024-01-15T10:30:00"
    }
}
```

#### 4. 新增排除類別
```http
POST /api/user/exclusion?categoryId=1
Authorization: Bearer {token}

Response:
{
    "success": true,
    "message": "Exclusion added",
    "data": {
        "exclusionId": 1,
        "categoryId": 1
    }
}
```

#### 5. 取得排除名單
```http
GET /api/user/exclusions
Authorization: Bearer {token}

Response:
{
    "success": true,
    "message": "Exclusions retrieved",
    "data": [
        {
            "exclusionId": 1,
            "categoryId": 1,
            "dish": {"categoryId": 1, "name": "日式"}
        }
    ]
}
```

#### 6. 移除排除類別
```http
DELETE /api/user/exclusion/1
Authorization: Bearer {token}

Response:
{
    "success": true,
    "message": "Exclusion removed"
}
```

### 餐廳 API

#### 7. 取得所有餐廳
```http
GET /api/restaurant/all

Response:
{
    "success": true,
    "message": "Restaurants retrieved",
    "data": [
        {
            "restaurantId": 1,
            "name": "三號亭日本料理",
            "category": "日式",
            "priceRange": "$$$",
            "avgScore": 4.5,
            "ratingCount": 10,
            "locationAt": "台北市信義區"
        }
    ]
}
```

#### 8. 取得單一餐廳
```http
GET /api/restaurant/{restaurantId}

Response:
{
    "success": true,
    "message": "Restaurant retrieved",
    "data": {
        "restaurantId": 1,
        "name": "三號亭日本料理",
        ...
    }
}
```

#### 9. 新增餐廳
```http
POST /api/restaurant/save
Content-Type: application/json

{
    "name": "新餐廳",
    "category": "中式",
    "priceRange": "$$",
    "locationAt": "台北市中山區"
}

Response:
{
    "success": true,
    "message": "Restaurant saved",
    "data": {
        "restaurantId": 11,
        "name": "新餐廳",
        ...
    }
}
```

### 評分 API

#### 10. 新增評分
```http
POST /api/restaurant/rate
Authorization: Bearer {token}
Content-Type: application/json

{
    "restaurantId": 1,
    "score": 5,
    "comment": "非常好吃！"
}

Response:
{
    "success": true,
    "message": "Rating added",
    "data": {
        "ratingId": 1,
        "restaurantId": 1,
        "score": 5,
        "comment": "非常好吃！",
        "ratedAt": "2024-01-15T10:35:00"
    }
}
```

#### 11. 取得餐廳評論
```http
GET /api/restaurant/{restaurantId}/ratings

Response:
{
    "success": true,
    "message": "Ratings retrieved",
    "data": [
        {
            "ratingId": 1,
            "score": 5,
            "comment": "非常好吃！",
            "ratedAt": "2024-01-15T10:35:00"
        }
    ]
}
```

### 推薦 API

#### 12. 取得個人推薦
```http
GET /api/recommend/personal
Authorization: Bearer {token}

Response:
{
    "success": true,
    "message": "Recommendations retrieved",
    "data": [
        {
            "restaurantId": 1,
            "name": "三號亭日本料理",
            "avgScore": 4.5,
            ...
        }
    ]
}
```

#### 13. 取得群組推薦
```http
GET /api/recommend/group/{sessionId}
Authorization: Bearer {token}

Response:
{
    "success": true,
    "message": "Group recommendations retrieved",
    "data": [...]
}
```

### 揪團 API

#### 14. 建立群組
```http
POST /api/groups/create
Authorization: Bearer {token}

Response:
{
    "success": true,
    "message": "Group created",
    "data": {
        "sessionId": 1,
        "inviteCode": "ABC12345",
        "status": "揪團中",
        "createdAt": "2024-01-15T10:40:00"
    }
}
```

#### 15. 加入群組
```http
POST /api/groups/join?inviteCode=ABC12345
Authorization: Bearer {token}

Response:
{
    "success": true,
    "message": "Joined group",
    "data": {
        "sessionId": 1,
        "inviteCode": "ABC12345",
        ...
    }
}
```

#### 16. 結束群組
```http
POST /api/groups/{sessionId}/end
Authorization: Bearer {token}

Response:
{
    "success": true,
    "message": "Group ended"
}
```

### 歷史記錄 API

#### 17. 查詢個人歷史
```http
GET /api/history/me
Authorization: Bearer {token}

Response:
{
    "success": true,
    "message": "History retrieved",
    "data": [
        {
            "recordId": 1,
            "restaurantId": 1,
            "mealName": "套餐 A",
            "visitDate": "2024-01-14",
            "createdAt": "2024-01-15T10:45:00"
        }
    ]
}
```

#### 18. 保存用餐決策
```http
POST /api/history/save
Authorization: Bearer {token}
Content-Type: application/json

{
    "restaurantId": 1,
    "visitDate": "2024-01-14",
    "mealName": "套餐 A",
    "note": "與朋友一起吃的"
}

Response:
{
    "success": true,
    "message": "Record saved",
    "data": {
        "recordId": 1,
        "restaurantId": 1,
        "mealName": "套餐 A",
        "visitDate": "2024-01-14",
        "createdAt": "2024-01-15T10:45:00"
    }
}
```

#### 19. 刪除歷史記錄
```http
DELETE /api/history/{recordId}
Authorization: Bearer {token}

Response:
{
    "success": true,
    "message": "Record deleted"
}
```

## 項目結構

```
restaurant_system/
├── src/
│   └── main/
│       ├── java/group19/restaurant_system/
│       │   ├── RestaurantSystemApplication.java
│       │   ├── model/                 # 實體類
│       │   │   ├── User.java
│       │   │   ├── Restaurant.java
│       │   │   ├── Rating.java
│       │   │   ├── GroupSession.java
│       │   │   ├── Record.java
│       │   │   ├── UserExclusion.java
│       │   │   ├── Dish.java
│       │   │   └── Ingredient.java
│       │   ├── repository/            # 數據訪問層
│       │   ├── service/               # 業務邏輯層
│       │   ├── controller/            # API 控制層
│       │   ├── dto/                   # 數據傳輸對象
│       │   ├── util/                  # 工具類
│       │   └── config/                # 配置類
│       └── resources/
│           └── application.properties
├── pom.xml                            # Maven 配置
└── database_init.sql                 # 數據庫初始化腳本
```

## 關鍵設計特性

### 1. 密碼安全
- 所有密碼使用 bcrypt 加密存儲
- 登入時進行安全的密碼驗證

### 2. JWT 認證
- Token 有效期為 24 小時
- 所有保護的 API 都需要有效的 JWT Token
- Token 包含 userId 和 username

### 3. 推薦算法
- 根據用戶排除的料理類別篩選
- 按平均評分排序結果
- 若查無結果自動 fallback 到高評分餐廳
- 群組推薦使用所有成員排除條件的聯集

### 4. 交易處理
- 評分新增與平均分更新使用交易
- 確保資料一致性
- 支援 CASCADE DELETE

### 5. 錯誤處理
- 統一的 ApiResponse 格式
- 詳細的錯誤信息
- 適當的 HTTP 狀態碼

## 開發指南

### 新增新的 API 端點

1. **定義 DTO** (如需要)
   - 在 `dto/` 目錄中創建新的類

2. **實現 Service**
   - 在 `service/` 目錄中創建業務邏輯

3. **添加 Controller**
   - 在 `controller/` 目錄中創建新的端點

4. **添加 Repository** (如需要)
   - 在 `repository/` 目錄中創建 JPA Repository

### 測試 API

使用 Postman 或 curl:

```bash
# 註冊用戶
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"test","email":"test@example.com","password":"test123"}'

# 登入
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"name":"test","password":"test123"}'

# 使用 Token 訪問受保護的資源
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/user/profile
```

## 常見問題

### 如何修改 JWT 過期時間？
編輯 `application.properties`:
```properties
jwt.expiration=86400000  # 毫秒，預設 24 小時
```

### 如何更改密碼加密強度？
編輯 `config/SecurityConfig.java`:
```java
return new BCryptPasswordEncoder(12);  // 增加強度
```

### 如何添加新的推薦規則？
修改 `service/RestaurantService.java` 中的 `getRecommendedRestaurants()` 方法。

## 生產環境部署建議

1. **修改 JWT Secret**
   - 在生產環境中使用強的隨機密鑰

2. **啟用 HTTPS**
   - 配置 SSL/TLS 證書

3. **數據庫安全**
   - 使用環境變數存儲連接信息
   - 設置強的資料庫密碼

4. **日誌配置**
   - 配置適當的日誌級別
   - 將日誌保存到文件

5. **監控和告警**
   - 添加應用監控
   - 配置錯誤告警

## 貢獻指南

歡迎提交 Pull Requests 或報告 Issues。

## 許可證

MIT License
