// ============ 全域狀態 ============
// 儲存登入資訊、頁面狀態、餐廳清單、排除偏好等共用資料
const state = {
    token: localStorage.getItem("rs_token") || "",       // JWT 登入憑證
    userId: localStorage.getItem("rs_userId") || "",     // 登入使用者 ID
    userName: localStorage.getItem("rs_userName") || "", // 登入使用者名稱
    currentPage: "auth",       // 目前所在頁面
    restaurants: [],           // 餐廳清單快取
    currentExclusions: [],     // 目前排除項目原始資料
    userExclusionPreferences: [], // 格式化後的排除偏好（供顯示用）
    exclusionOptions: {        // 排除選項的下拉清單資料
        ingredients: [],
        dishes: [],
        restaurants: []
    },
    _excludeSelected: [],         // 排除功能中已選取的項目（tag 清單）
    _activeGroupSessionId: null,  // 目前展開的群組 sessionId
    _selectedRestaurant: null     // 群組推薦中已選取的餐廳
};

// ============ 頁面導航 ============
// 切換頁面：隱藏所有 .page，顯示目標頁面，並執行對應的初始化
function goPage(pageName) {
    document.querySelectorAll('.page').forEach(page => {
        page.classList.remove('active');
    });

    const page = document.getElementById(`page-${pageName}`);
    if (page) {
        page.classList.add('active');
        state.currentPage = pageName;

        if (pageName === 'restaurants') {
            loadRestaurants();
        } else if (pageName === 'rate') {
            loadRestaurantSelects(); // 載入評分用的餐廳下拉選單
        } else if (pageName === 'recommend') {
            loadExclusionOptions();  // 載入排除選項
            hideExclusions();        // 預設隱藏已排除項目區塊
        } else if (pageName === 'groups') {
            // 切換到揪團頁時收起詳情面板，重置狀態
            const panel = document.getElementById('group-detail-panel');
            if (panel) panel.classList.add('hidden');
            state._activeGroupSessionId = null;
        }
    }

    window.scrollTo(0, 0);
}

// ============ API 請求 ============
// 統一的 fetch 封裝：自動帶入 JWT、解析回應、處理登入失效
async function request(path, options = {}) {
    const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
    if (state.token) headers.Authorization = `Bearer ${state.token}`;

    const response = await fetch(path, { ...options, headers });
    const payload = await response.json();

    if (!response.ok || payload.success === false) {
        const message = payload.message || "請求失敗";
        // 若 token 失效則自動登出並跳回登入頁
        if (/User not found|Invalid token|Missing or invalid authorization header/i.test(message)) {
            clearSession();
            throw new Error("登入狀態失效，請重新登入");
        }
        throw new Error(message);
    }
    return payload.data;
}

// ============ Toast 提示訊息 ============
// 短暫顯示操作結果（成功 / 錯誤）
function showToast(message, type = "success") {
    const toast = document.getElementById("toast");
    toast.className = `toast ${type}`;
    toast.textContent = message;
    setTimeout(() => { toast.classList.add("hidden"); }, 2600);
}

// ============ Session 管理 ============

// 清除登入狀態並跳回登入頁
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

// 將表單資料轉為 JSON 物件
function formDataToJson(form) {
    const fd = new FormData(form);
    return Object.fromEntries(fd.entries());
}

// 更新頁面頂部顯示的使用者名稱
function updateSession() {
    const info = document.getElementById("session-info");
    info.textContent = state.userName ? state.userName : "未登入";
}

// 儲存登入資訊到 state 與 localStorage
function setSession(token, userId, userName) {
    state.token = token || "";
    state.userId = userId || "";
    state.userName = userName || "";
    localStorage.setItem("rs_token", state.token);
    localStorage.setItem("rs_userId", String(state.userId || ""));
    localStorage.setItem("rs_userName", state.userName || "");
    updateSession();
}

// 登出：清除 session 並跳回登入頁
function logout() {
    setSession("", "", "");
    state.currentExclusions = [];
    state.userExclusionPreferences = [];
    goPage("auth");
    showToast("已登出", "success");
}

// ============ 認證功能 ============

// 註冊表單送出
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

// 登入表單送出
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

