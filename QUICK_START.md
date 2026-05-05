# 餐廳推薦與揪團系統 - 快速開始指南

## 5 分鐘快速設置

### 第 1 步：確保系統要求

```bash
# 檢查 Java 版本 (需要 Java 21+)
java -version

# 檢查 MySQL 版本 (需要 MySQL 8.0+)
mysql --version
```

### 第 2 步：設置數據庫

```bash
# 1. 打開 MySQL
mysql -u root -p

# 2. 在 MySQL 命令行執行
mysql> source database_init.sql;

# 驗證數據庫創建成功
mysql> use restaurant_db;
mysql> show tables;
```

### 第 3 步：配置應用

編輯 `src/main/resources/application.properties`:

```properties
# 只需修改這三行
spring.datasource.url=jdbc:mysql://localhost:3306/restaurant_db
spring.datasource.username=root
spring.datasource.password=你的密碼

# JWT 密鑰 (生產環境請更改)
jwt.secret=your-secret-key-change-this-in-production
```

### 第 4 步：啟動應用

```bash
# 使用 Maven 啟動
mvn clean spring-boot:run

# 或編譯後運行
mvn clean package
java -jar target/restaurant_system-0.0.1-SNAPSHOT.jar
```

看到這行信息表示成功啟動:
```
Started RestaurantSystemApplication in X.XXX seconds
```

## 測試 API

### 1. 註冊新用戶

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "john_doe",
    "email": "john@example.com",
    "password": "password123"
  }'
```

**響應示例:**
```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
    "userId": 1,
    "name": "john_doe"
  }
}
```

保存這個 `token`，後面所有請求都需要它。

### 2. 登入

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "name": "john_doe",
    "password": "password123"
  }'
```

### 3. 查看個人資料

```bash
# 替換 YOUR_TOKEN 為上面取得的 token
curl -X GET http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 4. 查看所有餐廳

```bash
curl -X GET http://localhost:8080/api/restaurant/all
```

### 5. 為餐廳評分

```bash
curl -X POST http://localhost:8080/api/restaurant/rate \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": 1,
    "score": 5,
    "comment": "非常好吃！"
  }'
```

### 6. 獲得推薦

```bash
curl -X GET http://localhost:8080/api/recommend/personal \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 7. 建立群組

```bash
curl -X POST http://localhost:8080/api/groups/create \
  -H "Authorization: Bearer YOUR_TOKEN"
```

響應會包含 `inviteCode`，分享給朋友加入。

### 8. 加入群組

```bash
# 替換 INVITE_CODE 為上面取得的邀請碼
curl -X POST http://localhost:8080/api/groups/join?inviteCode=INVITE_CODE \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## 常見操作

### 添加排除類別

某個餐廳類別不想看？

```bash
# 查看所有類別
curl -X GET http://localhost:8080/api/restaurant/all

# 找到你想排除的類別 ID (例如日式是 ID 3)
# 添加到排除名單
curl -X POST http://localhost:8080/api/user/exclusion?categoryId=3 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 保存用餐記錄

```bash
curl -X POST http://localhost:8080/api/history/save \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": 1,
    "visitDate": "2024-01-15",
    "mealName": "套餐 A",
    "note": "和朋友一起吃的，很棒"
  }'
```

### 查看用餐歷史

