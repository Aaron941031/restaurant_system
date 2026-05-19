// 狀態中添加用戶的排除偏好
const state = {
    token: localStorage.getItem("rs_token") || "",
    userId: localStorage.getItem("rs_userId") || "",
    userName: localStorage.getItem("rs_userName") || "",
    currentPage: "auth",
    restaurants: [],
    dishes: [],
    currentExclusions: [],
    userExclusionPreferences: [] // 新增：存儲用戶的原始排除偏好
};

// 頁面導航
function goPage(pageName) {
    document.querySelectorAll('.page').forEach(page => {
        page.classList.remove('active');
    });
    
    const page = document.getElementById(`page-${pageName}`);
    if (page) {
        page.classList.add('active');
        state.currentPage = pageName;
        
        // 根據頁面類別初始化
        if (pageName === 'restaurants') {
            loadRestaurants();
        } else if (pageName === 'rate' || pageName === 'history') {
            loadRestaurantSelects();
        } else if (pageName === 'recommend') {
            loadDishes();
            loadExclusions();
        }
    }
    window.scrollTo(0, 0);
}

// API 請求
async function request(path, options = {}) {
    const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
    if (state.token) headers.Authorization = `Bearer ${state.token}`;

    const response = await fetch(path, { ...options, headers });
    const payload = await response.json();
    if (!response.ok || payload.success === false) {
        const message = payload.message || "請求失敗";
        if (/User not found|Invalid token|Missing or invalid authorization header/i.test(message)) {
            clearSession();
            throw new Error("登入狀態失效，請重新登入");
        }
        throw new Error(message);
    }
    return payload.data;
}

// 顯示提示訊息
function showToast(message, type = "success") {
    const toast = document.getElementById("toast");
    toast.className = `toast ${type}`;
    toast.textContent = message;
    setTimeout(() => {
        toast.classList.add("hidden");
    }, 2600);
}

function clearSession() {
    state.token = "";
    state.userId = "";
    state.userName = "";
    state.currentExclusions = [];
    state.userExclusionPreferences = [];
    localStorage.removeItem("rs_token");
    localStorage.removeItem("rs_userId");
    localStorage.removeItem("rs_userName");
    updateSession();
    goPage("auth");
}

// 表單數據轉JSON
function formDataToJson(form) {
    const fd = new FormData(form);
    return Object.fromEntries(fd.entries());
}

// 更新 Session 信息
function updateSession() {
    const info = document.getElementById("session-info");
    if (state.userName) {
        info.textContent = `${state.userName}`;
    } else {
        info.textContent = "未登入";
    }
}

// 設置 Session
function setSession(token, userId, userName) {
    state.token = token || "";
    state.userId = userId || "";
    state.userName = userName || "";
    localStorage.setItem("rs_token", state.token);
    localStorage.setItem("rs_userId", String(state.userId || ""));
    localStorage.setItem("rs_userName", state.userName || "");
    updateSession();
}

// 登出
function logout() {
    setSession("", "", "");
    state.currentExclusions = [];
    state.userExclusionPreferences = []; // 清空用戶排除偏好
    goPage("auth");
    showToast("已登出", "success");
}

// ============ 認證功能 ============

document.getElementById("register-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
        const body = formDataToJson(e.target);
        const data = await request("/auth/register", { method: "POST", body: JSON.stringify(body) });
        setSession(data.token, data.userId, data.name);
        showToast("註冊成功！歡迎 " + data.name, "success");
        goPage("menu");
    } catch (err) {
        showToast(err.message, "error");
    }
});

document.getElementById("login-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
        const body = formDataToJson(e.target);
        const data = await request("/auth/login", { method: "POST", body: JSON.stringify(body) });
        setSession(data.token, data.userId, data.name);
        showToast("登入成功！", "success");
        goPage("menu");
    } catch (err) {
        showToast(err.message, "error");
    }
});

// ============ 餐廳功能 ============

async function loadRestaurants() {
    try {
        state.restaurants = await request("/restaurant/all");
        renderRestaurants();
    } catch (err) {
        showToast(err.message, "error");
    }
}

function renderRestaurants() {
    const container = document.getElementById("restaurants-list");
    if (!state.restaurants.length) {
        container.innerHTML = '<div class="empty-state"><div class="empty-state-icon"></div>暫無餐廳</div>';
        return;
    }
    container.innerHTML = state.restaurants.map(r => `
        <div class="list-item">
            <div class="list-item-title">${r.name}</div>
            <div class="list-item-meta">
                <strong>${r.category}</strong> | ${r.priceRange} | ⭐ ${r.avgScore} (${r.ratingCount}人)
            </div>
            <div class="list-item-meta">${r.locationAt}</div>
        </div>
    `).join("");
}

