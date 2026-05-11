// State管理
const state = {
    token: localStorage.getItem("rs_token") || "",
    userId: localStorage.getItem("rs_userId") || "",
    userName: localStorage.getItem("rs_userName") || "",
    currentPage: "auth",
    restaurants: [],
    dishes: [],
    currentExclusions: []
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
        throw new Error(payload.message || "請求失敗");
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
        const select = document.getElementById("exclusion-category");
        const options = dishes.map(d => 
            `<option value="${d.categoryId}">${d.name}</option>`
        ).join("");
        select.innerHTML = '<option value="">選擇要排除的分類</option>' + options;
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function addExclusion() {
    try {
        if (!state.token) throw new Error("請先登入");
        const categoryId = Number(document.getElementById("exclusion-category").value);
        if (!categoryId) throw new Error("請選擇分類");
        
        await request(`/user/exclusion?categoryId=${categoryId}`, { method: "POST" });
        
        // 添加到本地狀態
        const dish = state.dishes.find(d => d.categoryId === categoryId);
        if (dish && !state.currentExclusions.find(e => e.categoryId === categoryId)) {
            state.currentExclusions.push(dish);
        }
        
        loadExclusions();
        showToast("已加入排除類別", "success");
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function loadExclusions() {
    try {
        if (!state.token) throw new Error("請先登入");
        const exclusions = await request("/user/exclusions");
        state.currentExclusions = exclusions;
        renderExclusions();
    } catch (err) {
        showToast(err.message, "error");
    }
}

function renderExclusions() {
    const container = document.getElementById("exclusion-display");
    if (!state.currentExclusions.length) {
        container.innerHTML = '<p style="color: #999;">無</p>';
        return;
    }
    container.innerHTML = state.currentExclusions.map(e => 
        `<div class="list-item" style="margin-bottom: 8px;">${e.dish ? e.dish.name : e.categoryId}</div>`
    ).join("");
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
