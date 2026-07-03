const API_BASE = window.BAECHU_API_BASE || localStorage.getItem("baechuApiBase") || "";
const ACCESS_TOKEN_KEY = "cabbageAccessToken";

export function getToken() {
    return localStorage.getItem(ACCESS_TOKEN_KEY) || "";
}

export function setToken(token) {
    if (!token) {
        localStorage.removeItem(ACCESS_TOKEN_KEY);
        return;
    }
    localStorage.setItem(ACCESS_TOKEN_KEY, token);
}

export function clearToken() {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
}

function getCookie(name) {
    return document.cookie
        .split("; ")
        .find(cookie => cookie.startsWith(`${name}=`))
        ?.split("=")[1] || "";
}

function buildQuery(params = {}) {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== "") {
            query.set(key, value);
        }
    });
    const queryString = query.toString();
    return queryString ? `?${queryString}` : "";
}

async function request(path, options = {}) {
    const headers = new Headers(options.headers || {});
    const token = getToken();

    if (!headers.has("Content-Type") && options.body && !(options.body instanceof FormData)) {
        headers.set("Content-Type", "application/json");
    }

    if (token) {
        headers.set("Authorization", `Bearer ${token}`);
    }

    const xsrfToken = getCookie("XSRF-TOKEN");
    if (xsrfToken && !headers.has("X-XSRF-TOKEN")) {
        headers.set("X-XSRF-TOKEN", decodeURIComponent(xsrfToken));
    }

    let response;
    try {
        response = await fetch(`${API_BASE}${path}`, {
            credentials: "include",
            ...options,
            headers
        });
    } catch {
        throw new Error("API 서버에 연결할 수 없습니다. 백엔드 서버 상태를 확인해 주세요.");
    }

    const authorization = response.headers.get("Authorization");
    if (authorization) {
        setToken(authorization.replace(/^Bearer\s+/i, ""));
    }

    const text = await response.text();
    const payload = text ? JSON.parse(text) : null;

    if (!response.ok) {
        const error = new Error(payload?.message || payload?.code || "요청에 실패했습니다.");
        error.status = response.status;
        throw error;
    }

    return payload?.data ?? payload;
}

export const api = {
    signup: payload => request("/api/auth/signup", { method: "POST", body: JSON.stringify(payload) }),
    login: payload => request("/api/auth/login", { method: "POST", body: JSON.stringify(payload) }),
    logout: () => request("/api/auth/logout", { method: "POST" }),
    getMe: () => request("/api/clients/me"),
    listCategories: params => request(`/api/categories${buildQuery(params)}`),
    listItems: params => request(`/api/items${buildQuery(params)}`),
    searchItems: params => request(`/api/v1/items/search${buildQuery(params)}`),
    getItem: itemId => request(`/api/items/${itemId}`),
    getClientProfile: clientId => request(`/api/clients/${clientId}`),
    listClientReviews: (clientId, params) => request(`/api/clients/${clientId}/reviews${buildQuery(params)}`),
    toggleLike: itemId => request(`/api/items/${itemId}/likes`, { method: "POST" }),
    listMyItems: params => request(`/api/clients/me/items${buildQuery(params)}`),
    listMyLikes: params => request(`/api/clients/me/likes${buildQuery(params)}`),
    listWrittenReviews: params => request(`/api/clients/me/reviews/written${buildQuery(params)}`),
    createItem: payload => request("/api/items", { method: "POST", body: JSON.stringify(payload) }),
    createDraft: payload => request("/api/items/drafts", { method: "POST", body: JSON.stringify(payload) }),
    updateItem: (itemId, payload) => request(`/api/items/${itemId}`, { method: "PUT", body: JSON.stringify(payload) }),
    updateItemStatus: (itemId, payload) => request(`/api/items/${itemId}/status`, { method: "PATCH", body: JSON.stringify(payload) }),
    bidItem: (itemId, payload) => request(`/api/items/${itemId}/auction-status/bid`, { method: "POST", body: JSON.stringify(payload) }),
    createReview: (itemId, payload) => request(`/api/items/${itemId}/reviews`, { method: "POST", body: JSON.stringify(payload) }),
    uploadItemImages: (itemId, files) => {
        const formData = new FormData();
        Array.from(files).forEach(file => formData.append("files", file));
        return request(`/api/items/${itemId}/images`, { method: "POST", body: formData });
    },
    createChatRoom: itemId => request(`/api/chat-rooms/${itemId}`, { method: "POST" }),
    listChatRooms: params => request(`/api/chat-rooms/my${buildQuery(params)}`),
    listChatMessages: (roomId, params) => request(`/api/chat-rooms/${roomId}/messages${buildQuery(params)}`)
};