document.getElementById("restaurant-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
        const body = formDataToJson(e.target);
        body.avgScore = 0;
        body.ratingCount = 0;
        await request("/restaurant/save", { method: "POST", body: JSON.stringify(body) });
        showToast("餐廳已新增", "success");
        e.target.reset();
        loadRestaurants();
    } catch (err) {
        showToast(err.message, "error");
    }
});

async function loadRestaurantSelects() {
    try {
        await loadRestaurants();
        const options = state.restaurants.map(r => 
            `<option value="${r.restaurantId}">${r.name} (${r.category})</option>`
        ).join("");
        document.getElementById("rate-restaurant-select").innerHTML = options;
        document.getElementById("history-restaurant-select").innerHTML = options;
    } catch (err) {
        showToast(err.message, "error");
    }
}

// ============ 評分功能 ============

document.getElementById("rating-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
        if (!state.token) throw new Error("請先登入");
        const body = formDataToJson(e.target);
        body.restaurantId = Number(body.restaurantId);
        body.score = Number(body.score);
        await request("/restaurant/rate", { method: "POST", body: JSON.stringify(body) });
        showToast("評分已送出", "success");
        e.target.reset();
        loadRestaurants();
    } catch (err) {
        showToast(err.message, "error");
    }
});

// ============ 推薦功能 ============

