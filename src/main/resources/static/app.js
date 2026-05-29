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
    _selectedRestaurant: null,    // 群組推薦中已選取的餐廳
    selectedDishes: [],
    currentGroupSessionId: null,
    currentGroupSelectedDishes: [], // 供雙模式菜單使用
    currentGroupRestaurantId: null  // 供雙模式菜單使用
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
        const targetId = r.restaurantId || r.id;
        return `
        <div class="list-item">
            <div class="list-item-title">${r.name}</div>
            <div class="list-item-meta">
                <strong>${r.category}</strong> | ${r.priceRange} | ⭐ ${r.avgScore} (${r.ratingCount}人)
            </div>
            <div class="list-item-meta">${r.locationAt}</div>
            <div class="button-group" style="margin-top: 12px;">
                <button type="button" style="padding: 4px 8px; margin-right: 5px; cursor: pointer; border-radius: 4px; border: 1px solid #ccc; background: #f9f9f9;" onclick="viewMenu(${targetId}, 'view')">查看菜單</button>
                <button type="button" style="padding: 4px 8px; cursor: pointer; border-radius: 4px; border: 1px solid #ccc; background: #f9f9f9;" onclick="viewReviews(${targetId})">查看評論</button>
            </div>
        </div>`;
    }).join("");
}

