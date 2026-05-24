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
        } else if (pageName === 'groups') {
            loadMyGroups();
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
    container.innerHTML = state.restaurants.map(r => {
        // 自動防呆：判斷後端回傳的主鍵名稱是 restaurantId 還是 id
        const targetId = r.restaurantId || r.id; 
        
        return `
        <div class="list-item">
            <div class="list-item-title">${r.name}</div>
            <div class="list-item-meta">
                <strong>${r.category}</strong> | ${r.priceRange} | ⭐ ${r.avgScore} (${r.ratingCount}人)
            </div>
            <div class="list-item-meta">${r.locationAt}</div>
            <div class="button-group" style="margin-top: 12px;">
                <button type="button" style="padding: 4px 8px; margin-right: 5px; cursor: pointer; border-radius: 4px; border: 1px solid #ccc; background: #f9f9f9;" onclick="viewMenu(${targetId})">📋 查看菜單</button>
                <button type="button" style="padding: 4px 8px; cursor: pointer; border-radius: 4px; border: 1px solid #ccc; background: #f9f9f9;" onclick="viewReviews(${targetId})">💬 查看評論</button>
            </div>
        </div>
        `;
    }).join("");
}

// 👉 修改重點：既然把新增餐廳介面拿掉了，這裡也註解掉以防報錯
/*document.getElementById("restaurant-form").addEventListener("submit", async (e) => {
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
*/