async function loadDishes() {
    try {
        const dishes = await request("/dish/all");
        state.dishes = dishes;
        
        // 載入餐廳選項
        await loadRestaurants();
        const restaurantSelect = document.getElementById("exclude-restaurant");
        const restaurantOptions = state.restaurants.map(r => 
            `<option value="${r.restaurantId}">${r.name}</option>`
        ).join("");
        restaurantSelect.innerHTML = '<option value="">選擇要排除的餐廳</option>' + restaurantOptions;
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function addExclusion() {
    try {
        if (!state.token) throw new Error("請先登入");

        const ingredientsInput = document.getElementById("exclude-ingredients");
        const restaurantSelect = document.getElementById("exclude-restaurant");
        const dishesInput = document.getElementById("exclude-dishes");

        const ingredients = ingredientsInput.value.trim();
        const restaurantId = restaurantSelect.value;
        const dishes = dishesInput.value.trim();

        // 至少要有一個排除項目
        if (!ingredients && !restaurantId && !dishes) {
            throw new Error("請至少選擇或輸入一個排除項目");
        }

        let exclusionsAdded = 0;

        // 處理食物品項排除 - 映射到相關分類
        if (ingredients) {
            const ingredientList = ingredients.split(',').map(i => i.trim()).filter(i => i);
            for (const ingredient of ingredientList) {
                const categoryId = mapIngredientToCategory(ingredient);
                if (categoryId) {
                    await request(`/user/exclusion?categoryId=${categoryId}`, { method: "POST" });
                    // 存儲用戶的原始輸入
                    state.userExclusionPreferences.push({
                        type: 'ingredient',
                        value: ingredient,
                        categoryId: categoryId
                    });
                    exclusionsAdded++;
                }
            }
            ingredientsInput.value = "";
        }

        // 處理餐廳排除 - 排除該餐廳所屬的分類
        if (restaurantId) {
            const restaurant = state.restaurants.find(r => r.restaurantId == restaurantId);
            if (restaurant) {
                const categoryId = mapRestaurantNameToCategory(restaurant.name);
                if (categoryId) {
                    await request(`/user/exclusion?categoryId=${categoryId}`, { method: "POST" });
                    // 存儲用戶的原始輸入
                    state.userExclusionPreferences.push({
                        type: 'restaurant',
                        value: restaurant.name,
                        restaurantId: restaurantId,
                        categoryId: categoryId
                    });
                    exclusionsAdded++;
                }
            }
            restaurantSelect.value = "";
        }

        // 處理菜餚排除 - 映射到相關分類
        if (dishes) {
            const dishList = dishes.split(',').map(d => d.trim()).filter(d => d);
            for (const dish of dishList) {
                const categoryId = mapDishNameToCategory(dish);
                if (categoryId) {
                    await request(`/user/exclusion?categoryId=${categoryId}`, { method: "POST" });
                    // 存儲用戶的原始輸入
                    state.userExclusionPreferences.push({
                        type: 'dish',
                        value: dish,
                        categoryId: categoryId
                    });
                    exclusionsAdded++;
                }
            }
            dishesInput.value = "";
        }

        if (exclusionsAdded > 0) {
            await loadExclusions();
            showToast(`已加入 ${exclusionsAdded} 個排除項目`, "success");
        } else {
            throw new Error("無法識別輸入的項目，請檢查拼寫");
        }

    } catch (err) {
        showToast(err.message, "error");
    }
}

// 輔助函數：將食材映射到分類
function mapIngredientToCategory(ingredient) {
    const ingredientMap = {
        '魚': 1, '魚肉': 1, '海鮮': 1, '蝦': 1, '蟹': 1, '貝類': 1,
        '牛': 2, '牛肉': 2, '豬': 2, '豬肉': 2, '羊': 2, '羊肉': 2, '雞': 2, '雞肉': 2, '鴨': 2, '鴨肉': 2,
        '洋蔥': 3, '蒜': 3, '薑': 3, '蔬菜': 3, '青菜': 3, '豆腐': 3,
        '米': 4, '飯': 4, '麵': 4, '麵條': 4, '餃子': 4,
        '起司': 5, '奶酪': 5, '披薩': 5, '義大利麵': 5,
        '漢堡': 6, '薯條': 6, '炸物': 6,
        '壽司': 7, '生魚片': 7, '刺身': 7, '日式': 7,
        '泡菜': 8, '韓式': 8, '烤肉': 8,
        '咖哩': 9, '泰式': 9, '冬陰功': 9,
        '咖啡': 10, '茶': 10, '飲料': 10
    };
    
    // 精確匹配
    if (ingredientMap[ingredient]) {
        return ingredientMap[ingredient];
    }
    
    // 模糊匹配
    for (const [key, value] of Object.entries(ingredientMap)) {
        if (ingredient.includes(key) || key.includes(ingredient)) {
            return value;
        }
    }
    
    return null;
}

// 輔助函數：將餐廳名稱映射到分類
function mapRestaurantNameToCategory(restaurantName) {
    const nameMap = {
        '壽司': 7, '日式': 7, '日本': 7, '拉麵': 7,
        '韓式': 8, '烤肉': 8, '泡菜': 8, '韓國': 8,
        '義大利': 5, '披薩': 5, '義式': 5,
        '美式': 6, '漢堡': 6, '美式': 6,
        '中式': 2, '中國': 2, '川菜': 2, '滬菜': 2,
        '泰式': 9, '泰國': 9, '冬陰功': 9,
        '咖啡': 10, '星巴克': 10, '飲料': 10
    };
    
    for (const [key, value] of Object.entries(nameMap)) {
        if (restaurantName.includes(key)) {
            return value;
        }
    }
    
    return 2; // 預設為中式
}

// 輔助函數：將菜餚名稱映射到分類
function mapDishNameToCategory(dishName) {
    const dishMap = {
        '壽司': 7, '生魚片': 7, '刺身': 7, '拉麵': 7, '日式': 7,
        '泡菜': 8, '烤肉': 8, '韓式': 8, '部隊鍋': 8,
        '披薩': 5, '義大利麵': 5, '義式': 5, '千層麵': 5,
        '漢堡': 6, '薯條': 6, '美式': 6, '炸雞': 6,
        '炒飯': 2, '炒麵': 2, '中式': 2, '宮保雞丁': 2,
        '咖哩': 9, '泰式': 9, '冬陰功': 9, '泰國菜': 9,
        '咖啡': 10, '茶': 10, '果汁': 10, '飲料': 10
    };
    
    for (const [key, value] of Object.entries(dishMap)) {
        if (dishName.includes(key)) {
            return value;
        }
    }
    
    return 2; // 預設為中式
}

async function loadExclusions() {
    try {
        const exclusions = await request("/user/exclusions");
        state.currentExclusions = exclusions;
        
        // 如果沒有用戶偏好數據，嘗試從分類數據重建（簡化版本）
        if (!state.userExclusionPreferences.length && exclusions.length > 0) {
            state.userExclusionPreferences = exclusions.map(e => ({
                type: 'category',
                value: e.dish ? e.dish.name : `分類 ${e.categoryId}`,
                categoryId: e.dish ? e.dish.categoryId : e.categoryId
            }));
        }
        
        renderExclusions();
    } catch (err) {
        showToast(err.message, "error");
    }
}

function renderExclusions() {
    const container = document.getElementById("exclusion-display");
    if (!state.userExclusionPreferences.length) {
        container.innerHTML = '<p style="color: #999;">無</p>';
        return;
    }
    
    const exclusionItems = state.userExclusionPreferences.map(pref => {
        let typeLabel = "";
        
        switch (pref.type) {
            case 'ingredient':
                typeLabel = "食物品項";
                break;
            case 'restaurant':
                typeLabel = "餐廳";
                break;
            case 'dish':
                typeLabel = "菜餚";
                break;
            default:
                typeLabel = "其他";
        }
        
        return `<div class="list-item" style="margin-bottom: 8px;">
            <span style="font-size: 12px; color: #666; background: #f0f0f0; padding: 2px 6px; border-radius: 4px; margin-right: 8px;">${typeLabel}</span>
            ${pref.value}
        </div>`;
    });
    
    container.innerHTML = exclusionItems.join("");
}

async function getRecommendations() {
    try {
        if (!state.token) throw new Error("請先登入");
        const recs = await request("/recommend/personal");
        
        const container = document.getElementById("recommend-list");
        if (!recs.length) {
            container.innerHTML = '<div class="empty-state"><div class="empty-state-icon">⭐</div>暫無推薦</div>';
            return;
        }
        
        container.innerHTML = recs.map(r => `
            <div class="list-item">
                <div class="list-item-title">${r.name}</div>
                <div class="list-item-meta">
                    分類：<strong>${r.category}</strong> | 評分：⭐ ${r.avgScore.toFixed(1)} | ${r.priceRange}
                </div>
            </div>
        `).join("");
    } catch (err) {
        showToast(err.message, "error");
    }
}

// ============ 揪團功能 ============

async function createGroup() {
    try {
        if (!state.token) throw new Error("請先登入");
        const session = await request("/groups/create", { method: "POST" });
        
        const display = document.getElementById("group-display");
        display.innerHTML = `
            <div>
                <strong>群組已建立！</strong>
                <p style="margin-top: 10px; font-size: 14px;">
                    📌 群組編號：<code style="background: #f0f0f0; padding: 2px 5px; border-radius: 3px;">${session.sessionId}</code>
                </p>
                <p style="margin-top: 8px; font-size: 14px;">
                    🔗 邀請碼：<code style="background: #f0f0f0; padding: 2px 5px; border-radius: 3px;">${session.inviteCode}</code>
                </p>
                <p style="margin-top: 12px; font-size: 12px; color: #999;">分享邀請碼給朋友加入</p>
            </div>
        `;
        showToast("群組已建立", "success");
    } catch (err) {
        showToast(err.message, "error");
    }
}

document.getElementById("join-group-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
        if (!state.token) throw new Error("請先登入");
        const body = formDataToJson(e.target);
        const session = await request(`/groups/join?inviteCode=${body.inviteCode}`, { method: "POST" });
        
        const display = document.getElementById("group-display");
        display.innerHTML = `
            <div>
                <strong>已加入群組</strong>
                <p style="margin-top: 10px; font-size: 14px;">
                    群組編號：${session.sessionId} | 邀請碼：${session.inviteCode}
                </p>
            </div>
        `;
        showToast("已加入群組", "success");
        e.target.reset();
    } catch (err) {
        showToast(err.message, "error");
    }
});

