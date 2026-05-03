# 餐廳推薦與揪團系統後端開發實作規範

## 1. 文件目的
本文件定義「餐廳推薦與揪團系統」的後端實作標準，涵蓋開發環境、資料庫 Schema、API 設計、核心商業邏輯、交易與防錯機制，作為後續程式碼開發與分工依據。

## 2. 建議技術棧
本專案可採下列任一組合實作，核心要求如下：

- 語言 / 框架：Node.js + Express，或 Python + FastAPI / Flask
- 資料庫：MySQL 或 PostgreSQL
- 身分驗證：JWT 或 Session
- 密碼加密：bcrypt
- ORM / 資料存取：依語言選擇對應 ORM，或直接使用 SQL Driver

若延續目前專案的既有架構，也可對應為 Java + Spring Boot + MySQL，規格內容不受影響。

## 3. 資料庫 Schema 設計
實作前需先建立以下核心資料表。以下欄位命名已盡量對齊目前專案中的 model 命名。

### 3.1 User
使用者基本資料與登入憑證。

- `userId`：PK
- `name`：帳號名稱
- `password`：bcrypt 雜湊密碼
- `email`：電子郵件
- `createdAt`：建立時間
- `updatedAt`：更新時間

### 3.2 Restaurant
餐廳主資料。

- `restaurantId`：PK
- `name`：餐廳名稱
- `category`：料理類型
- `priceRange`：價位區間
- `avgScore`：平均評分
- `ratingCount`：評分數量
- `locationAt`：地址或地點描述

### 3.3 Dish
餐點或料理分類資料。

- `categoryId`：PK
- `name`：料理名稱或分類名稱

### 3.4 UserExclusion
使用者排除名單，目前 model 對應的是「排除料理類別」，不是排除單一餐廳。

- `exclusionId`：PK
- `userId`：FK -> User
- `categoryId`：FK -> Dish

### 3.5 Rating
餐廳評論與評分。

- `ratingId`：PK
- `userId`：FK -> User
- `restaurantId`：FK -> Restaurant
- `score`：1~5 分
- `comment`：評論內容
- `ratedAt`：評分時間

### 3.6 GroupSession
揪團房間與邀請狀態。

- `sessionId`：PK
- `creatorId`：FK -> User
- `inviteCode`：唯一邀請碼
- `status`：揪團中 / 已結束
- `createdAt`：建立時間

### 3.7 Record
個人與群組決策紀錄。

- `recordId`：PK
- `userId`：FK -> User
- `restaurantId`：FK -> Restaurant
- `visitDate`：用餐日期
- `mealName`：餐點名稱或用餐名稱
- `note`：備註
- `createdAt`：建立時間

### 3.8 Ingredient
食材或補充分類資料，目前 model 已預留但尚未與核心功能直接綁定。

- `ingredientId`：PK
- `name`：食材名稱

## 4. 後端 API 規格

### 4.1 用戶管理模組

#### POST /auth/register
註冊新使用者。

- Request：`name`, `password`, `email`
- 行為：密碼使用 bcrypt 雜湊後存入 User
- 回應：註冊成功或帳號已存在

#### POST /auth/login
使用者登入。

- Request：`name`, `password`
- 行為：比對雜湊值，成功後回傳 JWT Token 或建立 Session
- 回應：Token / Session 資訊

#### GET /user/profile
取得個人資料。

- 權限：需登入
- 回應：使用者基本資訊、排除類別摘要

#### POST /user/exclusion
將特定料理類別加入排除名單。

- Request：`categoryId`
- 行為：寫入 UserExclusion，推薦查詢必須排除此料理類別

### 4.2 餐廳評分系統

#### POST /rate
新增評分。

- Request：`restaurantId`, `score`, `comment`
- 行為：寫入 Rating，並更新 Restaurant.avgScore 與 Restaurant.ratingCount
- 驗證：`score` 必須介於 1 到 5

#### GET /restaurant/:id/ratings
取得單一餐廳的所有評論。

- 回應：該餐廳所有評分列表

### 4.3 推薦餐廳功能

#### GET /recommend
取得個人推薦名單。

- 行為：根據使用者排除的料理類別推薦餐廳
- 排序：以 `avgScore` 由高到低
- 筆數：預設回傳前 5 筆

推薦邏輯參考：

```sql
SELECT * FROM Restaurant
WHERE category NOT IN (
  SELECT d.name
  FROM UserExclusion ue
  JOIN Dish d ON ue.categoryId = d.categoryId
  WHERE ue.userId = ?
)
ORDER BY avgScore DESC
LIMIT 5;
```

若排除條件查無結果，需改為推薦高評分餐廳作為 fallback。

### 4.4 群組揪團系統

#### POST /groups/create
建立群組。