async function loadRestaurantSelects() {
    try {
        await loadRestaurants();
        const options = state.restaurants.map(r => {
            const targetId = r.restaurantId || r.id;
            return `<option value="${targetId}">${r.name} (${r.category})</option>`;
        }).join("");
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
            return { label: "選擇要排除的菜餚", key: "dishes", idKey: "dishId" };
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

    // prepare visible select options and a hidden JSON store for autocomplete
    itemsSelect.innerHTML = options
        .map(item => `<option value="${item[meta.idKey]}">${item.name}</option>`)
        .join("");

    const store = document.getElementById('exclude-options');
    if (store) store.dataset.options = JSON.stringify(options.map(item => ({ id: item[meta.idKey], name: item.name })));

    // reset search and selected UI if present
    const search = document.getElementById('exclude-search');
    const suggestions = document.getElementById('exclude-suggestions');
    const selectedList = document.getElementById('exclude-selected');
    if (search) search.value = '';
    if (suggestions) suggestions.innerHTML = '';
    if (selectedList) selectedList.innerHTML = '';
}

// AUTOCOMPLETE: selected items tracked here
state._excludeSelected = [];

function renderSuggestions(filtered) {
    const suggestions = document.getElementById('exclude-suggestions');
    if (!suggestions) return;
    if (!filtered.length) {
        suggestions.classList.add('hidden');
        suggestions.innerHTML = '';
        return;
    }
    suggestions.classList.remove('hidden');
    suggestions.innerHTML = filtered.map(opt => `<div class="suggestion-item" data-id="${opt.id}">${opt.name}</div>`).join('');
    suggestions.querySelectorAll('.suggestion-item').forEach(el => {
        el.addEventListener('click', () => {
            addExcludeSelected(el.dataset.id, el.textContent);
        });
    });
}

function renderExcludeSelected() {
    const container = document.getElementById('exclude-selected');
    if (!container) return;
    if (!state._excludeSelected.length) {
        container.innerHTML = '';
        return;
    }
    container.innerHTML = state._excludeSelected.map(s => `
        <span class="tag" data-id="${s.id}">${s.name} <button class="tag-remove" data-id="${s.id}">×</button></span>
    `).join('');
    container.querySelectorAll('.tag-remove').forEach(btn => {
        btn.addEventListener('click', () => {
            const id = btn.dataset.id;
            state._excludeSelected = state._excludeSelected.filter(x => String(x.id) !== String(id));
            renderExcludeSelected();
        });
    });
}

function addExcludeSelected(id, name) {
    if (!id) return;
    // avoid duplicates
    if (state._excludeSelected.some(x => String(x.id) === String(id))) return;
    state._excludeSelected.push({ id, name });
    renderExcludeSelected();
    // clear suggestions and search
    const search = document.getElementById('exclude-search');
    const suggestions = document.getElementById('exclude-suggestions');
    if (search) search.value = '';
    if (suggestions) {
        suggestions.classList.add('hidden');
        suggestions.innerHTML = '';
    }
}

// wire input events
const searchInput = document.getElementById('exclude-search');
if (searchInput) {
    searchInput.addEventListener('input', (e) => {
        const type = document.getElementById('exclude-type').value;
        const q = (e.target.value || '').trim();
        if (!q) {
            renderSuggestions([]);
            return;
        }

        // Try server-side search for suggestions
        const apiMap = {
            ingredient: `/api/ingredient/search?q=${encodeURIComponent(q)}&limit=10`,
            dish: `/api/dish/search?q=${encodeURIComponent(q)}&limit=10`,
            restaurant: `/api/restaurant/search?q=${encodeURIComponent(q)}&limit=10`
        };

        const url = apiMap[type] || apiMap.ingredient;
        request(url).then(items => {
            // normalize items to {id, name}
            const normalized = (items || []).map(it => {
                if (it.ingredientId) return { id: it.ingredientId, name: it.name };
                if (it.dishId) return { id: it.dishId, name: it.name };
                if (it.restaurantId) return { id: it.restaurantId, name: it.name };
                return { id: it.id || it[Object.keys(it)[0]], name: it.name || String(it) };
            });
            renderSuggestions(normalized);
        }).catch(() => {
            // fallback to client-side filter using stored options
            const raw = document.getElementById('exclude-options');
            const options = JSON.parse(raw?.dataset?.options || '[]');
            const filtered = options.filter(o => o.name.toLowerCase().includes(q.toLowerCase())).slice(0, 10);
            renderSuggestions(filtered);
        });
    });

    // handle Enter to add top suggestion or exact match
    searchInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            const raw = document.getElementById('exclude-options');
            const options = JSON.parse(raw?.dataset?.options || '[]');
            const q = (searchInput.value || '').trim().toLowerCase();
            if (!q) return;
            const match = options.find(o => o.name.toLowerCase() === q) || options.find(o => o.name.toLowerCase().includes(q));
            if (match) addExcludeSelected(match.id, match.name);
        }
    });
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
        const type = typeSelect.value;
        let selected = [];
        if (state._excludeSelected && state._excludeSelected.length) {
            selected = state._excludeSelected;
        } else {
            const selectEl = document.getElementById('exclude-items');
            if (selectEl) {
                const ids = getSelectedValues(selectEl);
                selected = ids.map(id => ({ id }));
            }
        }

        if (!selected.length) {
            throw new Error("請至少選擇一個排除項目（可從下拉或搜尋選取）");
        }

        let exclusionsAdded = 0;
        let existingCount = 0;
        let notFoundItems = [];

        if (type === "ingredient") {
                const ingredients = selected
                    .map(s => state.exclusionOptions.ingredients.find(item => String(item.ingredientId) === String(s.id)))
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
            for (const s of selected) {
                const dish = state.exclusionOptions.dishes.find(d => String(d.dishId) === String(s.id));
                if (dish) {
                    await request(`/api/user/exclusion/dish?dishId=${dish.dishId}`, { method: "POST" });
                    exclusionsAdded++;
                }
            }
        } else if (type === "restaurant") {
            for (const s of selected) {
                const rest = state.exclusionOptions.restaurants.find(r => String(r.restaurantId) === String(s.id));
                if (rest) {
                    await request(`/api/user/exclusion/restaurant?restaurantId=${rest.restaurantId}`, { method: "POST" });
                    exclusionsAdded++;
                }
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
            // clear selected tags
            state._excludeSelected = [];
            renderExcludeSelected();
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
                    type: 'dish',
                    value: e.dish.name,
                    dishId: e.dish.dishId
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
            case 'dish':
                typeLabel = "菜餚";
                break;
            case 'restaurant':
                typeLabel = "餐廳";
                break;
            default:
                typeLabel = "其他";
        }

        let targetId = "";
        if (pref.type === "ingredient") targetId = pref.ingredientId;
        if (pref.type === "dish") targetId = pref.dishId;
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
        loadMyGroups();
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

    container.innerHTML = groups.map(group => `
        <div class="list-item">
            <div class="list-item-title">群組 #${group.sessionId}</div>
            <div class="list-item-meta">
                狀態：${group.status} | 邀請碼：${group.inviteCode}
            </div>
            <div class="button-group" style="margin-top: 12px;">
                <button type="button" class="btn-secondary" onclick="loadGroupRecommendations(${group.sessionId})">查看推薦</button>
            </div>
        </div>
    `).join("");
}

