const state = {
    token: localStorage.getItem("rs_token") || "",
    userId: localStorage.getItem("rs_userId") || "",
    userName: localStorage.getItem("rs_userName") || "",
    currentGroupId: ""
};

const $ = (selector) => document.querySelector(selector);

function setSession(token, userId, userName) {
    state.token = token || "";
    state.userId = userId || "";
    state.userName = userName || "";
    localStorage.setItem("rs_token", state.token);
    localStorage.setItem("rs_userId", String(state.userId || ""));
    localStorage.setItem("rs_userName", state.userName || "");
    renderSession();
}

function renderSession() {
    $("#session-user").textContent = state.userName ? `${state.userName} (#${state.userId})` : "未登入";
    $("#session-token").textContent = `Token: ${state.token ? `${state.token.slice(0, 20)}...` : "-"}`;
}

function showToast(message, type = "success") {
    const toast = $("#toast");
    toast.className = `toast ${type}`;
    toast.textContent = message;
    setTimeout(() => {
        toast.className = "toast hidden";
    }, 2600);
}

async function request(path, options = {}) {
    const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
    if (state.token) headers.Authorization = `Bearer ${state.token}`;

    const response = await fetch(path, { ...options, headers });
    const payload = await response.json();
    if (!response.ok || payload.success === false) {
        throw new Error(payload.message || "Request failed");
    }
    return payload.data;
}

function formDataToJson(form) {
    const fd = new FormData(form);
    return Object.fromEntries(fd.entries());
}

function renderTable(container, rows) {
    if (!rows.length) {
        container.innerHTML = "<p>目前沒有資料</p>";
        return;
    }
    const headers = Object.keys(rows[0]);
    container.innerHTML = `
        <table>
            <thead><tr>${headers.map((h) => `<th>${h}</th>`).join("")}</tr></thead>
            <tbody>
                ${rows.map((r) => `<tr>${headers.map((h) => `<td>${r[h] ?? ""}</td>`).join("")}</tr>`).join("")}
            </tbody>
        </table>
    `;
}