// 載入評分頁的餐廳下拉選單
async function loadRestaurantSelects() {
    try {
        state.restaurants = await request("/api/restaurant/all");
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
                        <button type="button" style="padding: 4px 8px; margin-right: 5px; cursor: pointer; border-radius: 4px; border: 1px solid #ccc; background: #f9f9f9;" onclick="viewMenu(${targetId}, 'view')">查看菜單</button>
                        <button type="button" style="padding: 4px 8px; cursor: pointer; border-radius: 4px; border: 1px solid #ccc; background: #f9f9f9;" onclick="viewReviews(${targetId})">查看評論</button>
                    </div>
                </div>`;
        }).join("");

    } catch (err) {
        showToast(err.message, "error");
    }
}

// ============ 我的評論功能 ============
// 綁定「我的評論」按鈕點擊事件
document.getElementById("my-reviews-btn")?.addEventListener("click", async () => {
    try {
        if (!state.token) throw new Error("請先登入");
        
        // ⚠️ 修正 1：網址改為 /api/restaurant/reviews/me (去掉 s)
        const response = await request("/api/restaurant/reviews/me", { method: "GET" });
        
        // ⚠️ 修正 2：因為後端是用 ApiResponse 回傳，真正的陣列放在 data 裡面
        const myReviews = response.data || response; 
        
        renderMyReviews(myReviews);
        document.getElementById("my-reviews-modal").style.display = "block";
    } catch (error) {
        alert("無法載入評論：" + error.message);
    }
});

// 動態渲染我的評論列表
function renderMyReviews(reviews) {
    const container = document.getElementById("my-reviews-list");
    if (!reviews || reviews.length === 0) {
        container.innerHTML = "<p style='text-align:center;color:var(--text-muted);padding:20px 0;'>你還沒有寫過任何評論喔！</p>";
        return;
    }

    container.innerHTML = reviews.map(review => {
        const dateStr = review.createdAt ? new Date(review.createdAt).toLocaleDateString() : "未知日期";
        const stars = "⭐".repeat(review.rating);
        const editedTag = review.isEdited ? `<span style="font-size:11px;color:var(--text-muted);">（已編輯）</span>` : "";
        return `
            <div class="list-item" id="review-item-${review.id}">
                <div class="list-item-title">${review.restaurantName || '未知餐廳'}</div>
                <div class="list-item-meta">${stars} ${review.rating} 分 ・ ${dateStr} ${editedTag}</div>
                <div class="list-item-meta" style="margin-top:4px;" id="review-comment-${review.id}">${review.comment || '(無評論內容)'}</div>
                <div id="review-edit-form-${review.id}" style="display:none;margin-top:10px;">
                    <select id="review-edit-score-${review.id}" style="width:100%;margin-bottom:8px;padding:8px;border:1px solid var(--border);border-radius:var(--radius-sm);font-size:13px;">
                        <option value="5" ${review.rating===5?'selected':''}>⭐⭐⭐⭐⭐ 非常好</option>
                        <option value="4" ${review.rating===4?'selected':''}>⭐⭐⭐⭐ 很好</option>
                        <option value="3" ${review.rating===3?'selected':''}>⭐⭐⭐ 普通</option>
                        <option value="2" ${review.rating===2?'selected':''}>⭐⭐ 不太好</option>
                        <option value="1" ${review.rating===1?'selected':''}>⭐ 很差</option>
                    </select>
                    <textarea id="review-edit-comment-${review.id}" style="width:100%;padding:8px;border:1px solid var(--border);border-radius:var(--radius-sm);font-size:13px;resize:vertical;min-height:60px;">${review.comment || ''}</textarea>
                    <div style="display:flex;gap:8px;margin-top:8px;">
                        <button class="btn-primary" style="flex:1;" onclick="submitEditReview(${review.id})">儲存</button>
                        <button class="btn-back" onclick="cancelEditReview(${review.id})">取消</button>
                    </div>
                </div>
                <div id="review-actions-${review.id}" style="display:flex;gap:8px;margin-top:10px;">
                    <button class="btn-warning" onclick="showEditReview(${review.id})">編輯</button>
                    <button class="btn-danger" style="flex:0 0 auto;" onclick="deleteReview(${review.id})">刪除</button>
                </div>
            </div>
        `;
    }).join("");
}

window.showEditReview = function(id) {
    document.getElementById(`review-edit-form-${id}`).style.display = "block";
    document.getElementById(`review-actions-${id}`).style.display = "none";
};

window.cancelEditReview = function(id) {
    document.getElementById(`review-edit-form-${id}`).style.display = "none";
    document.getElementById(`review-actions-${id}`).style.display = "flex";
};

window.submitEditReview = async function(id) {
    const score = parseInt(document.getElementById(`review-edit-score-${id}`).value);
    const comment = document.getElementById(`review-edit-comment-${id}`).value;
    try {
        await request(`/api/restaurant/reviews/${id}`, {
            method: "PUT",
            body: JSON.stringify({ score, comment })
        });
        showToast("評論已更新", "success");
        const response = await request("/api/restaurant/reviews/me", { method: "GET" });
        renderMyReviews(response.data || response);
    } catch (e) {
        showToast("編輯失敗：" + e.message, "error");
    }
};

// 刪除評論前端觸發邏輯
window.deleteReview = async function(reviewId) {
    if (!confirm("確定要刪除這則評論嗎？")) return;

    try {
        await request(`/api/restaurant/reviews/${reviewId}`, { method: "DELETE" });
        showToast("評論已刪除", "success");
        const response = await request("/api/restaurant/reviews/me", { method: "GET" });
        const myReviews = response.data || response;
        renderMyReviews(myReviews);
    } catch (error) {
        showToast("刪除失敗：" + error.message, "error");
    }
};

// ============ 揪團功能 ============

function showCreateGroupInput() {
    document.getElementById('btn-show-create-group').classList.add('hidden');
    document.getElementById('create-group-panel').classList.remove('hidden');
    document.getElementById('new-group-name').value = ''; 
    document.getElementById('new-group-name').focus();
}

function hideCreateGroupInput() {
    document.getElementById('btn-show-create-group').classList.remove('hidden');
    document.getElementById('create-group-panel').classList.add('hidden');
}

async function submitCreateGroup() {
    try {
        if (!state.token) throw new Error("請先登入");

        const groupName = document.getElementById('new-group-name').value.trim();
        
        // 將名稱傳給後端
        const bodyData = groupName ? { groupName: groupName } : {};

        const session = await request("/api/groups/create", { 
            method: "POST",
            body: JSON.stringify(bodyData)
        });

        const displayName = session.groupName ? session.groupName : `群組 #${session.sessionId}`;

        document.getElementById("group-display").innerHTML = `
            <div class="list-item">
                <strong>群組「${displayName}」已建立！</strong>
                <p style="margin-top: 10px; font-size: 14px;">
                    邀請碼：<code>${session.inviteCode}</code>
                </p>
                <p style="margin-top: 12px; font-size: 12px; color: #999;">
                    分享邀請碼給朋友加入
                </p>
            </div>`;

        showToast("群組已建立", "success");
        hideCreateGroupInput(); // 成功後收回面板
        loadMyGroups(); // 重新載入群組列表
    } catch (err) {
        showToast(err.message, "error");
    }
}

