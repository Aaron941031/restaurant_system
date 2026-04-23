## 🍽️ 餐廳篩選與推薦系統 (Restaurant Recommendation System)
本系統採用三層式架構 (Three-Tier Architecture) 開發網頁版應用程式，旨在解決個人日常用餐選擇困難，以及群體聚餐時因飲食禁忌 (如不吃辣、素食) 產生的衝突 

### 🛠️ 開發技術棧 (Tech Stack)

**後端框架**：Java (Spring Boot 4.0.5 / Spring MVC) 
**資料庫**：MySQL (透過 Spring Data JDBC 連接) 
**開發工具**：VS Code, Maven
**輔助套件**：Lombok, Spring Boot DevTools

### ✨ Core Features

1.  **推薦餐廳模組** 
    **條件篩選**：依據料理類型、價位、評分門檻動態查詢
    **個人化推薦**：分析歷史飲食記錄與評分，推薦偏好的料理類型 
2.  **用戶管理**
    基礎登入驗證與個人化偏好設定 (如：忌口與不吃清單設定) 
3. **結果記錄模組 + 小 note** 
    新增、查詢、編輯個人歷史用餐紀錄與備註
4.  **評分系統模組**
    * 撰寫 1-5 星評分與評論，並具備防重複投票機制 (運用 `ON DUPLICATE KEY UPDATE`)
    * 寫入評分後同步觸發更新餐廳平均分
5.  **群體揪團與交集運算模組** 
    *專案亮點**：建立聚餐房間，自動讀取並匯總房間內所有成員的「忌口與不吃清單」
    透過資料庫子查詢與 `NOT IN` 語法，排除所有人不想吃的類別，推薦符合大家胃口且高評價的餐廳 
6.   **分帳與拆款模組 (Split-Bill System)** 
    記錄代墊付款，自動計算並顯示房間內每個人應分攤的金額與債務狀態

### 📂 系統架構與資料庫設計 (Architecture & DB)

後端採用標準的分層設計：
**Controller 層**：接收 HTTP 請求，呼叫 Service，回傳資料 
**Service 層**：處理商業邏輯、參數驗證、交易機制 (Transaction)
**DAO 層**：封裝 SQL 邏輯，與 MySQL 進行資料存取 
**Model 層**：對應資料庫欄位的 Java POJO 類別

詳細的實體關聯模型 (ERD) 與資料表設計，請參閱本專案的系統設計說明文件

### 🚀 快速啟動 (Getting Started)

#### 1. 環境準備
請確保你的電腦已安裝：
* Java Development Kit (JDK) 21 或以上版本
* MySQL Server (建議 8.0 以上版本)

#### 2. 資料庫初始化
1. 開啟 MySQL，建立專案專用的資料庫 (例如：`restaurant_db`)。
2. 匯入專案中提供的初始化 SQL 腳本 (`schema.sql` 或 `init.sql`) 來建立資料表。

#### 3. 設定環境變數
進入 `src/main/resources/application.properties`，修改為你的 MySQL 帳號密碼：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/restaurant_db
spring.datasource.username=你的MySQL帳號
spring.datasource.password=你的MySQL密碼
```

#### 4. 啟動應用程式
在 VS Code 中開啟專案，執行 `RestaurantApplication.java` 即可啟動本地伺服器。預設將運行於 `http://localhost:8080`。
---
分工表
---
資料庫：113306025 陳心祈、1133060 朱冠瑋
後端：11330629 李洋昇、113306032 李嘉皓
前端：113306087 崔庭勳