async function handleRegister(e) {
    e.preventDefault();
    try {
        const body = formDataToJson(e.target);
        await request("/auth/register", { method: "POST", body: JSON.stringify(body) });
        showToast("註冊成功，請登入");
        e.target.reset();
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function handleLogin(e) {
    e.preventDefault();
    try {
        const body = formDataToJson(e.target);
        const data = await request("/auth/login", { method: "POST", body: JSON.stringify(body) });
        setSession(data.token, data.userId, data.name);
        showToast("登入成功");
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function loadRestaurants() {
    try {
        const restaurants = await request("/restaurant/all");
        const rows = restaurants.map((r) => ({
            ID: r.restaurantId,
            名稱: r.name,
            分類: r.category,
            價位: r.priceRange,
            評分: r.avgScore,
            評分數: r.ratingCount,
            地點: r.locationAt
        }));
        renderTable($("#restaurants-table"), rows);
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function handleCreateRestaurant(e) {
    e.preventDefault();
    try {
        const body = formDataToJson(e.target);
        body.avgScore = 0.0;
        body.ratingCount = 0;
        await request("/restaurant/save", { method: "POST", body: JSON.stringify(body) });
        showToast("餐廳建立成功");
        e.target.reset();
        loadRestaurants();
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function handleRateRestaurant(e) {
    e.preventDefault();
    try {
        if (!state.token) throw new Error("請先登入");
        const body = formDataToJson(e.target);
        body.restaurantId = Number(body.restaurantId);
        body.score = Number(body.score);
        await request("/restaurant/rate", { method: "POST", body: JSON.stringify(body) });
        showToast("評分送出成功");
        e.target.reset();
        loadRestaurants();
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function handleAddExclusion(e) {
    e.preventDefault();
    try {
        if (!state.token) throw new Error("請先登入");
        const body = formDataToJson(e.target);
        await request(`/user/exclusion?categoryId=${encodeURIComponent(body.categoryId)}`, { method: "POST" });
        showToast("已加入排除類別");
        e.target.reset();
        loadExclusions();
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function loadExclusions() {
    try {
        if (!state.token) throw new Error("請先登入");
        const exclusions = await request("/user/exclusions");
        if (!exclusions.length) {
            $("#exclusions-list").innerHTML = "<p>尚無排除類別</p>";
            return;
        }
        $("#exclusions-list").innerHTML = exclusions
            .map((ex) => `<span class="chip">#${ex.dish.categoryId} ${ex.dish.name}</span>`)
            .join("");
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function loadRecommendations() {
    try {
        if (!state.token) throw new Error("請先登入");
        const rec = await request("/recommend/personal");
        if (!rec.length) {
            $("#recommend-list").innerHTML = "<p>目前沒有推薦資料</p>";
            return;
        }
        const rows = rec.map((r) => `<li>${r.name} (${r.category}) - ${r.avgScore} 分</li>`).join("");
        $("#recommend-list").innerHTML = `<ul>${rows}</ul>`;
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function createGroup() {
    try {
        if (!state.token) throw new Error("請先登入");
        const session = await request("/groups/create", { method: "POST" });
        state.currentGroupId = session.sessionId;
        $("#group-current").textContent = `群組 #${session.sessionId}，邀請碼：${session.inviteCode}`;
        showToast("群組建立成功");
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function handleJoinGroup(e) {
    e.preventDefault();
    try {
        if (!state.token) throw new Error("請先登入");
        const body = formDataToJson(e.target);
        const session = await request(`/groups/join?inviteCode=${encodeURIComponent(body.inviteCode)}`, { method: "POST" });
        state.currentGroupId = session.sessionId;
        $("#group-current").textContent = `已加入群組 #${session.sessionId}，邀請碼：${session.inviteCode}`;
        showToast("加入群組成功");
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function handleSaveHistory(e) {
    e.preventDefault();
    try {
        if (!state.token) throw new Error("請先登入");
        const body = formDataToJson(e.target);
        body.restaurantId = Number(body.restaurantId);
        await request("/history/save", { method: "POST", body: JSON.stringify(body) });
        showToast("歷史紀錄已儲存");
        e.target.reset();
        loadHistory();
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function loadHistory() {
    try {
        if (!state.token) throw new Error("請先登入");
        const history = await request("/history/me");
        if (!history.length) {
            $("#history-list").innerHTML = "<p>目前沒有歷史紀錄</p>";
            return;
        }
        const rows = history.map((h) => ({
            ID: h.recordId,
            餐廳ID: h.restaurant?.restaurantId || "",
            餐點: h.mealName,
            日期: h.visitDate,
            備註: h.note
        }));
        renderTable($("#history-list"), rows);
    } catch (err) {
        showToast(err.message, "error");
    }
}

function bindEvents() {
    $("#register-form").addEventListener("submit", handleRegister);
    $("#login-form").addEventListener("submit", handleLogin);
    $("#restaurant-form").addEventListener("submit", handleCreateRestaurant);
    $("#rating-form").addEventListener("submit", handleRateRestaurant);
    $("#load-restaurants-btn").addEventListener("click", loadRestaurants);
    $("#exclusion-form").addEventListener("submit", handleAddExclusion);
    $("#load-exclusions-btn").addEventListener("click", loadExclusions);
    $("#load-recommend-btn").addEventListener("click", loadRecommendations);
    $("#create-group-btn").addEventListener("click", createGroup);
    $("#join-group-form").addEventListener("submit", handleJoinGroup);
    $("#history-form").addEventListener("submit", handleSaveHistory);
    $("#load-history-btn").addEventListener("click", loadHistory);
    $("#refresh-all-btn").addEventListener("click", () => {
        loadRestaurants();
        if (state.token) {
            loadExclusions();
            loadRecommendations();
            loadHistory();
        }
    });
}

function setDefaultDate() {
    const today = new Date().toISOString().slice(0, 10);
    $("#history-form input[name='visitDate']").value = today;
}

function init() {
    bindEvents();
    setDefaultDate();
    renderSession();
    loadRestaurants();
}

init();
const state = {
    user: null,
    sessionId: null
};

const byId = (id) => document.getElementById(id);

async function api(path, options = {}) {
    const response = await fetch(`/api${path}`, {
        headers: { "Content-Type": "application/json" },
        ...options
    });
    const data = await response.json();
    if (!response.ok) {
        throw new Error(data.error || "Request failed");
    }
    return data;
}

function showMessage(message, isError = false) {
    const el = byId("message");
    el.textContent = message;
    el.style.color = isError ? "#dc2626" : "#059669";
}

function requireLogin() {
    if (!state.user) {
        throw new Error("Please login first.");
    }
}

async function loadBaseData() {
    const [dishes, restaurants] = await Promise.all([
        api("/dishes"),
        api("/restaurants")
    ]);

    const dishSelect = byId("dish-select");
    dishSelect.innerHTML = dishes.map(d => `<option value="${d.categoryId}">${d.name}</option>`).join("");

    const options = restaurants.map(r => `<option value="${r.restaurantId}">${r.name} (${r.category})</option>`).join("");
    byId("restaurant-select").innerHTML = options;
    byId("history-restaurant-select").innerHTML = options;
}

function renderList(elementId, rows) {
    byId(elementId).innerHTML = rows.map(r => `<li>${r}</li>`).join("");
}

async function register() {
    try {
        const user = await api("/auth/register", {
            method: "POST",
            body: JSON.stringify({
                name: byId("reg-name").value.trim(),
                email: byId("reg-email").value.trim(),
                password: byId("reg-password").value
            })
        });
        showMessage(user.message);
    } catch (err) {
        showMessage(err.message, true);
    }
}

async function login() {
    try {
        const result = await api("/auth/login", {
            method: "POST",
            body: JSON.stringify({
                name: byId("login-name").value.trim(),
                password: byId("login-password").value
            })
        });
        state.user = result.user;
        byId("current-user").textContent = `Logged in: ${result.user.name} (userId=${result.user.userId})`;
        showMessage(result.message);
    } catch (err) {
        showMessage(err.message, true);
    }
}

async function addExclusion() {
    try {
        requireLogin();
        const categoryId = Number(byId("dish-select").value);
        await api("/user/exclusion", {
            method: "POST",
            body: JSON.stringify({
                userId: state.user.userId,
                categoryId
            })
        });
        showMessage("Exclusion saved.");
    } catch (err) {
        showMessage(err.message, true);
    }
}

async function recommend() {
    try {
        requireLogin();
        const rows = await api(`/recommend?userId=${state.user.userId}`);
        renderList("recommend-list", rows.map(r => `${r.name} | ${r.category} | score: ${r.avgScore}`));
        showMessage("Recommendations loaded.");
    } catch (err) {
        showMessage(err.message, true);
    }
}

async function submitRating() {
    try {
        requireLogin();
        const restaurantId = Number(byId("restaurant-select").value);
        const score = Number(byId("score-select").value);
        const comment = byId("rating-comment").value;
        await api("/rate", {
            method: "POST",
            body: JSON.stringify({
                userId: state.user.userId,
                restaurantId,
                score,
                comment
            })
        });
        const ratings = await api(`/restaurant/${restaurantId}/ratings`);
        renderList("rating-list", ratings.map(r => `user ${r.userId}: ${r.score}★ - ${r.comment}`));
        showMessage("Rating submitted.");
    } catch (err) {
        showMessage(err.message, true);
    }
}

async function createGroup() {
    try {
        requireLogin();
        const group = await api("/groups/create", {
            method: "POST",
            body: JSON.stringify({ creatorId: state.user.userId })
        });
        state.sessionId = group.sessionId;
        byId("group-info").textContent = `Session ${group.sessionId}, invite: ${group.inviteCode}`;
        byId("invite-code").value = group.inviteCode;
        showMessage("Group created.");
    } catch (err) {
        showMessage(err.message, true);
    }
}

async function joinGroup() {
    try {
        requireLogin();
        const inviteCode = byId("invite-code").value.trim();
        await api("/groups/join", {
            method: "POST",
            body: JSON.stringify({ userId: state.user.userId, inviteCode })
        });
        showMessage("Joined group.");
    } catch (err) {
        showMessage(err.message, true);
    }
}

async function groupRecommend() {
    try {
        if (!state.sessionId) {
            throw new Error("Create a group first in this browser.");
        }
        const rows = await api(`/groups/${state.sessionId}/recommend`);
        renderList("group-recommend-list", rows.map(r => `${r.name} | ${r.category} | score: ${r.avgScore}`));
        showMessage("Group recommendations loaded.");
    } catch (err) {
        showMessage(err.message, true);
    }
}

async function saveHistory() {
    try {
        requireLogin();
        await api("/history/save", {
            method: "POST",
            body: JSON.stringify({
                userId: state.user.userId,
                restaurantId: Number(byId("history-restaurant-select").value),
                mealName: byId("history-meal").value.trim(),
                note: byId("history-note").value.trim(),
                visitDate: new Date().toISOString().slice(0, 10)
            })
        });
        showMessage("History saved.");
    } catch (err) {
        showMessage(err.message, true);
    }
}

async function loadHistory() {
    try {
        requireLogin();
        const rows = await api(`/history/me?userId=${state.user.userId}`);
        renderList("history-list", rows.map(r => `restaurant ${r.restaurantId} | ${r.visitDate} | ${r.mealName} | ${r.note}`));
        showMessage("History loaded.");
    } catch (err) {
        showMessage(err.message, true);
    }
}

function bindEvents() {
    byId("register-btn").addEventListener("click", register);
    byId("login-btn").addEventListener("click", login);
    byId("add-exclusion-btn").addEventListener("click", addExclusion);
    byId("recommend-btn").addEventListener("click", recommend);
    byId("rate-btn").addEventListener("click", submitRating);
    byId("create-group-btn").addEventListener("click", createGroup);
    byId("join-group-btn").addEventListener("click", joinGroup);
    byId("group-recommend-btn").addEventListener("click", groupRecommend);
    byId("save-history-btn").addEventListener("click", saveHistory);
    byId("load-history-btn").addEventListener("click", loadHistory);
}

async function init() {
    bindEvents();
    await loadBaseData();
}

init().catch(err => showMessage(err.message, true));