// 從後端載入所有餐廳並渲染列表
async function loadRestaurants() {
    try {
        state.restaurants = await request("/api/restaurant/all");
        renderRestaurants();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// 將餐廳清單渲染到 #restaurants-list
function renderRestaurants() {
    const container = document.getElementById("restaurants-list");
    if (!state.restaurants.length) {
        container.innerHTML = '<div class="empty-state"><div class="empty-state-icon"></div>暫無餐廳</div>';
        return;
    }
    container.innerHTML = state.restaurants.map(r => {
        // 相容後端主鍵名稱為 restaurantId 或 id 的情況
        const targetId = r.restaurantId || r.id;
        return `
        <div class="list-item">
            <div class="list-item-title">${r.name}</div>
            <div class="list-item-meta">
                <strong>${r.category}</strong> | ${r.priceRange} | ⭐ ${r.avgScore} (${r.ratingCount}人)
            </div>
            <div class="list-item-meta">${r.locationAt}</div>
            <div class="button-group" style="margin-top: 12px;">
                <button type="button" style="padding: 4px 8px; margin-right: 5px; cursor: pointer; border-radius: 4px; border: 1px solid #ccc; background: #f9f9f9;" onclick="viewMenu(${targetId})">查看菜單</button>
                <button type="button" style="padding: 4px 8px; cursor: pointer; border-radius: 4px; border: 1px solid #ccc; background: #f9f9f9;" onclick="viewReviews(${targetId})">查看評論</button>
            </div>
        </div>`;
    }).join("");
}

// 載入評分頁的餐廳下拉選單
async function loadRestaurantSelects() {
    try {
        await loadRestaurants();
        const options = state.restaurants.map(r => {
            const targetId = r.restaurantId || r.id;
            return `<option value="${targetId}">${r.name}</option>`;
        }).join("");
        document.getElementById("rate-restaurant-select").innerHTML = options;
    } catch (err) {
        showToast(err.message, "error");
    }
}

// ============ 評分功能 ============

// 評分表單送出
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
        loadRestaurants(); // 重新載入以更新平均評分
    } catch (err) {
        showToast(err.message, "error");
    }
});

// ============ 推薦功能 ============

// 載入排除設定的下拉選項（食材、菜餚、餐廳）
async function loadExclusionOptions() {
    try {
        const [ingredients, dishes, restaurants] = await Promise.all([
            request("/api/ingredient/all"),
            request("/api/dish/all"),
            request("/api/restaurant/all")
        ]);
        state.exclusionOptions = { ingredients, dishes, restaurants };
        updateExclusionItemList(); // 根據目前選取的排除種類更新下拉清單
    } catch (err) {
        showToast(err.message, "error");
    }
}

// 取得 <select multiple> 中所有選取的值
function getSelectedValues(select) {
    return Array.from(select.selectedOptions).map(option => option.value);
}

// 依排除種類回傳對應的標籤文字、資料鍵名與 ID 欄位名稱
function getExclusionTypeMeta(type) {
    switch (type) {
        case "ingredient": return { label: "選擇要排除的食材", key: "ingredients", idKey: "ingredientId" };
        case "dish":       return { label: "選擇要排除的菜餚", key: "dishes", idKey: "dishId" };
        case "restaurant": return { label: "選擇要排除的餐廳", key: "restaurants", idKey: "restaurantId" };
        default:           return { label: "選擇要排除的項目", key: "ingredients", idKey: "ingredientId" };
    }
}

// 當排除種類改變時，更新下拉選單與搜尋欄位
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

    // 更新 <select> 選項
    itemsSelect.innerHTML = options
        .map(item => `<option value="${item[meta.idKey]}">${item.name}</option>`)
        .join("");

    // 將選項存入隱藏元素，供搜尋 fallback 使用
    const store = document.getElementById('exclude-options');
    if (store) store.dataset.options = JSON.stringify(
        options.map(item => ({ id: item[meta.idKey], name: item.name }))
    );

    // 清空搜尋欄與已選取標籤
    const search = document.getElementById('exclude-search');
    const suggestions = document.getElementById('exclude-suggestions');
    const selectedList = document.getElementById('exclude-selected');
    if (search) search.value = '';
    if (suggestions) suggestions.innerHTML = '';
    if (selectedList) selectedList.innerHTML = '';
}

