// 狀態中添加用戶的排除偏好
const state = {
    token: localStorage.getItem("rs_token") || "",
    userId: localStorage.getItem("rs_userId") || "",
    userName: localStorage.getItem("rs_userName") || "",
    currentPage: "auth",
    restaurants: [],
    currentExclusions: [],
    userExclusionPreferences: [], // 新增：存儲用戶的原始排除偏好
    exclusionOptions: {
        ingredients: [],
        dishes: [],
        restaurants: []
    }
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
            loadExclusionOptions();
            hideExclusions();
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
        const data = await request("/api/auth/register", { method: "POST", body: JSON.stringify(body) });
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
        const data = await request("/api/auth/login", { method: "POST", body: JSON.stringify(body) });
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
        state.restaurants = await request("/api/restaurant/all");
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
        await request("/api/restaurant/save", { method: "POST", body: JSON.stringify(body) });
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
        await request("/api/restaurant/rate", { method: "POST", body: JSON.stringify(body) });
        showToast("評分已送出", "success");
        e.target.reset();
        loadRestaurants();
    } catch (err) {
        showToast(err.message, "error");
    }
});

// ============ 推薦功能 ============

async function loadExclusionOptions() {
    try {
        const [ingredients, dishes, restaurants] = await Promise.all([
            request("/api/ingredient/all"),
            request("/api/dish/all"),
            request("/api/restaurant/all")
        ]);

        state.exclusionOptions = {
            ingredients,
            dishes,
            restaurants
        };

        updateExclusionItemList();
    } catch (err) {
        showToast(err.message, "error");
    }
}

function getSelectedValues(select) {
    return Array.from(select.selectedOptions).map(option => option.value);
}

function getExclusionTypeMeta(type) {
    switch (type) {
        case "ingredient":
            return { label: "選擇要排除的食材", key: "ingredients", idKey: "ingredientId" };
        case "dish":
            return { label: "選擇要排除的菜系", key: "dishes", idKey: "categoryId" };
        case "restaurant":
            return { label: "選擇要排除的餐廳", key: "restaurants", idKey: "restaurantId" };
        default:
            return { label: "選擇要排除的項目", key: "ingredients", idKey: "ingredientId" };
    }
}

function updateExclusionItemList() {
    const typeSelect = document.getElementById("exclude-type");
    const itemsSelect = document.getElementById("exclude-items");
    const itemsLabel = document.getElementById("exclude-items-label");
    if (!typeSelect || !itemsSelect || !itemsLabel) return;

    const type = typeSelect.value;
    const meta = getExclusionTypeMeta(type);
    const options = state.exclusionOptions[meta.key] || [];

    itemsLabel.textContent = meta.label;
    if (!options.length) {
        itemsSelect.innerHTML = '<option value="">載入中...</option>';
        return;
    }

    itemsSelect.innerHTML = options
        .map(item => `<option value="${item[meta.idKey]}">${item.name}</option>`)
        .join("");
}

function hideExclusions() {
    const section = document.getElementById("exclusion-section");
    if (section) {
        section.classList.add("hidden");
    }
}

async function showExclusions() {
    await loadExclusions();
    const section = document.getElementById("exclusion-section");
    if (section) {
        section.classList.remove("hidden");
    }
}

async function addExclusion() {
    try {
        if (!state.token) throw new Error("請先登入");

        const typeSelect = document.getElementById("exclude-type");
        const itemsSelect = document.getElementById("exclude-items");
        const type = typeSelect.value;
        const selectedIds = getSelectedValues(itemsSelect);

        if (!selectedIds.length) {
            throw new Error("請至少選擇一個排除項目");
        }

        let exclusionsAdded = 0;
        let existingCount = 0;
        let notFoundItems = [];

        if (type === "ingredient") {
            const ingredients = selectedIds
                .map(id => state.exclusionOptions.ingredients.find(item => String(item.ingredientId) === id))
                .filter(Boolean)
                .map(item => item.name);

            const result = await request("/api/user/exclusion/ingredient", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ ingredients: ingredients.join(",") })
            });

            exclusionsAdded += result.added?.length || 0;
            existingCount += result.existing?.length || 0;
            if (result.notFound?.length > 0) {
                notFoundItems.push(...result.notFound);
            }
        } else if (type === "dish") {
            for (const categoryId of selectedIds) {
                await request(`/api/user/exclusion/category?categoryId=${categoryId}`, { method: "POST" });
                exclusionsAdded++;
            }
        } else if (type === "restaurant") {
            for (const restaurantId of selectedIds) {
                await request(`/api/user/exclusion/restaurant?restaurantId=${restaurantId}`, { method: "POST" });
                exclusionsAdded++;
            }
        }

        if (exclusionsAdded > 0 || existingCount > 0 || notFoundItems.length > 0) {
            if (!document.getElementById("exclusion-section")?.classList.contains("hidden")) {
                await loadExclusions();
            }
            const messageParts = [];
            if (exclusionsAdded > 0) {
                messageParts.push(`已加入 ${exclusionsAdded} 個排除項目`);
            }
            if (existingCount > 0) {
                messageParts.push(`已有 ${existingCount} 個排除項目`);
            }
            if (notFoundItems.length > 0) {
                messageParts.push(`資料庫中找不到：${notFoundItems.join('、')}`);
            }
            showToast(messageParts.join('，'), "success");
        } else {
            throw new Error("沒有新增任何排除項目");
        }

    } catch (err) {
        showToast(err.message, "error");
    }
}