// ============ 歷史紀錄功能 ============

document.getElementById("history-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
        if (!state.token) throw new Error("請先登入");
        const body = formDataToJson(e.target);
        body.restaurantId = Number(body.restaurantId);
        
        await request("/history/save", { method: "POST", body: JSON.stringify(body) });
        showToast("紀錄已保存", "success");
        e.target.reset();
        loadHistory();
    } catch (err) {
        showToast(err.message, "error");
    }
});

async function loadHistory() {
    try {
        if (!state.token) throw new Error("請先登入");
        const history = await request("/history/me");
        
        const container = document.getElementById("history-list");
        if (!history.length) {
            container.innerHTML = '<div class="empty-state"><div class="empty-state-icon"></div>暫無紀錄</div>';
            return;
        }
        
        container.innerHTML = history.map(h => `
            <div class="list-item">
                <div class="list-item-title">${h.mealName || "未命名"}</div>
                <div class="list-item-meta">
                    餐廳ID：${h.restaurantId} | ${h.visitDate}
                </div>
                <div class="list-item-meta">${h.note || "無備註"}</div>
            </div>
        `).join("");
    } catch (err) {
        showToast(err.message, "error");
    }
}

// ============ 初始化 ============

updateSession();

// 如果已登入，進入菜單頁面
if (state.token && state.userName) {
    goPage("menu");
}