// 加入群組的表單送出事件
document.getElementById("join-group-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
        if (!state.token) throw new Error("請先登入");

        const body = formDataToJson(e.target);
        const session = await request(`/api/groups/join?inviteCode=${body.inviteCode}`, {
            method: "POST"
        });

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


async function loadMyGroups() {
    try {
        if (!state.token) throw new Error("請先登入");
        const groups = await request("/api/groups/my");
        renderMyGroups(groups);
    } catch (err) {
        showToast(err.message, "error");
    }
}

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

        const isCreator = String(group.creator?.userId) === String(state.userId);
        const displayName = group.groupName ? group.groupName : `群組 #${group.sessionId}`;
        const idLine = group.groupName
            ? `<div class="list-item-meta" style="margin-top:2px;">群組 #${group.sessionId}</div>`
            : '';

        const titleHtml = isCreator
            ? `<div style="display:flex;align-items:center;gap:6px;">
                   <span id="rename-label-${group.sessionId}" class="list-item-title"
                         style="cursor:pointer;"
                         onclick="showRenameGroup(${group.sessionId})">${displayName}</span>
                   <div id="rename-form-${group.sessionId}" style="display:none;flex:1;display:none;align-items:center;gap:6px;">
                       <input type="text" id="rename-input-${group.sessionId}" value="${group.groupName || ''}"
                           placeholder="輸入群組名稱"
                           style="flex:1;padding:5px 8px;border:1px solid var(--border);border-radius:var(--radius-sm);font-size:13px;font-weight:600;">
                       <button class="btn-primary" style="flex:0 0 auto;padding:5px 12px;" onclick="submitRenameGroup(${group.sessionId})">確認</button>
                       <button class="btn-back" style="flex:0 0 auto;padding:5px 10px;" onclick="cancelRenameGroup(${group.sessionId})">✕</button>
                   </div>
               </div>`
            : `<div class="list-item-title">${displayName}</div>`;

        return `
            <div class="list-item" id="group-list-item-${group.sessionId}">
                ${titleHtml}
                ${idLine}
                <div class="list-item-meta">
                    建立日期：${createdAt} ｜ 邀請碼：${group.inviteCode}
                    ${isCreator ? ' ｜ <span style="font-size:11px;">建立者</span>' : ''}
                </div>
                <div class="button-group" style="margin-top:10px;">
                    <button type="button" class="btn-secondary" onclick="loadGroupDetail(${group.sessionId})">查看詳情</button>
                    ${isCreator ? `
                        <button type="button" class="btn-danger" onclick="deleteGroup(${group.sessionId})">刪除</button>
                    ` : `
                        <button type="button" class="btn-danger" onclick="leaveGroup(${group.sessionId})">退出</button>
                    `}
                </div>
            </div>`;
    }).join('');
}

let _activeRenameSessionId = null;
let _renameOutsideHandler = null;

window.showRenameGroup = function(sessionId) {
    if (_activeRenameSessionId !== null && _activeRenameSessionId !== sessionId) {
        cancelRenameGroup(_activeRenameSessionId);
    }

    _activeRenameSessionId = sessionId;
    document.getElementById(`rename-label-${sessionId}`).style.display = "none";
    const form = document.getElementById(`rename-form-${sessionId}`);
    form.style.display = "flex";
    document.getElementById(`rename-input-${sessionId}`).focus();

    if (_renameOutsideHandler) document.removeEventListener("click", _renameOutsideHandler);
    _renameOutsideHandler = function(e) {
        if (!form.contains(e.target)) {
            cancelRenameGroup(sessionId);
        }
    };
    setTimeout(() => document.addEventListener("click", _renameOutsideHandler), 0);
};

window.cancelRenameGroup = function(sessionId) {
    document.getElementById(`rename-form-${sessionId}`).style.display = "none";
    document.getElementById(`rename-label-${sessionId}`).style.display = "";
    if (_renameOutsideHandler) {
        document.removeEventListener("click", _renameOutsideHandler);
        _renameOutsideHandler = null;
    }
    _activeRenameSessionId = null;
};