// 輔助函數：標準化排除輸入文字
function normalizeInputText(text) {
    if (!text) return '';
    return text
        .trim()
        .replace(/[，；;。！?…！]/g, '')  // 移除標點，不要轉成逗號（分割已在上層做）
        .replace(/[""''"']/g, '')
        .replace(/[()（）]/g, '')
        .replace(/　/g, '')               // 全形空格直接移除
        .replace(/\s+/g, '')              // 中文詞彙間不需要空格
}

async function loadExclusions() {
    try {
        const exclusions = await request("/api/user/exclusions");
        state.currentExclusions = exclusions;

        state.userExclusionPreferences = exclusions.map(e => {
            if (e.ingredient && e.ingredient.name) {
                return {
                    type: 'ingredient',
                    value: e.ingredient.name,
                    ingredientId: e.ingredient.ingredientId
                };
            }

            if (e.dish && e.dish.name) {
                return {
                    type: 'category',
                    value: e.dish.name,
                    categoryId: e.dish.categoryId
                };
            }

            if (e.restaurant && e.restaurant.name) {
                return {
                    type: 'restaurant',
                    value: e.restaurant.name,
                    restaurantId: e.restaurant.restaurantId
                };
            }

            return {
                type: 'other',
                value: `排除項目 ${e.exclusionId}`
            };
        });
        
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
                typeLabel = "食材";
                break;
            case 'category':
                typeLabel = "菜系";
                break;
            case 'restaurant':
                typeLabel = "餐廳";
                break;
            default:
                typeLabel = "其他";
        }

        let targetId = "";
        if (pref.type === "ingredient") targetId = pref.ingredientId;
        if (pref.type === "category") targetId = pref.categoryId;
        if (pref.type === "restaurant") targetId = pref.restaurantId;

        const actionButton = targetId
            ? `<button type="button" class="exclusion-remove" data-type="${pref.type}" data-id="${targetId}">取消</button>`
            : "";

        return `<div class="list-item exclusion-item">
            <div class="exclusion-item-info">
                <span class="exclusion-tag">${typeLabel}</span>
                <span>${pref.value}</span>
            </div>
            ${actionButton}
        </div>`;
    });
    
    container.innerHTML = exclusionItems.join("");
    container.querySelectorAll(".exclusion-remove").forEach(button => {
        button.addEventListener("click", async () => {
            const type = button.dataset.type;
            const targetId = button.dataset.id;
            await removeExclusion(type, targetId);
        });
    });
}

async function removeExclusion(type, targetId) {
    try {
        if (!state.token) throw new Error("請先登入");
        if (!targetId) throw new Error("缺少排除項目編號");

        if (type === "ingredient") {
            await request(`/api/user/exclusion/ingredient/${targetId}`, { method: "DELETE" });
        } else if (type === "category") {
            await request(`/api/user/exclusion/category/${targetId}`, { method: "DELETE" });
        } else if (type === "restaurant") {
            await request(`/api/user/exclusion/restaurant/${targetId}`, { method: "DELETE" });
        } else {
            throw new Error("不支援的排除種類");
        }

        await loadExclusions();
        showToast("已取消排除", "success");
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function getRecommendations() {
    try {
        if (!state.token) throw new Error("請先登入");
        const recs = await request("/api/recommend/personal");
        
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
        const session = await request("/api/groups/create", { method: "POST" });
        
        const display = document.getElementById("group-display");
        display.innerHTML = `
            <div>
                <strong>群組已建立！</strong>
                <p style="margin-top: 10px; font-size: 14px;">
                    群組編號：<code style="background: #f0f0f0; padding: 2px 5px; border-radius: 3px;">${session.sessionId}</code>
                </p>
                <p style="margin-top: 8px; font-size: 14px;">
                    邀請碼：<code style="background: #f0f0f0; padding: 2px 5px; border-radius: 3px;">${session.inviteCode}</code>
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
        const session = await request(`/api/groups/join?inviteCode=${body.inviteCode}`, { method: "POST" });
        
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
        
        await request("/api/history/save", { method: "POST", body: JSON.stringify(body) });
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
        const history = await request("/api/history/me");
        
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

const excludeTypeSelect = document.getElementById("exclude-type");
if (excludeTypeSelect) {
    excludeTypeSelect.addEventListener("change", updateExclusionItemList);
}

// 如果已登入，進入菜單頁面
if (state.token && state.userName) {
    goPage("menu");
}