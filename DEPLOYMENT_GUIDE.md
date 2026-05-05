# 建構和部署指南

## 構建項目

### 使用 Maven 構建

```bash
# 清理並構建
mvn clean build

# 跳過測試進行構建
mvn clean build -DskipTests

# 生成可執行 JAR
mvn clean package
```

構建完成後，JAR 文件位於 `target/restaurant_system-0.0.1-SNAPSHOT.jar`

### 運行應用

#### 本地開發環境

```bash
# 使用 Maven 直接運行
mvn spring-boot:run

# 或運行 JAR 文件
java -jar target/restaurant_system-0.0.1-SNAPSHOT.jar
```

#### 生產環境運行

```bash
# 後臺運行 (Linux/Mac)
nohup java -jar restaurant_system-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

# Windows 服務註冊
sc create RestaurantSystem binPath= "C:\path\to\java -jar restaurant_system-0.0.1-SNAPSHOT.jar"
```

## 配置說明

### application.properties 常見配置

```properties
# 服務器配置
server.port=8080
server.servlet.context-path=/api

# 數據庫連接
spring.datasource.url=jdbc:mysql://localhost:3306/restaurant_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate 配置
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# JWT 配置
jwt.secret=your-secret-key-at-least-32-characters-long
jwt.expiration=86400000

# 日誌級別
logging.level.root=INFO
logging.level.group19.restaurant_system=DEBUG

# 連接池配置
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
```

## 數據庫部署

### MySQL 版本要求
- MySQL 8.0+
- 字符集: utf8mb4

### 導入初始化腳本

```bash
# 使用命令行
mysql -u root -p restaurant_db < database_init.sql

# 或在 MySQL Workbench 中執行
# 1. 打開 database_init.sql
# 2. 點擊"執行"
```

### 備份數據庫

```bash
# 完整備份
mysqldump -u root -p restaurant_db > backup_$(date +%Y%m%d_%H%M%S).sql

# 恢復備份
mysql -u root -p restaurant_db < backup_20240115_120000.sql
```

## Docker 部署 (可選)

### 創建 Dockerfile

```dockerfile
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY target/restaurant_system-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 構建和運行 Docker 鏡像

```bash
# 構建鏡像
docker build -t restaurant-system:latest .

# 運行容器
docker run -d \
  --name restaurant-system \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql-host:3306/restaurant_db \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=password \
  -e JWT_SECRET=your-secret-key \
  restaurant-system:latest
```

### Docker Compose 部署

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: restaurant-mysql
    environment:
      MYSQL_ROOT_PASSWORD: password
      MYSQL_DATABASE: restaurant_db
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./database_init.sql:/docker-entrypoint-initdb.d/init.sql

  app:
    build: .
    container_name: restaurant-app
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/restaurant_db
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: password
      JWT_SECRET: your-secret-key-change-this
    depends_on:
      - mysql

volumes:
  mysql_data:
```

運行:
```bash
docker-compose up -d
```

## 環境變量配置

使用環境變量優先級高於 application.properties:

```bash
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/restaurant_db
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=password
export JWT_SECRET=your-secret-key
java -jar restaurant_system-0.0.1-SNAPSHOT.jar
```

## 依賴項目清單

檢查所有需要的依賴:

```bash
mvn dependency:tree
```

## 常見部署問題

### 1. 連接拒絕錯誤

**問題**: `java.sql.SQLException: Connection refused`

**解決方案**:
- 確保 MySQL 服務正在運行
- 檢查數據庫 URL 和端口
- 驗證用戶名和密碼

### 2. 端口已被佔用

**問題**: `Address already in use`

**解決方案**:
```bash
# 查找佔用端口 8080 的進程 (Linux/Mac)
lsof -i :8080

# 查找佔用端口 8080 的進程 (Windows)
netstat -ano | findstr :8080

# 改變應用端口
java -Dserver.port=8081 -jar restaurant_system-0.0.1-SNAPSHOT.jar
```

### 3. 內存不足

**解決方案**:
```bash
# 增加 Java 堆內存
java -Xmx512m -Xms256m -jar restaurant_system-0.0.1-SNAPSHOT.jar
```

### 4. 連接超時

**解決方案**:
```properties
# 增加連接超時時間
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

## 性能優化建議

### 1. 數據庫優化

```sql
-- 添加必要的索引
CREATE INDEX idx_user_exclusions ON user_exclusions(userId);
CREATE INDEX idx_ratings_restaurant ON ratings(restaurantId);
CREATE INDEX idx_records_user ON records(userId);

-- 啟用查詢緩存
SET GLOBAL query_cache_type = ON;
SET GLOBAL query_cache_size = 268435456;
```

### 2. 連接池優化

```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.auto-commit=true
```

### 3. 緩存配置

```properties
# 啟用 Spring Cache
spring.cache.type=simple
```

### 4. 日誌優化

```properties
# 異步日誌記錄
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
logging.level.org.springframework=WARN
```

## 監控和日誌

### 應用日誌位置

默認日誌輸出到控制台。配置文件日誌:

**logback-spring.xml**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/app.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/app.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>

    <root level="INFO">
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

### 健康檢查端點

添加以下依賴到 pom.xml:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

訪問:
```
http://localhost:8080/api/actuator/health
```

## 版本升級指南

### 升級 Spring Boot 版本

1. 修改 pom.xml 中的版本
2. 運行 `mvn clean test`
3. 修復任何兼容性問題
4. 部署新版本

### 升級 Java 版本

1. 修改 pom.xml 中的 java.version
2. 安裝新版 JDK
3. 重新構建項目
4. 測試和部署

## 備份和恢復計劃

### 每日備份腳本

**backup.sh**:
```bash
#!/bin/bash
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups"

# 備份數據庫
mysqldump -u root -ppassword restaurant_db > $BACKUP_DIR/restaurant_db_$TIMESTAMP.sql

# 備份應用日誌
tar -czf $BACKUP_DIR/app_logs_$TIMESTAMP.tar.gz logs/

# 清理 30 天前的備份
find $BACKUP_DIR -mtime +30 -delete
```

### 恢復流程

```bash
# 1. 停止應用
systemctl stop restaurant_system

# 2. 恢復數據庫
mysql -u root -p restaurant_db < backup_20240115_120000.sql

# 3. 恢復應用日誌 (可選)
tar -xzf app_logs_20240115_120000.tar.gz

# 4. 重啟應用
systemctl start restaurant_system
```

## 發佈檢查清單

在部署到生產環境前:

- [ ] 修改 JWT Secret
- [ ] 修改數據庫密碼
- [ ] 禁用 debug 日誌
- [ ] 配置 HTTPS
- [ ] 設置 CORS 白名單
- [ ] 配置防火牆規則
- [ ] 設置監控告警
- [ ] 創建備份計劃
- [ ] 測試恢復流程
- [ ] 準備回滾計劃

## 技術支持

如有問題,請檢查:
1. 應用日誌
2. 數據庫連接
3. 防火牆設置
4. 資源使用情況 (CPU, 內存, 磁盤)