// 渲染搜尋建議下拉清單
function renderSuggestions(filtered) {
    const suggestions = document.getElementById('exclude-suggestions');
    if (!suggestions) return;
    if (!filtered.length) {
        suggestions.classList.add('hidden');
        suggestions.innerHTML = '';
        return;
    }
    suggestions.classList.remove('hidden');
    suggestions.innerHTML = filtered
        .map(opt => `<div class="suggestion-item" data-id="${opt.id}">${opt.name}</div>`)
        .join('');
    // 點擊建議項目時加入已選取清單
    suggestions.querySelectorAll('.suggestion-item').forEach(el => {
        el.addEventListener('click', () => {
            addExcludeSelected(el.dataset.id, el.textContent);
        });
    });
}

// 渲染已選取排除項目的 tag 清單
function renderExcludeSelected() {
    const container = document.getElementById('exclude-selected');
    if (!container) return;
    if (!state._excludeSelected.length) {
        container.innerHTML = '';
        return;
    }
    container.innerHTML = state._excludeSelected.map(s => `
        <span class="tag" data-id="${s.id}">
            ${s.name} <button class="tag-remove" data-id="${s.id}">×</button>
        </span>
    `).join('');
    // 點擊 × 移除該項目
    container.querySelectorAll('.tag-remove').forEach(btn => {
        btn.addEventListener('click', () => {
            state._excludeSelected = state._excludeSelected.filter(
                x => String(x.id) !== String(btn.dataset.id)
            );
            renderExcludeSelected();
        });
    });
}

// 新增一個排除項目到 tag 清單（避免重複）
function addExcludeSelected(id, name) {
    if (!id) return;
    if (state._excludeSelected.some(x => String(x.id) === String(id))) return;
    state._excludeSelected.push({ id, name });
    renderExcludeSelected();
    // 清空搜尋欄與建議清單
    const search = document.getElementById('exclude-search');
    const suggestions = document.getElementById('exclude-suggestions');
    if (search) search.value = '';
    if (suggestions) {
        suggestions.classList.add('hidden');
        suggestions.innerHTML = '';
    }
}

// 搜尋欄輸入事件：優先打後端搜尋 API，失敗時 fallback 到前端過濾
const searchInput = document.getElementById('exclude-search');
if (searchInput) {
    searchInput.addEventListener('input', (e) => {
        const type = document.getElementById('exclude-type').value;
        const q = (e.target.value || '').trim();
        if (!q) { renderSuggestions([]); return; }

        const apiMap = {
            ingredient: `/api/ingredient/search?q=${encodeURIComponent(q)}&limit=10`,
            dish:       `/api/dish/search?q=${encodeURIComponent(q)}&limit=10`,
            restaurant: `/api/restaurant/search?q=${encodeURIComponent(q)}&limit=10`
        };

        request(apiMap[type] || apiMap.ingredient).then(items => {
            // 統一格式化為 { id, name }
            const normalized = (items || []).map(it => {
                if (it.ingredientId) return { id: it.ingredientId, name: it.name };
                if (it.dishId)       return { id: it.dishId, name: it.name };
                if (it.restaurantId) return { id: it.restaurantId, name: it.name };
                return { id: it.id || it[Object.keys(it)[0]], name: it.name || String(it) };
            });
            renderSuggestions(normalized);
        }).catch(() => {
            // 後端搜尋失敗時，改用前端已快取的選項過濾
            const raw = document.getElementById('exclude-options');
            const options = JSON.parse(raw?.dataset?.options || '[]');
            const filtered = options
                .filter(o => o.name.toLowerCase().includes(q.toLowerCase()))
                .slice(0, 10);
            renderSuggestions(filtered);
        });
    });

    // 按 Enter 時加入第一個完全符合或部分符合的項目
    searchInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            const raw = document.getElementById('exclude-options');
            const options = JSON.parse(raw?.dataset?.options || '[]');
            const q = (searchInput.value || '').trim().toLowerCase();
            if (!q) return;
            const match = options.find(o => o.name.toLowerCase() === q)
                       || options.find(o => o.name.toLowerCase().includes(q));
            if (match) addExcludeSelected(match.id, match.name);
        }
    });
}

// 隱藏已排除項目區塊
function hideExclusions() {
    const section = document.getElementById("exclusion-section");
    if (section) section.classList.add("hidden");
}

// 顯示已排除項目區塊（先重新載入資料）
async function showExclusions() {
    await loadExclusions();
    const section = document.getElementById("exclusion-section");
    if (section) section.classList.remove("hidden");
}