async function loadGroupRecommendations(sessionId) {
    try {
        if (!state.token) throw new Error("請先登入");
        const recs = await request(`/api/groups/${sessionId}/recommend`);
        renderGroupRecommendations(recs, sessionId);
    } catch (err) {
        showToast(err.message, "error");
    }
}

function renderGroupRecommendations(recommendations, sessionId) {
    const container = document.getElementById("group-recommend-list");
    if (!container) return;
    if (!recommendations.length) {
        container.innerHTML = '<div class="empty-state"><div class="empty-state-icon">⭐</div>暫無推薦</div>';
        return;
    }

    container.innerHTML = `
        <div class="list-item" style="margin-bottom: 12px;">
            <div class="list-item-title">群組 #${sessionId} 推薦</div>
        </div>
    ` + recommendations.map(r => `
        <div class="list-item">
            <div class="list-item-title">${r.name}</div>
            <div class="list-item-meta">
                分類：<strong>${r.category}</strong> | 評分：⭐ ${r.avgScore.toFixed(1)} | ${r.priceRange}
            </div>
        </div>
    `).join("");
}

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

// ============ 彈窗 (Modal) 與詳細資訊查看邏輯 ============

// 關閉彈窗
window.closeModal = function(modalId) {
    const modal = document.getElementById(modalId);
    if(modal) {
        modal.style.display = "none";
    }
}

// 查看菜單
window.viewMenu = async function(restaurantId) {
    const modal = document.getElementById("menu-modal");
    if (modal) modal.style.display = "block";
    
    const container = document.getElementById("menu-container");
    if (!container) return;
    
    container.innerHTML = "<p style='color:#666;'>載入中...</p>";

    try {
        // 👇 加入這行看傳出什麼 ID
        console.log("正在查詢的餐廳 ID:", restaurantId); 
        
        const dishes = await request(`/api/dish/restaurant/${restaurantId}`); 
        
        // 👇 加入這行看後端回傳了什麼
        console.log("後端回傳的菜單資料:", dishes); 
        
        if (dishes && dishes.length > 0) {
            container.innerHTML = dishes.map(dish => 
                `<div style="margin-bottom: 8px; padding-bottom: 8px; border-bottom: 1px dashed #eee;">
                   <strong>${dish.name}</strong>
                 </div>`
            ).join('');
        } else {
            container.innerHTML = "<p style='color:#999;'>這間餐廳目前沒有建立菜單喔！</p>";
        }
    } catch (error) {
        container.innerHTML = "<p style='color:red;'>無法載入菜單，請確認伺服器連線或 API 路徑是否正確。</p>";
        console.error(error);
    }
}

// 查看評論
window.viewReviews = async function(restaurantId) {
    const modal = document.getElementById("review-modal");
    if (modal) modal.style.display = "block";
    
    const container = document.getElementById("review-container");
    if (!container) return;
    
    container.innerHTML = "<p style='color:#666;'>載入中...</p>";

    try {
        // 請求評論 API (加上了 /api 前綴，請確認路徑與後端定義是否相符)
        const reviews = await request(`/api/restaurant/${restaurantId}/ratings`);
        if (reviews && reviews.length > 0) {
            container.innerHTML = reviews.map(r => 
                `<div style="margin-bottom: 12px; border-bottom: 1px solid #eee; padding-bottom: 8px;">
                   <p style="margin: 0;">⭐ <strong>${r.score} / 5</strong></p>
                   <p style="margin: 5px 0 0 0; color: #555;">${r.comment || '無文字評論'}</p>
                 </div>`
            ).join('');
        } else {
            container.innerHTML = "<p style='color:#999;'>這間餐廳目前還沒有人留下評論喔！</p>";
        }
    } catch (error) {
        container.innerHTML = "<p style='color:red;'>無法載入評論，請確認伺服器連線或 API 路徑是否正確。</p>";
        console.error(error);
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