window.submitRenameGroup = async function(sessionId) {
    const name = document.getElementById(`rename-input-${sessionId}`).value.trim();
    try {
        await request(`/api/groups/${sessionId}/name`, {
            method: "PATCH",
            body: JSON.stringify({ name })
        });
        showToast("群組已改名", "success");
        loadMyGroups();
    } catch (e) {
        showToast("改名失敗：" + e.message, "error");
    }
};

// ============ 群組詳情 ============

async function loadGroupDetail(sessionId) {
    console.log("loadGroupDetail clicked:", sessionId);

    state._activeGroupSessionId = sessionId;
    state._selectedRestaurant = null;
    state.selectedDishes = [];

    const panel = document.getElementById("group-detail-panel");
    const header = document.getElementById("group-detail-header");
    const recordFormPanel = document.getElementById("record-form-panel");

    if (!panel) {
        console.error("找不到 group-detail-panel");
        showToast("找不到群組詳情區塊", "error");
        return;
    }

    if (!header) {
        console.error("找不到 group-detail-header");
        showToast("找不到群組標題區塊", "error");
        return;
    }

    panel.classList.remove("hidden");

    if (recordFormPanel) {
        recordFormPanel.classList.add("hidden");
    }

    header.textContent = `群組 #${sessionId} 詳情`;
    panel.scrollIntoView({ behavior: "smooth" });

    try {
        const group = await request(`/api/groups/${sessionId}`);
        const createdAt = group.createdAt
            ? new Date(group.createdAt).toLocaleDateString('zh-TW')
            : '—';

        const displayName = group.groupName ? group.groupName : `群組 #${sessionId}`;
        const idLabel = group.groupName
            ? `<div style="font-size:11px;font-weight:400;color:var(--text-muted);margin-top:2px;">群組 #${sessionId} ・ ${createdAt}</div>`
            : `<div style="font-size:11px;font-weight:400;color:var(--text-muted);margin-top:2px;">${createdAt}</div>`;

        header.innerHTML = `<div style="font-size:15px;font-weight:600;color:var(--text-primary);">${displayName}</div>${idLabel}`;

        const endBtnArea = document.getElementById("group-end-btn-area");
        if (endBtnArea) endBtnArea.innerHTML = "";

    } catch (err) {
        console.error("群組基本資料載入失敗:", err);
    }

    await loadGroupMembers(sessionId);
    await loadGroupRecommendations(sessionId);
    await loadGroupHistory(sessionId);
}

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
                        letter-spacing: 0.4px; color: var(--text-muted); margin-bottom: 6px;">
                成員
            </div>
            <div style="display: flex; flex-wrap: wrap; gap: 6px;">
                ${members.map(m => {
                    const label = m.name ? m.name : `用戶 #${m.userId || m}`;
                    const isMe = String(m.userId) === String(state.userId);

                    return `
                        <span style="background: var(--surface); border: 1px solid var(--border);
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
            const safeName = String(r.name || '').replace(/'/g, "\\'");

            return `
                <div class="list-item group-rec-card" id="group-rec-${rid}">
                    <div class="list-item-title">${r.name || '未命名餐廳'}</div>

                    <div class="list-item-meta">
                        評分：⭐ ${score} |
                        ${r.priceRange || ''}
                    </div>

                    <div class="list-item-meta">
                        ${r.locationAt || ''}
                    </div>

                    <div class="button-group" style="margin-top: 16px;">
                        <button type="button" class="btn-secondary" style="margin-right: 8px;" onclick="pickGroupRestaurant(${rid}, '${safeName}'); viewMenu(${rid}, 'select')">
                            選擇餐點
                        </button>
                        <button type="button" class="btn-secondary" onclick="viewReviews(${rid})">
                            查看評論
                        </button>
                    </div>
                </div>`;
        }).join("");

    } catch {
        container.innerHTML = '<p style="color:red; font-size:13px;">無法載入推薦</p>';
    }
}

function pickGroupRestaurant(restaurantId, name) {
    state._selectedRestaurant = { restaurantId, name };
    state.selectedDishes = [];
}

async function loadGroupHistory(sessionId) {
    const container = document.getElementById("group-history-list");
    container.innerHTML = '<p style="color:#999; font-size:13px;">載入紀錄中…</p>';

    try {
        const history = await request(`/api/history/group/${sessionId}`);

        if (!history || !history.length) {
            container.innerHTML = '<p style="color:#999; font-size:13px;">尚無紀錄</p>';
            return;
        }

        container.innerHTML = history.map(h => {
            const dt = h.visitDate ? new Date(h.visitDate) : null;
            const pad = n => String(n).padStart(2, '0');
            const dateStr = dt
                ? `${dt.getFullYear()}/${pad(dt.getMonth()+1)}/${pad(dt.getDate())} ${pad(dt.getHours())}:${pad(dt.getMinutes())}`
                : '';
            const participants = (h.participants && h.participants.length)
                ? h.participants.map(p => p.name).join('、')
                : '';
            return `
            <div class="list-item">
                <div class="list-item-title">${h.mealName || '未命名'}</div>
                <div class="list-item-meta">
                    餐廳：${h.restaurant?.name || `#${h.restaurantId}`} ｜ ${dateStr}
                </div>
                ${participants ? `<div class="list-item-meta">成員： ${participants}</div>` : ''}
                ${h.note ? `<div class="list-item-meta">小記： ${h.note}</div>` : ''}
                <div style="margin-top:10px;">
                    <button type="button" class="btn-danger"
                            style="padding:4px 12px;font-size:12px;border-radius:6px;"
                            onclick="deleteHistoryRecord(${h.recordId})">
                        刪除紀錄
                    </button>
                </div>
            </div>`;
        }).join('');
    } catch {
        container.innerHTML = '<p style="color:#999; font-size:13px;">無法載入紀錄</p>';
    }
}

window.deleteHistoryRecord = async function(recordId) {
    if (!confirm("確定要刪除這筆紀錄嗎？")) return;
    try {
        await request(`/api/history/${recordId}`, { method: "DELETE" });
        showToast("紀錄已刪除", "success");
        if (state._activeGroupSessionId) {
            await loadGroupHistory(state._activeGroupSessionId);
        }
    } catch (err) {
        showToast("刪除失敗：" + err.message, "error");
    }
};

async function leaveGroup(sessionId) {
    try {
        if (!confirm("確定要退出這個群組嗎？")) return;

        await request(`/api/groups/${sessionId}/leave`, { method: "DELETE" });

        showToast("已退出群組", "success");
        document.getElementById("group-detail-panel")?.classList.add("hidden");
        loadMyGroups();
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function deleteGroup(sessionId) {
    try {
        if (!confirm("確定要刪除這個群組嗎？")) return;

        await request(`/api/groups/${sessionId}`, {
            method: "DELETE"
        });

        showToast("群組已刪除", "success");

        document.getElementById("group-detail-panel")?.classList.add("hidden");
        loadMyGroups();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// ============ 彈窗（Modal）============

window.closeModal = function(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.style.display = "none";
};

// ============ 支援雙模式的查看/選擇菜單邏輯 ============
window.viewMenu = async function(restaurantId, mode = 'view') {
    const modal = document.getElementById("menu-modal");
    if (modal) modal.style.display = "block";
    
    const container = document.getElementById("menu-container");
    if (!container) return;
    
    container.innerHTML = "<p style='color:#666;'>載入中...</p>";

    try {
        console.log(`正在以 [${mode}] 模式查詢餐廳 ID:`, restaurantId); 
        const dishes = await request(`/api/dish/restaurant/${restaurantId}`); 
        console.log("後端回傳的菜單資料:", dishes); 
        
        if (!dishes || dishes.length === 0) {
            container.innerHTML = "<p style='color:#999;'>這間餐廳目前沒有建立菜單喔！</p>";
            return;
        }

        // 如果是揪團選擇模式，初始化這間餐廳的已選餐點陣列（暫存於 state 裡）
        if (mode === 'select') {
            state.currentGroupSelectedDishes = [];
            state.currentGroupDishQuantities = {};
            state.currentGroupRestaurantId = restaurantId;
            state.currentGroupDishesData = dishes;
            state.currentGroupTotalPrice = 0;
        }

        let html = dishes.map(dish => {
            const dishId = dish.dishId;
            const safeName = dish.name.replace(/'/g, "\\'");
            // 漂亮的價格標籤
            const priceHtml = `<span style="color: #e44d26; font-weight: bold; margin-left: auto; margin-right: 15px;">$${dish.price || 0}</span>`;

            if (mode === 'select') {
                return `
                <div class="dish-select-item" id="dish-item-${dishId}"
                     onclick="toggleDishSelectionLocal(${dishId}, '${safeName}')"
                     style="display:flex;align-items:center;justify-content:space-between;margin-bottom:8px;cursor:pointer;">
                    <div style="font-weight:500;">${dish.name}</div>
                    <div style="display:flex;align-items:center;gap:8px;">
                        <span style="color:#e44d26;font-weight:bold;">$${dish.price || 0}</span>
                        <div class="dish-qty-control hidden" id="qty-${dishId}">
                            <button class="dish-qty-btn" onclick="changeDishQty(${dishId},-1);event.stopPropagation()">−</button>
                            <span id="qty-val-${dishId}">1</span>
                            <button class="dish-qty-btn" onclick="changeDishQty(${dishId},1);event.stopPropagation()">+</button>
                        </div>
                        <div class="dish-check-circle" id="check-${dishId}">✓</div>
                    </div>
                </div>`;
            } else {
                // 首頁純瀏覽模式：乾淨的純文字排版，無法點擊
                return `
                <div style="margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px dashed #eee; display: flex; justify-content: space-between; align-items: center;">
                   <span style="font-weight: 500; color: #333;">${dish.name}</span>
                   <span style="color: #e44d26; font-weight: bold;">$${dish.price || 0}</span>
                </div>`;
            }
        }).join('');

        container.innerHTML = html;

        // 揪團模式：把確認按鈕放到 menu-confirm-area（彈窗底部固定區）
        const confirmArea = document.getElementById("menu-confirm-area");
        if (confirmArea) {
            if (mode === 'select') {
                confirmArea.innerHTML = `
                <div style="padding-top: 12px; border-top: 2px solid #eee;">
                    <button type="button" id="confirm-dishes-btn" class="btn-primary"
                            style="width:100%;padding:12px;font-size:15px;font-weight:bold;border-radius:8px;cursor:pointer;"
                            onclick="submitGroupDishesSelection()">
                        確認選擇（已選 <span id="selected-dishes-count">0</span> 道）｜ 合計 $<span id="selected-dishes-total">0</span>
                    </button>
                </div>`;
            } else {
                confirmArea.innerHTML = '';
            }
        }
    } catch (error) {
        container.innerHTML = "<p style='color:red;'>無法載入菜單，請確認伺服器連線。</p>";
        console.error(error);
    }
}

// 揪團模式：切換選取 / 取消
window.toggleDishSelectionLocal = function(dishId, safeName) {
    if (!state.currentGroupSelectedDishes) state.currentGroupSelectedDishes = [];
    if (!state.currentGroupDishQuantities) state.currentGroupDishQuantities = {};

    const index = state.currentGroupSelectedDishes.indexOf(dishId);
    const element = document.getElementById(`dish-item-${dishId}`);
    const qtyControl = document.getElementById(`qty-${dishId}`);

    if (index > -1) {
        state.currentGroupSelectedDishes.splice(index, 1);
        delete state.currentGroupDishQuantities[dishId];
        element?.classList.remove('selected');
        qtyControl?.classList.add('hidden');
    } else {
        state.currentGroupSelectedDishes.push(dishId);
        state.currentGroupDishQuantities[dishId] = 1;
        element?.classList.add('selected');
        if (qtyControl) {
            qtyControl.classList.remove('hidden');
            const v = document.getElementById(`qty-val-${dishId}`);
            if (v) v.textContent = 1;
        }
    }
    updateDishSelectionSummary();
}

// 調整份數
window.changeDishQty = function(dishId, delta) {
    if (!state.currentGroupDishQuantities) state.currentGroupDishQuantities = {};
    const current = state.currentGroupDishQuantities[dishId] || 0;
    const next = current + delta;

    if (next <= 0) {
        toggleDishSelectionLocal(dishId, '');
        return;
    }

    state.currentGroupDishQuantities[dishId] = next;
    const v = document.getElementById(`qty-val-${dishId}`);
    if (v) v.textContent = next;
    updateDishSelectionSummary();
}

// 更新確認按鈕的道數 + 總金額
function updateDishSelectionSummary() {
    const dishes = state.currentGroupDishesData || [];
    const qtys = state.currentGroupDishQuantities || {};

    const totalQty = Object.values(qtys).reduce((a, b) => a + b, 0);
    const total = state.currentGroupSelectedDishes.reduce((sum, id) => {
        const dish = dishes.find(d => d.dishId === id);
        return sum + (dish ? (dish.price || 0) * (qtys[id] || 1) : 0);
    }, 0);

    state.currentGroupTotalPrice = total;

    const countSpan = document.getElementById("selected-dishes-count");
    if (countSpan) countSpan.textContent = totalQty;
    const totalSpan = document.getElementById("selected-dishes-total");
    if (totalSpan) totalSpan.textContent = total;
}

// ============ 確保更新顯示邏輯已在全域範圍 ============
window.updateSelectedDishNames = function() {
    const el = document.getElementById("selected-dish-names");
    if (!el) {
        console.warn("找不到 ID 為 selected-dish-names 的 HTML 元素");
        return;
    }

    if (!state.selectedDishes || state.selectedDishes.length === 0) {
        el.textContent = "尚未選擇餐點";
        return;
    }

    el.textContent = state.selectedDishes.map(d => d.name).join("、");
};

// 按下最底下確認按鈕時執行的邏輯
// ============ 按下最底下確認按鈕時執行的邏輯 ============
window.submitGroupDishesSelection = async function() {
    const restaurantId = state.currentGroupRestaurantId;
    const selectedDishIds = state.currentGroupSelectedDishes || [];

    if (selectedDishIds.length === 0) {
        showToast("請至少選擇一道菜！", "error");
        return;
    }

    try {
        const allDishes = await request(`/api/dish/restaurant/${restaurantId}`);
        state.selectedDishes = allDishes.filter(d => selectedDishIds.includes(d.dishId));
        
        // 1. 強制更新 HTML 文字（含數量）
        const el = document.getElementById("selected-dish-names");
        if (el) {
            const qtys = state.currentGroupDishQuantities || {};
            el.textContent = state.selectedDishes.map(d => {
                const qty = qtys[d.dishId] || 1;
                return `${d.name} × ${qty}`;
            }).join("、");
        }

        // 2. 更新已選餐廳名稱顯示
        const restaurantEl = document.getElementById("record-selected-restaurant");
        if (restaurantEl && state._selectedRestaurant) {
            restaurantEl.textContent = `餐廳：${state._selectedRestaurant.name}`;
        }

        // 2b. 顯示總金額
        const totalPriceEl = document.getElementById("record-total-price");
        if (totalPriceEl && state.currentGroupTotalPrice > 0) {
            totalPriceEl.textContent = `合計 $${state.currentGroupTotalPrice}`;
        }

        // 3. 顯示記錄表單
        const recordPanel = document.getElementById("record-form-panel");
        if (recordPanel) {
            recordPanel.classList.remove("hidden");
        }

        // 4. 載入群組成員產生勾選清單
        await renderParticipantCheckboxes(state._activeGroupSessionId);

        showToast(`已選定 ${state.selectedDishes.length} 道餐點`, "success");
        closeModal('menu-modal'); // 關閉彈窗
        
    } catch (err) {
        showToast("選擇確認失敗：" + err.message, "error");
    }
}


// ============ 儲存用餐紀錄 ============

async function renderParticipantCheckboxes(sessionId) {
    const container = document.getElementById("record-participants-list");
    if (!container || !sessionId) return;

    container.innerHTML = '<span style="font-size:12px;color:#999;">載入中…</span>';
    try {
        const members = await request(`/api/groups/${sessionId}/members`);
        if (!members || !members.length) {
            container.innerHTML = '<span style="font-size:12px;color:#999;">無成員資料</span>';
            return;
        }
        container.innerHTML = members.map(m => {
            const uid = m.userId;
            const label = m.name || `用戶 #${uid}`;
            const isMe = String(uid) === String(state.userId);
            return `
            <label style="display:flex;align-items:center;gap:6px;
                          background:var(--surface);border:1px solid var(--border);
                          border-radius:20px;padding:4px 12px;cursor:pointer;font-size:13px;">
                <input type="checkbox" value="${uid}" ${isMe ? 'checked' : ''}>
                ${label}${isMe ? ' （我）' : ''}
            </label>`;
        }).join('');
    } catch {
        container.innerHTML = '<span style="font-size:12px;color:#999;">無法載入成員</span>';
    }
}