- Request：建立者資訊
- 行為：生成唯一 `inviteCode`
- 回應：`sessionId` 與邀請碼

#### POST /groups/join
透過邀請碼加入群組。

- Request：`inviteCode`
- 行為：更新成員關聯資料，避免重複加入同一 `sessionId`
- 驗證：需避免重複加入同一群組

#### GET /groups/:id/recommend
取得群組推薦結果。

- 行為：整合所有成員的排除料理類別聯集
- 條件：只要任一成員排除，即不得推薦
- 喜好條件：目前 model 未提供成員喜好表，因此此規則可先以排除條件為主

### 4.5 結果記錄模組

#### GET /history/me
查詢個人歷史紀錄。

- 權限：需登入
- 回應：個人吃過的餐廳、日期、備註、是否為群組決策

#### POST /history/save
保存決策結果。

- Request：`restaurantId`, `sessionId`(可為 null)
- 行為：寫入 Record
- 規則：若為群組決策，所有成員的歷史紀錄都應可查到

## 5. 核心商業邏輯

### 5.1 密碼與登入
- 註冊時密碼必須使用 bcrypt 雜湊
- 登入時比對雜湊值，不可明文儲存密碼
- JWT 或 Session 必須能傳遞 `userId` 至各控制器

### 5.2 餐廳平均分數更新
- 每次新增或更新評分後，後端需重新計算該 `restaurantId` 的平均分數
- 將結果同步更新至 Restaurant.avgScore 與 Restaurant.ratingCount

### 5.3 推薦機制
- 個人推薦：依 `UserExclusion` 排除的料理類別篩選，再查找餐廳
- 群組推薦：若後續補上群組成員表，可再合併成員排除條件；目前以單一使用者排除邏輯為基準
- 若查無結果，啟用 fallback：改推高評分餐廳或隨機推薦

### 5.4 揪團邏輯
- `inviteCode` 必須唯一
- 同一用戶不可重複加入同一 `sessionId`
- `status` 應支援「揪團中」與「已結束」

## 6. 後端資料流程建議

### 6.1 註冊流程
1. 驗證欄位是否完整
2. 檢查 name / email 是否重複
3. bcrypt 加密密碼
4. 寫入 User
5. 回傳註冊結果

### 6.2 登入流程
1. 以 name 查詢使用者
2. 比對 password hash
3. 成功後簽發 JWT 或建立 Session
4. 後續 API 以 middleware 驗證身份

### 6.3 推薦流程
1. 取得登入者 userId
2. 讀取排除的料理類別
3. 執行推薦 SQL
4. 若結果為空則 fallback
5. 回傳前 5 筆結果

### 6.4 群組推薦流程
1. 驗證群組狀態與成員權限
2. 匯總所有成員的排除條件
3. 執行聯合篩選
4. 若為空，使用 fallback

### 6.5 評分與紀錄流程
1. 寫入 Rating
2. 重新計算餐廳平均分
3. 更新 Restaurant.avgScore 與 Restaurant.ratingCount
4. 如使用者已決定用餐，再寫入 Record

## 7. Middleware 與安全性要求

- JWT / Session 驗證 middleware 必須統一處理登入狀態
- Controller 不可直接信任前端傳入的 `userId`
- 所有密碼只允許以雜湊形式保存
- 建議為關鍵 API 增加請求驗證與錯誤回應格式統一

## 8. 防錯與例外處理

- 排除類別過多導致查無結果時，需提供 fallback 推薦
- 加入群組前需檢查是否已存在於同一 `sessionId`
- 評分資料需檢查 `score` 範圍是否為 1~5
- 邀請碼需驗證格式與有效性
- 查無資料時需回傳明確錯誤訊息，不可直接回空陣列而不說明

## 9. Transaction 建議

以下操作建議使用資料庫交易：

- 評分新增與平均分更新
- 群組建立與邀請碼生成
- 群組加入與成員關係建立
- 決策結果寫入 Record 與群組狀態更新

交易原則：
- 任一步驟失敗即回滾
- 確保資料一致性與可追蹤性

## 10. 開發工作清單

- DB 連線測試
- JWT / Session middleware 實作
- bcrypt 密碼加密整合
- User / Restaurant / Rating / GroupSession / Record CRUD
- 個人推薦 SQL 與 fallback 機制
- 群組推薦 SQL 與排除類別聯集邏輯
- 歷史紀錄寫入與查詢
- 交易處理與錯誤碼統一

## 11. 驗收標準

- 可正常註冊、登入與取得個人資料
- 可新增排除類別
- 可提交評分並同步更新平均分數
- 可取得個人與群組推薦結果
- 可建立群組、加入群組並產生邀請碼
- 可保存與查詢歷史紀錄
- 所有關鍵流程皆具備錯誤處理與 fallback