```bash
curl -X GET http://localhost:8080/api/history/me \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## 使用 Postman 測試

### 導入集合

1. 打開 Postman
2. 點擊 "Import"
3. 創建新的 Collection
4. 添加以下請求:

**POST 註冊**
```
URL: {{base_url}}/auth/register
Body (JSON):
{
  "name": "user123",
  "email": "user@test.com",
  "password": "password123"
}
```

**GET 個人資料**
```
URL: {{base_url}}/user/profile
Headers:
Authorization: Bearer {{token}}
```

**POST 評分**
```
URL: {{base_url}}/restaurant/rate
Headers:
Authorization: Bearer {{token}}
Body (JSON):
{
  "restaurantId": 1,
  "score": 5,
  "comment": "Good!"
}
```

### 環境變量設置

在 Postman 中設置:
- `base_url`: http://localhost:8080/api
- `token`: (登入後複製)
- `restaurantId`: 1

## 故障排除

### 問題 1: 無法連接到數據庫

```
ERROR org.springframework.boot.actuate.health.db.DatabaseHealthIndicator - Database health check failed
```

**解決方案:**
1. 確保 MySQL 正在運行: `mysql.server start` (Mac) 或 `net start MySQL80` (Windows)
2. 檢查用戶名和密碼
3. 驗證數據庫存在: `SHOW DATABASES;`

### 問題 2: 端口 8080 被佔用

```
Address already in use
```

**解決方案:**
- 方案 A: 停止其他使用端口的應用
- 方案 B: 修改應用端口:
```properties
server.port=8081
```

### 問題 3: JWT Token 無效

```json
{
  "success": false,
  "message": "Invalid token"
}
```

**解決方案:**
1. 確認 Token 格式正確
2. 檢查 Token 是否過期 (24 小時)
3. 重新登入獲取新 Token

### 問題 4: 找不到類或資源

```
ClassNotFoundException
```

**解決方案:**
```bash
# 清理並重新構建
mvn clean compile
mvn spring-boot:run
```

## 下一步

1. **閱讀完整文檔**
   - [API_DOCUMENTATION.md](API_DOCUMENTATION.md) - 完整 API 文檔
   - [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) - 部署指南

2. **自定義功能**
   - 修改推薦算法: `service/RestaurantService.java`
   - 添加新 API: 在 `controller/` 中添加新類

3. **部署到生產環境**
   - 修改 JWT Secret
   - 配置數據庫密碼
   - 設置 HTTPS
   - 參考 DEPLOYMENT_GUIDE.md

## 示例工作流

### 用戶 A 的工作流

```
1. 註冊帳號
   curl -X POST http://localhost:8080/api/auth/register ...

2. 登入
   curl -X POST http://localhost:8080/api/auth/login ...

3. 查看餐廳
   curl -X GET http://localhost:8080/api/restaurant/all

4. 為喜歡的餐廳評分
   curl -X POST http://localhost:8080/api/restaurant/rate ...

5. 創建群組邀請朋友
   curl -X POST http://localhost:8080/api/groups/create ...

6. 分享邀請碼: ABC12345

7. 查看群組推薦
   curl -X GET http://localhost:8080/api/recommend/group/1 ...

8. 保存用餐記錄
   curl -X POST http://localhost:8080/api/history/save ...
```

### 用戶 B 的工作流

```
1. 註冊帳號
2. 登入
3. 加入用戶 A 的群組
   curl -X POST http://localhost:8080/api/groups/join?inviteCode=ABC12345 ...
4. 查看群組推薦
5. 一起去吃飯！
```

## 有用的命令

```bash
# 查看日誌
tail -f nohup.log

# 查看進程
ps aux | grep java

# 停止應用
kill -9 [PID]

# 數據庫備份
mysqldump -u root -p restaurant_db > backup.sql

# 數據庫恢復
mysql -u root -p restaurant_db < backup.sql

# 查看數據庫大小
SELECT table_schema, ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS size_mb
FROM information_schema.tables GROUP BY table_schema;
```

## 性能測試

使用 Apache Bench 進行簡單的性能測試:

```bash
# 100 個請求，並發 10 個
ab -n 100 -c 10 http://localhost:8080/api/restaurant/all

# 測試登入端點
ab -n 100 -c 10 -p data.json http://localhost:8080/api/auth/login
```

## 建議和反饋

- 🐛 發現 Bug? 檢查應用日誌
- 💡 有改進建議? 修改相應的 Service 類
- 📚 需要幫助? 查看完整文檔

---

**祝你使用愉快！** 🍽️🎉