window.saveGroupRecord = async function() {
    const restaurant = state._selectedRestaurant;
    if (!restaurant) {
        showToast("請先選擇餐廳", "error");
        return;
    }

    const participantIds = [...document.querySelectorAll('#record-participants-list input[type=checkbox]:checked')]
        .map(cb => parseInt(cb.value));

    if (participantIds.length === 0) {
        showToast("群組至少要包含一個成員", "error");
        return;
    }

    const note = (document.getElementById("record-note")?.value || "").trim();
    const qtys = state.currentGroupDishQuantities || {};
    const dishNames = state.selectedDishes.length > 0
        ? state.selectedDishes.map(d => `${d.name} × ${qtys[d.dishId] || 1}`).join("、")
        : restaurant.name;
    const total = state.currentGroupTotalPrice || 0;
    const mealName = total > 0 ? `${dishNames}（合計 $${total}）` : dishNames;

    const now = new Date();
    const pad = n => String(n).padStart(2, '0');
    const visitDate = `${now.getFullYear()}-${pad(now.getMonth()+1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;

    try {
        await request("/api/history/save", {
            method: "POST",
            body: JSON.stringify({
                restaurantId: restaurant.restaurantId,
                visitDate: visitDate,
                mealName: mealName,
                note: note,
                participantIds: participantIds,
                groupSessionId: state._activeGroupSessionId
            })
        });

        showToast("紀錄已儲存！", "success");

        // 隱藏表單、清空狀態
        document.getElementById("record-form-panel")?.classList.add("hidden");
        document.getElementById("record-note").value = "";
        state._selectedRestaurant = null;
        state.selectedDishes = [];

        // 重新載入群組歷史紀錄
        if (state._activeGroupSessionId) {
            await loadGroupHistory(state._activeGroupSessionId);
        }
    } catch (err) {
        showToast("儲存失敗：" + err.message, "error");
    }
};

window.cancelRecord = function() {
    document.getElementById("record-form-panel")?.classList.add("hidden");
    document.getElementById("record-note").value = "";
    state._selectedRestaurant = null;
    state.selectedDishes = [];
};

// 查看餐廳評論
window.viewReviews = async function(restaurantId) {
    const modal = document.getElementById("review-modal");
    if (modal) modal.style.display = "block";
    
    const container = document.getElementById("review-container");
    if (!container) return;
    
    container.innerHTML = "<p style='color:#666;'>載入中...</p>";
    
    try {
        // 請求後端 API 取得評論列表
        const reviews = await request(`/api/restaurant/${restaurantId}/ratings`, { method: "GET" });
        
        if (!reviews || reviews.length === 0) {
            container.innerHTML = "<p style='color:var(--text-muted);text-align:center;padding:20px 0;'>這間餐廳目前還沒有人留下評論喔！</p>";
        } else {
            container.innerHTML = reviews.map(r => {
                const dateStr = r.ratedAt ? new Date(r.ratedAt).toLocaleDateString('zh-TW') : "未知日期";
                const authorName = (r.user && r.user.name) ? r.user.name : "匿名使用者";
                const ratingScore = r.score || 0;
                const commentText = r.comment || "無文字評論";
                const editedTag = r.isEdited ? `<span style="font-size:11px;color:var(--text-muted);margin-left:4px;">（已編輯）</span>` : "";

                return `
                    <div class="list-item">
                        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;">
                            <span class="list-item-title" style="margin-bottom:0;">${authorName}</span>
                            <span class="list-item-meta">${dateStr}${editedTag}</span>
                        </div>
                        <div class="list-item-meta" style="margin-bottom:4px;">⭐ ${ratingScore} / 5</div>
                        <div class="list-item-meta">${commentText}</div>
                    </div>
                `;
            }).join("");
        }
    } catch (error) {
        container.innerHTML = "<p style='color:red;'>無法載入評論，請確認伺服器連線或 API 路徑是否正確。</p>";
        console.error("無法載入評論：", error);
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