// 將已選取的項目送出到後端，加入排除清單
async function addExclusion() {
    try {
        if (!state.token) throw new Error("請先登入");

        const type = document.getElementById("exclude-type").value;
        let selected = [];

        // 優先使用 tag 清單中的選取項目，其次用 <select> 的選取值
        if (state._excludeSelected && state._excludeSelected.length) {
            selected = state._excludeSelected;
        } else {
            const selectEl = document.getElementById('exclude-items');
            if (selectEl) selected = getSelectedValues(selectEl).map(id => ({ id }));
        }

        if (!selected.length) throw new Error("請至少選擇一個排除項目（可從下拉或搜尋選取）");

        let exclusionsAdded = 0;
        let existingCount = 0;
        let notFoundItems = [];

        if (type === "ingredient") {
            // 食材：一次批次送出（後端支援逗號分隔）
            const ingredients = selected
                .map(s => state.exclusionOptions.ingredients.find(
                    item => String(item.ingredientId) === String(s.id)
                ))
                .filter(Boolean)
                .map(item => item.name);

            const result = await request("/api/user/exclusion/ingredient", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ ingredients: ingredients.join(",") })
            });

            exclusionsAdded += result.added?.length || 0;
            existingCount += result.existing?.length || 0;
            if (result.notFound?.length > 0) notFoundItems.push(...result.notFound);

        } else if (type === "dish") {
            // 菜餚：逐筆送出
            for (const s of selected) {
                const dish = state.exclusionOptions.dishes.find(
                    d => String(d.dishId) === String(s.id)
                );
                if (dish) {
                    await request(`/api/user/exclusion/dish?dishId=${dish.dishId}`, { method: "POST" });
                    exclusionsAdded++;
                }
            }
        } else if (type === "restaurant") {
            // 餐廳：逐筆送出
            for (const s of selected) {
                const rest = state.exclusionOptions.restaurants.find(
                    r => String(r.restaurantId) === String(s.id)
                );
                if (rest) {
                    await request(`/api/user/exclusion/restaurant?restaurantId=${rest.restaurantId}`, { method: "POST" });
                    exclusionsAdded++;
                }
            }
        }

        // 顯示結果訊息
        if (exclusionsAdded > 0 || existingCount > 0 || notFoundItems.length > 0) {
            if (!document.getElementById("exclusion-section")?.classList.contains("hidden")) {
                await loadExclusions();
            }
            const parts = [];
            if (exclusionsAdded > 0) parts.push(`已加入 ${exclusionsAdded} 個排除項目`);
            if (existingCount > 0)   parts.push(`已有 ${existingCount} 個排除項目`);
            if (notFoundItems.length > 0) parts.push(`資料庫中找不到：${notFoundItems.join('、')}`);
            showToast(parts.join('，'), "success");
            // 清空 tag 清單
            state._excludeSelected = [];
            renderExcludeSelected();
        } else {
            throw new Error("沒有新增任何排除項目");
        }
    } catch (err) {
        showToast(err.message, "error");
    }
}

// 從後端載入使用者的排除清單，並格式化存入 state
async function loadExclusions() {
    try {
        const exclusions = await request("/api/user/exclusions");
        state.currentExclusions = exclusions;

        // 將後端資料格式化為統一的 { type, value, ...id } 結構
        state.userExclusionPreferences = exclusions.map(e => {
            if (e.ingredient?.name) return { type: 'ingredient', value: e.ingredient.name, ingredientId: e.ingredient.ingredientId };
            if (e.dish?.name)       return { type: 'dish',       value: e.dish.name,       dishId: e.dish.dishId };
            if (e.restaurant?.name) return { type: 'restaurant', value: e.restaurant.name, restaurantId: e.restaurant.restaurantId };
            return { type: 'other', value: `排除項目 ${e.exclusionId}` };
        });

        renderExclusions();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// 渲染已排除項目列表（含取消按鈕）
function renderExclusions() {
    const container = document.getElementById("exclusion-display");
    if (!state.userExclusionPreferences.length) {
        container.innerHTML = '<p style="color: #999;">無</p>';
        return;
    }

    container.innerHTML = state.userExclusionPreferences.map(pref => {
        const typeLabel = { ingredient: "食材", dish: "菜餚", restaurant: "餐廳" }[pref.type] || "其他";
        const targetId = pref.ingredientId || pref.dishId || pref.restaurantId || "";
        const btn = targetId
            ? `<button type="button" class="exclusion-remove" data-type="${pref.type}" data-id="${targetId}">取消</button>`
            : "";
        return `
            <div class="list-item exclusion-item">
                <div class="exclusion-item-info">
                    <span class="exclusion-tag">${typeLabel}</span>
                    <span>${pref.value}</span>
                </div>
                ${btn}
            </div>`;
    }).join("");

    // 綁定取消按鈕事件
    container.querySelectorAll(".exclusion-remove").forEach(button => {
        button.addEventListener("click", async () => {
            await removeExclusion(button.dataset.type, button.dataset.id);
        });
    });
}

// 從排除清單移除指定項目
async function removeExclusion(type, targetId) {
    try {
        if (!state.token) throw new Error("請先登入");
        if (!targetId) throw new Error("缺少排除項目編號");

        if (type === "ingredient") {
            await request(`/api/user/exclusion/ingredient/${targetId}`, { method: "DELETE" });
        } else if (type === "dish") {
            await request(`/api/user/exclusion/dish/${targetId}`, { method: "DELETE" });
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

// 取得個人化推薦並渲染
async function getRecommendations() {
    try {
        if (!state.token) throw new Error("請先登入");

        const recs = await request("/api/recommend/personal");
        const container = document.getElementById("recommend-list");

        if (!recs || !recs.length) {
            container.innerHTML = '<div class="empty-state"><div class="empty-state-icon">⭐</div>暫無推薦</div>';
            return;
        }

        container.innerHTML = recs.map(r => {
            const targetId = r.restaurantId || r.id;
            const score = Number(r.avgScore || 0).toFixed(1);

            return `
                <div class="list-item" style="
                    width: 100%;
                    box-sizing: border-box;
                    overflow: hidden;
                    display: block;
                ">
                    <div class="list-item-title" style="
                        white-space: normal;
                        word-break: break-word;
                    ">
                        ${r.name || '未命名餐廳'}
                    </div>

                    <div class="list-item-meta" style="
                        white-space: normal;
                        word-break: break-word;
                        line-height: 1.6;
                    ">
                        分類：<strong>${r.category || '未分類'}</strong> |
                        評分：⭐ ${score} |
                        ${r.priceRange || ''}
                    </div>

                    <div class="list-item-meta" style="
                        white-space: normal;
                        word-break: break-word;
                    ">
                        ${r.locationAt || ''}
                    </div>

                    <div class="button-group" style="margin-top: 12px;">
                        <button type="button" onclick="viewMenu(${targetId})">查看菜單</button>
                        <button type="button" onclick="viewReviews(${targetId})">查看評論</button>
                    </div>
                </div>`;
        }).join("");

    } catch (err) {
        showToast(err.message, "error");
    }
}

// ============ 揪團功能 ============

// 建立新群組並顯示邀請碼
async function createGroup() {
    try {
        if (!state.token) throw new Error("請先登入");
        const session = await request("/api/groups/create", { method: "POST" });

        document.getElementById("group-display").innerHTML = `
            <div class="list-item">
                <strong>群組已建立！</strong>
                <p style="margin-top: 10px; font-size: 14px;">
                    群組編號：<code style="background: #f0f0f0; padding: 2px 5px; border-radius: 3px;">${session.sessionId}</code>
                </p>
                <p style="margin-top: 8px; font-size: 14px;">
                    邀請碼：<code style="background: #f0f0f0; padding: 2px 5px; border-radius: 3px;">${session.inviteCode}</code>
                </p>
                <p style="margin-top: 12px; font-size: 12px; color: #999;">分享邀請碼給朋友加入</p>
            </div>`;
        showToast("群組已建立", "success");
        loadMyGroups();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// 加入群組表單送出
document.getElementById("join-group-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
        if (!state.token) throw new Error("請先登入");
        const body = formDataToJson(e.target);
        const session = await request(`/api/groups/join?inviteCode=${body.inviteCode}`, { method: "POST" });

        document.getElementById("group-display").innerHTML = `
            <div class="list-item">
                <strong>已加入群組</strong>
                <p style="margin-top: 10px; font-size: 14px;">
                    群組編號：${session.sessionId} | 邀請碼：${session.inviteCode}
                </p>
            </div>`;
        showToast("已加入群組", "success");
        e.target.reset();
        loadMyGroups();
    } catch (err) {
        showToast(err.message, "error");
    }
});

// 載入目前使用者參與的所有群組
async function loadMyGroups() {
    try {
        if (!state.token) throw new Error("請先登入");
        const groups = await request("/api/groups/my");
        renderMyGroups(groups);
    } catch (err) {
        showToast(err.message, "error");
    }
}

// 渲染我的群組列表（含建立日期、狀態、查看詳情按鈕）
function renderMyGroups(groups) {
    const container = document.getElementById("my-groups-list");
    if (!container) return;
    if (!groups.length) {
        container.innerHTML = '<div class="empty-state"><div class="empty-state-icon"></div>尚未加入群組</div>';
        return;
    }

    container.innerHTML = groups.map(group => {
        const createdAt = group.createdAt
            ? new Date(group.createdAt).toLocaleDateString('zh-TW')
            : '—';
        // 判斷目前使用者是否為建立者
        const isCreator = String(group.creator?.userId) === String(state.userId);
        return `
            <div class="list-item">
                <div class="list-item-title">群組 #${group.sessionId}</div>
                <div class="list-item-meta">
                    建立日期：${createdAt} ｜ 狀態：${group.status} ｜ 邀請碼：${group.inviteCode}
                    ${isCreator ? ' ｜ <span style="color: var(--primary); font-size:11px;">建立者</span>' : ''}
                </div>
                <div class="button-group" style="margin-top: 10px;">
                    <button type="button" class="btn-secondary" onclick="loadGroupDetail(${group.sessionId})">
                        查看詳情
                    </button>
                </div>
            </div>`;
    }).join('');
}

// ============ 群組詳情 ============

// 展開群組詳情面板：載入基本資訊、成員、推薦餐廳、用餐紀錄
async function loadGroupDetail(sessionId) {
    state._activeGroupSessionId = sessionId;
    state._selectedRestaurant = null;

    const panel = document.getElementById("group-detail-panel");
    const header = document.getElementById("group-detail-header");
    const recordFormPanel = document.getElementById("record-form-panel");

    panel.classList.remove("hidden");
    recordFormPanel.classList.add("hidden");
    header.textContent = `群組 #${sessionId} 詳情`;
    panel.scrollIntoView({ behavior: "smooth" });

    // 取得群組基本資訊（建立日期、狀態、建立者）
    try {
        const group = await request(`/api/groups/${sessionId}`);
        const createdAt = group.createdAt
            ? new Date(group.createdAt).toLocaleDateString('zh-TW')
            : '—';
        header.innerHTML = `
            群組 #${sessionId}
            <span style="font-size: 12px; font-weight: 400; color: var(--text-muted); margin-left: 8px;">
                ${createdAt} ｜ ${group.status}
            </span>`;

        // 只有建立者且群組狀態為「揪團中」才顯示結束按鈕
        const endBtnArea = document.getElementById("group-end-btn-area");
        const isCreator = String(group.creator?.userId) === String(state.userId);
        if (group.status === "揪團中" && isCreator) {
            endBtnArea.innerHTML = `
                <button type="button" class="btn-secondary"
                        style="width: 100%; border-color: #e57373; color: #e57373;"
                        onclick="endGroup(${sessionId})">
                    結束揪團
                </button>`;
        } else {
            endBtnArea.innerHTML = '';
        }
    } catch {
        // 群組基本資訊載入失敗不影響其他區塊
    }

    await loadGroupMembers(sessionId);
    await loadGroupRecommendations(sessionId);
    await loadGroupHistory(sessionId);
}

// 載入群組成員列表（顯示名稱，並標示「我」）
async function loadGroupMembers(sessionId) {
    const container = document.getElementById("group-detail-members");
    container.innerHTML = '<p style="color:#999; font-size:13px;">載入成員中…</p>';
    try {
        const members = await request(`/api/groups/${sessionId}/members`);
        if (!members || !members.length) {
            container.innerHTML = '<p style="color:#999; font-size:13px;">無成員資料</p>';
            return;
        }
        container.innerHTML = `
            <div style="font-size: 12px; font-weight: 600; text-transform: uppercase;
                        letter-spacing: 0.4px; color: var(--text-muted); margin-bottom: 6px;">成員</div>
            <div style="display: flex; flex-wrap: wrap; gap: 6px;">
                ${members.map(m => {
                    const label = m.name ? m.name : `用戶 #${m.userId || m}`;
                    const isMe = String(m.userId) === String(state.userId);
                    return `<span style="background: var(--surface); border: 1px solid var(--border);
                                        border-radius: 20px; padding: 3px 10px; font-size: 12px;
                                        ${isMe ? 'font-weight:600;' : ''}">
                                ${label}${isMe ? ' （我）' : ''}
                            </span>`;
                }).join('')}
            </div>`;
    } catch {
        container.innerHTML = '<p style="color:#999; font-size:13px;">無法載入成員</p>';
    }
}

// 載入群組推薦餐廳，每筆附帶 radio 可供選擇最終餐廳
async function loadGroupRecommendations(sessionId) {
    const container = document.getElementById("group-recommend-list");
    container.innerHTML = '<p style="color:#999; font-size:13px;">載入推薦中…</p>';

    try {
        const recs = await request(`/api/groups/${sessionId}/recommend`);

        if (!recs || !recs.length) {
            container.innerHTML = '<div class="empty-state">暫無推薦</div>';
            return;
        }

        container.innerHTML = recs.map(r => {
            const rid = r.restaurantId || r.id;
            const score = Number(r.avgScore || 0).toFixed(1);
            const safeName = (r.name || '').replace(/'/g, "\\'");

            return `
                <div class="list-item group-rec-card" id="group-rec-${rid}">
                    <button type="button"
                            class="pick-circle"
                            onclick="pickGroupRestaurant(${rid}, '${safeName}')">
                    </button>

                    <div class="list-item-title">${r.name || '未命名餐廳'}</div>

                    <div class="list-item-meta">
                        分類：<strong>${r.category || '未分類'}</strong> |
                        評分：⭐ ${score} |
                        ${r.priceRange || ''}
                    </div>

                    <div class="list-item-meta">
                        ${r.locationAt || ''}
                    </div>

                    <div class="button-group" style="margin-top: 16px;">
                        <button type="button" onclick="viewMenu(${rid})">查看菜單</button>
                        <button type="button" onclick="viewReviews(${rid})">查看評論</button>
                    </div>
                </div>
            `;
        }).join("");

    } catch (err) {
        container.innerHTML = '<p style="color:red; font-size:13px;">無法載入推薦</p>';
    }
}

function pickGroupRestaurant(restaurantId, name) {
    state._selectedRestaurant = { restaurantId, name };

    document.querySelectorAll(".pick-circle").forEach(btn => {
        btn.classList.remove("selected");
    });

    const card = document.getElementById(`group-rec-${restaurantId}`);
    const btn = card?.querySelector(".pick-circle");
    if (btn) btn.classList.add("selected");

    const panel = document.getElementById("record-form-panel");
    document.getElementById("record-selected-restaurant").textContent = `已選擇：${name}`;
    document.getElementById("record-visit-date").value = new Date().toISOString().split("T")[0];
    document.getElementById("record-meal-name").value = "";
    document.getElementById("record-note").value = "";
    panel.classList.remove("hidden");
}

// 使用者勾選推薦餐廳後，顯示用餐紀錄表單
function onRestaurantPicked(restaurantId, name) {
    state._selectedRestaurant = { restaurantId, name };

    const panel = document.getElementById("record-form-panel");
    document.getElementById("record-selected-restaurant").textContent = `已選擇：${name}`;
    // 預設帶入今天日期，使用者仍可修改
    document.getElementById("record-visit-date").value = new Date().toISOString().split('T')[0];
    document.getElementById("record-meal-name").value = '';
    document.getElementById("record-note").value = '';
    panel.classList.remove("hidden");
    panel.scrollIntoView({ behavior: "smooth" });
}

// 取消建立紀錄，隱藏表單並清除 radio 選取
function cancelRecord() {
    document.getElementById("record-form-panel").classList.add("hidden");
    state._selectedRestaurant = null;
    document.querySelectorAll('input[name="group-restaurant-pick"]')
            .forEach(r => r.checked = false);
}

// 送出用餐紀錄到後端
async function saveGroupRecord() {
    try {
        if (!state.token) throw new Error("請先登入");
        if (!state._selectedRestaurant) throw new Error("請先選擇餐廳");

        const mealName = document.getElementById("record-meal-name").value.trim();
        const visitDate = document.getElementById("record-visit-date").value;
        const note = document.getElementById("record-note").value.trim();

        if (!mealName) throw new Error("請輸入餐點名稱");
        if (!visitDate) throw new Error("請選擇日期");

        await request("/api/history/save", {
            method: "POST",
            body: JSON.stringify({
                restaurantId: state._selectedRestaurant.restaurantId,
                mealName,
                visitDate,
                note: note || null
            })
        });

        showToast("用餐紀錄已儲存", "success");

        // 清空表單並隱藏
        document.getElementById("record-form-panel").classList.add("hidden");
        state._selectedRestaurant = null;
        document.querySelectorAll('input[name="group-restaurant-pick"]')
                .forEach(r => r.checked = false);

        // 重新載入紀錄區塊
        await loadGroupHistory(state._activeGroupSessionId);
    } catch (err) {
        showToast(err.message, "error");
    }
}

// 載入並渲染使用者的用餐紀錄（顯示於群組詳情下方）
// 目前使用 /api/history/me 撈全部個人紀錄
// 若後端未來新增 /api/history/group/{sessionId} 可改為只撈該群組的紀錄
async function loadGroupHistory(sessionId) {
    const container = document.getElementById("group-history-list");
    container.innerHTML = '<p style="color:#999; font-size:13px;">載入紀錄中…</p>';
    try {
        const history = await request("/api/history/me");
        if (!history || !history.length) {
            container.innerHTML = '<p style="color:#999; font-size:13px;">尚無紀錄</p>';
            return;
        }
        container.innerHTML = history.map(h => `
            <div class="list-item">
                <div class="list-item-title">${h.mealName || '未命名'}</div>
                <div class="list-item-meta">
                    餐廳：${h.restaurant?.name || `#${h.restaurantId}`} ｜ ${h.visitDate}
                </div>
                ${h.note ? `<div class="list-item-meta">${h.note}</div>` : ''}
            </div>`).join('');
    } catch {
        container.innerHTML = '<p style="color:#999; font-size:13px;">無法載入紀錄</p>';
    }
}

// 結束揪團（僅建立者可操作）
async function endGroup(sessionId) {
    try {
        if (!state.token) throw new Error("請先登入");
        await request(`/api/groups/${sessionId}/end`, { method: "POST" });
        showToast("揪團已結束", "success");
        // 重新整理詳情面板與群組列表
        await loadGroupDetail(sessionId);
        await loadMyGroups();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// ============ 彈窗（Modal）============

// 關閉指定 modal
window.closeModal = function(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.style.display = "none";
};

// 查看餐廳菜單
window.viewMenu = async function(restaurantId) {
    const modal = document.getElementById("menu-modal");
    if (modal) modal.style.display = "block";
    const container = document.getElementById("menu-container");
    if (!container) return;
    container.innerHTML = "<p style='color:#666;'>載入中...</p>";
    try {
        const dishes = await request(`/api/dish/restaurant/${restaurantId}`);
        container.innerHTML = dishes && dishes.length
            ? dishes.map(dish =>
                `<div style="margin-bottom: 8px; padding-bottom: 8px; border-bottom: 1px dashed #eee;">
                    <strong>${dish.name}</strong>
                 </div>`).join('')
            : "<p style='color:#999;'>這間餐廳目前沒有建立菜單喔！</p>";
    } catch (error) {
        container.innerHTML = "<p style='color:red;'>無法載入菜單，請確認伺服器連線或 API 路徑是否正確。</p>";
        console.error(error);
    }
};

// 查看餐廳評論
window.viewReviews = async function(restaurantId) {
    const modal = document.getElementById("review-modal");
    if (modal) modal.style.display = "block";
    const container = document.getElementById("review-container");
    if (!container) return;
    container.innerHTML = "<p style='color:#666;'>載入中...</p>";
    try {
        const reviews = await request(`/api/restaurant/${restaurantId}/ratings`);
        container.innerHTML = reviews && reviews.length
            ? reviews.map(r =>
                `<div style="margin-bottom: 12px; border-bottom: 1px solid #eee; padding-bottom: 8px;">
                    <p style="margin: 0;">⭐ <strong>${r.score} / 5</strong></p>
                    <p style="margin: 5px 0 0 0; color: #555;">${r.comment || '無文字評論'}</p>
                 </div>`).join('')
            : "<p style='color:#999;'>這間餐廳目前還沒有人留下評論喔！</p>";
    } catch (error) {
        container.innerHTML = "<p style='color:red;'>無法載入評論，請確認伺服器連線或 API 路徑是否正確。</p>";
        console.error(error);
    }
};

// ============ 初始化 ============

updateSession();

// 排除種類切換時更新下拉清單
const excludeTypeSelect = document.getElementById("exclude-type");
if (excludeTypeSelect) {
    excludeTypeSelect.addEventListener("change", updateExclusionItemList);
}

// 若已有登入狀態（localStorage 有 token），直接進入主選單
if (state.token && state.userName) {
    goPage("menu");
}