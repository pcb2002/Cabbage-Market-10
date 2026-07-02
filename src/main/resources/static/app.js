const API_BASE = window.BAECHU_API_BASE || localStorage.getItem("baechuApiBase") || "";
const ACCESS_TOKEN_KEY = "baechuAccessToken";

const state = {
    token: localStorage.getItem(ACCESS_TOKEN_KEY) || "",
    categories: [],
    currentItemId: null,
    currentChatRoomId: null,
    products: [
        {
            itemId: 1,
            title: "라이카 M10-P 블랙 페인트 에디션",
            categoryName: "중고 카메라",
            initialPrice: 8500000,
            currentBid: null,
            tradeType: "DIRECT",
            tradeStatus: "ON_SALE",
            conditionType: "USED",
            likeCount: 24,
            viewCount: 450,
            createdAt: "2026-06-20T10:00:00",
            thumbnailUrl: "https://images.unsplash.com/photo-1512790182412-b19e6d62bc39?auto=format&fit=crop&w=720&q=80",
            description: "상태 좋은 카메라입니다. 생활 흠집은 거의 없고 렌즈 캡과 박스를 함께 드립니다."
        },
        {
            itemId: 2,
            title: "소니 WH-1000XM5 실버",
            categoryName: "디지털기기",
            initialPrice: 320000,
            currentBid: null,
            tradeType: "DIRECT",
            tradeStatus: "RESERVED",
            conditionType: "USED",
            likeCount: 51,
            viewCount: 312,
            createdAt: "2026-06-21T09:00:00",
            thumbnailUrl: "https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=720&q=80",
            description: "노이즈 캔슬링이 좋은 헤드폰입니다. 실사용 기간은 6개월입니다."
        },
        {
            itemId: 3,
            title: "브라운 계산기 ET66 복각판",
            categoryName: "문구",
            initialPrice: 45000,
            currentBid: null,
            tradeType: "DIRECT",
            tradeStatus: "ON_SALE",
            conditionType: "NEW",
            likeCount: 18,
            viewCount: 128,
            createdAt: "2026-06-22T08:30:00",
            thumbnailUrl: "https://images.unsplash.com/photo-1564473185935-58113cba1e80?auto=format&fit=crop&w=720&q=80",
            description: "미개봉 복각판 계산기입니다. 책상 위 소품으로도 좋습니다."
        },
        {
            itemId: 4,
            title: "커스텀 기계식 키보드 HHKB 레이아웃",
            categoryName: "디지털기기",
            initialPrice: 280000,
            currentBid: null,
            tradeType: "DIRECT",
            tradeStatus: "SOLD_OUT",
            conditionType: "USED",
            likeCount: 38,
            viewCount: 908,
            createdAt: "2026-06-22T11:20:00",
            thumbnailUrl: "https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?auto=format&fit=crop&w=720&q=80",
            description: "윤활 작업 완료된 커스텀 키보드입니다. 조용하고 단단한 타건감입니다."
        },
        {
            itemId: 5,
            title: "아르떼미데 네시노 테이블 램프",
            categoryName: "가구/인테리어",
            initialPrice: 185000,
            currentBid: 211000,
            tradeType: "AUCTION",
            tradeStatus: "ON_SALE",
            conditionType: "USED",
            likeCount: 30,
            viewCount: 402,
            createdAt: "2026-06-23T12:30:00",
            thumbnailUrl: "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=720&q=80",
            description: "따뜻한 빛감의 테이블 램프입니다. 경매 상품으로 현재 입찰 중입니다."
        },
        {
            itemId: 6,
            title: "펠로우 오드 그라인더 Gen 2",
            categoryName: "생활",
            initialPrice: 350000,
            currentBid: null,
            tradeType: "DIRECT",
            tradeStatus: "ON_SALE",
            conditionType: "USED",
            likeCount: 17,
            viewCount: 260,
            createdAt: "2026-06-24T07:30:00",
            thumbnailUrl: "https://images.unsplash.com/photo-1517668808822-9ebb02f2a0e6?auto=format&fit=crop&w=720&q=80",
            description: "커피 그라인더입니다. 분쇄 균일도가 좋고 구성품 모두 있습니다."
        },
        {
            itemId: 7,
            title: "몬스테라 알보 희귀식물",
            categoryName: "식물",
            initialPrice: 120000,
            currentBid: null,
            tradeType: "DIRECT",
            tradeStatus: "ON_SALE",
            conditionType: "NEW",
            likeCount: 44,
            viewCount: 512,
            createdAt: "2026-06-24T15:00:00",
            thumbnailUrl: "https://images.unsplash.com/photo-1463320726281-696a485928c7?auto=format&fit=crop&w=720&q=80",
            description: "건강한 몬스테라입니다. 직거래 선호합니다."
        },
        {
            itemId: 8,
            title: "벨로라인 클래식 시티 바이크",
            categoryName: "취미/게임",
            initialPrice: 210000,
            currentBid: null,
            tradeType: "DIRECT",
            tradeStatus: "ON_SALE",
            conditionType: "USED",
            likeCount: 9,
            viewCount: 104,
            createdAt: "2026-06-24T17:10:00",
            thumbnailUrl: "https://images.unsplash.com/photo-1507035895480-2b3156c31fc8?auto=format&fit=crop&w=720&q=80",
            description: "가벼운 출퇴근용 시티 바이크입니다."
        }
    ]
};

const app = document.querySelector("#app");
const toast = document.querySelector("#toast");

function buildQuery(params = {}) {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== "") {
            query.set(key, value);
        }
    });
    const text = query.toString();
    return text ? `?${text}` : "";
}

function getCookie(name) {
    return document.cookie.split("; ").find(cookie => cookie.startsWith(`${name}=`))?.split("=")[1] || "";
}

async function request(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (!headers.has("Content-Type") && options.body && !(options.body instanceof FormData)) {
        headers.set("Content-Type", "application/json");
    }
    if (state.token) {
        headers.set("Authorization", `Bearer ${state.token}`);
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
        throw new Error("API 서버에 연결할 수 없습니다. 백엔드 서버를 실행한 뒤 다시 시도해주세요.");
    }

    const authorization = response.headers.get("Authorization");
    if (authorization) {
        state.token = authorization.replace(/^Bearer\s+/i, "");
        localStorage.setItem(ACCESS_TOKEN_KEY, state.token);
        updateLoginButton();
    }

    const text = await response.text();
    let payload = null;
    try {
        payload = text ? JSON.parse(text) : null;
    } catch {
        throw new Error(`API 응답 형식이 올바르지 않습니다. (${response.status})`);
    }
    if (!response.ok) {
        throw new Error(payload?.message || payload?.code || "요청에 실패했습니다.");
    }
    return payload?.data ?? payload;
}

const apiClient = {
    signup: payload => request("/api/auth/signup", { method: "POST", body: JSON.stringify(payload) }),
    login: payload => request("/api/auth/login", { method: "POST", body: JSON.stringify(payload) }),
    refresh: () => request("/api/auth/refresh", { method: "POST" }),
    logout: () => request("/api/auth/logout", { method: "POST" }),
    getMe: () => request("/api/clients/me"),
    updateMe: payload => request("/api/clients/me", { method: "PATCH", body: JSON.stringify(payload) }),
    getClientProfile: clientId => request(`/api/clients/${clientId}`),
    listCategories: params => request(`/api/categories${buildQuery(params)}`),
    listItems: params => request(`/api/items${buildQuery(params)}`),
    getItem: itemId => request(`/api/items/${itemId}`),
    createItem: payload => request("/api/items", { method: "POST", body: JSON.stringify(payload) }),
    createDraft: payload => request("/api/items/drafts", { method: "POST", body: JSON.stringify(payload) }),
    publishItem: itemId => request(`/api/items/${itemId}/publish`, { method: "POST" }),
    updateItem: (itemId, payload) => request(`/api/items/${itemId}`, { method: "PUT", body: JSON.stringify(payload) }),
    updateItemStatus: (itemId, payload) => request(`/api/items/${itemId}/status`, { method: "PATCH", body: JSON.stringify(payload) }),
    deleteItem: itemId => request(`/api/items/${itemId}`, { method: "DELETE" }),
    uploadItemImages: (itemId, files) => {
        const formData = new FormData();
        Array.from(files).forEach(file => formData.append("files", file));
        return request(`/api/items/${itemId}/images`, { method: "POST", body: formData });
    },
    setThumbnail: (itemId, imageId) => request(`/api/items/${itemId}/images/${imageId}/thumbnail`, { method: "PATCH" }),
    deleteImage: (itemId, imageId) => request(`/api/items/${itemId}/images/${imageId}`, { method: "DELETE" }),
    toggleLike: itemId => request(`/api/items/${itemId}/likes`, { method: "POST" }),
    searchItems: params => request(`/api/v1/items/search${buildQuery(params)}`),
    searchPopular: () => request("/api/search/popular"),
    listMyItems: params => request(`/api/clients/me/items${buildQuery(params)}`),
    listMyLikes: params => request(`/api/clients/me/likes${buildQuery(params)}`),
    followClient: clientId => request(`/api/clients/${clientId}/follows`, { method: "POST" }),
    unfollowClient: clientId => request(`/api/clients/${clientId}/follows`, { method: "DELETE" }),
    listFollowings: params => request(`/api/clients/me/followings${buildQuery(params)}`),
    listFollowers: params => request(`/api/clients/me/followers${buildQuery(params)}`),
    listClientReviews: clientId => request(`/api/clients/${clientId}/reviews`),
    listWrittenReviews: params => request(`/api/clients/me/reviews/written${buildQuery(params)}`),
    createReview: (itemId, payload) => request(`/api/items/${itemId}/reviews`, { method: "POST", body: JSON.stringify(payload) }),
    updateReview: (reviewId, payload) => request(`/api/reviews/${reviewId}`, { method: "PATCH", body: JSON.stringify(payload) }),
    deleteReview: reviewId => request(`/api/reviews/${reviewId}`, { method: "DELETE" }),
    listInquiries: (itemId, params) => request(`/api/items/${itemId}/inquiries${buildQuery(params)}`),
    createInquiry: (itemId, payload) => request(`/api/items/${itemId}/inquiries`, { method: "POST", body: JSON.stringify(payload) }),
    updateInquiry: (inquiryId, payload) => request(`/api/inquiries/${inquiryId}`, { method: "PUT", body: JSON.stringify(payload) }),
    deleteInquiry: inquiryId => request(`/api/inquiries/${inquiryId}`, { method: "DELETE" }),
    createInquiryAnswer: (inquiryId, payload) => request(`/api/inquiries/${inquiryId}/answer`, { method: "POST", body: JSON.stringify(payload) }),
    updateInquiryAnswer: (inquiryId, payload) => request(`/api/inquiries/${inquiryId}/answer`, { method: "PATCH", body: JSON.stringify(payload) }),
    deleteInquiryAnswer: inquiryId => request(`/api/inquiries/${inquiryId}/answer`, { method: "DELETE" }),
    createChatRoom: itemId => request(`/api/chat-rooms/${itemId}`, { method: "POST" }),
    listChatRooms: params => request(`/api/chat-rooms/my${buildQuery(params)}`),
    listChatMessages: (chatRoomId, params) => request(`/api/chat-rooms/${chatRoomId}/messages${buildQuery(params)}`),
    deleteChatMessage: messageId => request(`/api/chat-rooms/${messageId}`, { method: "DELETE" }),
    leaveChatRoom: chatRoomId => request(`/api/chat-rooms/${chatRoomId}/leave`, { method: "POST" }),
    markChatRoomRead: chatRoomId => request(`/api/chat-rooms/${chatRoomId}/read`, { method: "POST" }),
    bidItem: (itemId, payload) => request(`/api/items/${itemId}/auction-status/bid`, { method: "POST", body: JSON.stringify(payload) })
};

window.baechuApi = apiClient;

function formatPrice(value) {
    return `${Number(value || 0).toLocaleString("ko-KR")}원`;
}

function statusLabel(status) {
    return {
        ON_SALE: "판매중",
        RESERVED: "예약중",
        SOLD_OUT: "거래완료"
    }[status] || "판매중";
}

function timeAgo(dateText) {
    const date = new Date(dateText);
    if (Number.isNaN(date.getTime())) return "방금 전";
    const hours = Math.max(1, Math.round((Date.now() - date.getTime()) / 36e5));
    if (hours < 24) return `${hours}시간 전`;
    const days = Math.round(hours / 24);
    return days < 7 ? `${days}일 전` : `${Math.round(days / 7)}주일 전`;
}

function showToast(message) {
    toast.textContent = message;
    toast.classList.add("show");
    window.setTimeout(() => toast.classList.remove("show"), 2200);
}

function mount(templateId) {
    const template = document.querySelector(`#${templateId}`);
    app.replaceChildren(template.content.cloneNode(true));
    app.focus({ preventScroll: true });
}

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, char => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        "\"": "&quot;",
        "'": "&#39;"
    }[char]));
}

function productImage(product) {
    return product.thumbnailUrl || product.images?.find(image => image.isThumbnail)?.imageUrl || product.images?.[0]?.imageUrl || "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=720&q=80";
}

function productId(product) {
    return product.itemId || product.id;
}

function normalizeProduct(item, index = 0) {
    const fallback = state.products[index % state.products.length] || {};
    return {
        ...fallback,
        ...item,
        itemId: item.itemId || item.id || fallback.itemId,
        thumbnailUrl: item.thumbnailUrl || item.images?.find(image => image.isThumbnail)?.imageUrl || fallback.thumbnailUrl,
        categoryName: item.categoryName || fallback.categoryName || "중고",
        tradeType: item.tradeType || fallback.tradeType || "DIRECT",
        tradeStatus: item.tradeStatus || fallback.tradeStatus || "ON_SALE",
        likeCount: item.likeCount ?? fallback.likeCount ?? 0,
        viewCount: item.viewCount ?? fallback.viewCount ?? 0,
        createdAt: item.createdAt || fallback.createdAt || new Date().toISOString()
    };
}

function productCard(product) {
    const article = document.createElement("article");
    article.className = "product-card";
    article.innerHTML = `
        <button class="image-button" type="button" data-detail="${productId(product)}">
            <img src="${productImage(product)}" alt="${escapeHtml(product.title)}">
        </button>
        <button class="heart-button" type="button" data-like="${productId(product)}" aria-label="관심상품">♡</button>
        <p class="product-title">${escapeHtml(product.title)}</p>
        <p class="product-meta">${escapeHtml(product.categoryName || "중고")} · ${product.tradeType === "AUCTION" ? "경매" : "직거래"} · ${timeAgo(product.createdAt)}</p>
        <strong>${formatPrice(product.currentBid || product.initialPrice)}</strong>
    `;
    return article;
}

async function loadCategories() {
    try {
        const data = await apiClient.listCategories({ page: 0, size: 50 });
        state.categories = data?.content?.filter(category => category.isActive !== false) || [];
    } catch {
        state.categories = [];
    }
}

function hydrateCategorySelect() {
    const select = document.querySelector('select[name="categoryId"]');
    if (!select || state.categories.length === 0) return;
    const currentValue = select.value;
    select.replaceChildren(new Option("카테고리를 선택해주세요", ""));
    state.categories.forEach(category => select.append(new Option(category.name, category.id)));
    select.value = currentValue;
}

async function loadHomeItems(params = { page: 0, size: 20 }) {
    try {
        const data = await apiClient.listItems(params);
        const content = data?.content || [];
        if (content.length > 0) {
            state.products = content.map(normalizeProduct);
        }
    } catch {
        // 백엔드가 꺼져 있으면 초기 화면은 조용히 샘플 데이터로 유지한다.
    }
}

async function renderHome() {
    mount("homeTemplate");
    const grid = document.querySelector("#productGrid");
    grid.replaceChildren(...state.products.map(productCard));
    bindProductActions();
    document.querySelector("#searchForm").addEventListener("submit", handleSearch);
    document.querySelector("#loadMoreButton").addEventListener("click", () => loadMoreItems());
    await loadCategories();
    await loadHomeItems();
    grid.replaceChildren(...state.products.map(productCard));
    bindProductActions();
}

async function loadMoreItems() {
    try {
        const data = await apiClient.listItems({ page: Math.floor(state.products.length / 20), size: 20 });
        const content = data?.content || [];
        if (content.length === 0) {
            showToast("더 보여줄 상품이 없습니다.");
            return;
        }
        state.products = [...state.products, ...content.map(normalizeProduct)];
        document.querySelector("#productGrid").replaceChildren(...state.products.map(productCard));
        bindProductActions();
    } catch {
        showToast("더 보여줄 샘플 상품이 없습니다.");
    }
}

async function handleSearch(event) {
    event.preventDefault();
    const keyword = new FormData(event.currentTarget).get("keyword")?.toString().trim() || "";
    try {
        const data = await apiClient.searchItems({ keyword, page: 0, size: 20, sort: "createdAt,desc" });
        const content = data?.content || [];
        if (content.length > 0) {
            state.products = content.map(normalizeProduct);
        }
        showToast(keyword ? `"${keyword}" 검색 결과를 불러왔습니다.` : "전체 상품을 불러왔습니다.");
    } catch {
        showToast("검색 API 연결 전이라 샘플 상품으로 검색합니다.");
    }
    const lowered = keyword.toLowerCase();
    const visible = state.products.filter(product => {
        const title = product.title?.toLowerCase() || "";
        const category = product.categoryName?.toLowerCase() || "";
        return !lowered || title.includes(lowered) || category.includes(lowered);
    });
    document.querySelector("#productGrid").replaceChildren(...visible.map(productCard));
    bindProductActions();
}

function renderAuth() {
    mount("authTemplate");
    document.querySelector("#loginForm").addEventListener("submit", handleLogin);
    document.querySelector("#signupForm").addEventListener("submit", handleSignup);
}

async function handleLogin(event) {
    event.preventDefault();
    const body = Object.fromEntries(new FormData(event.currentTarget));
    try {
        await apiClient.login(body);
        showToast("로그인되었습니다.");
        location.hash = "profile";
    } catch (error) {
        showToast(error.message || "로그인 정보를 확인해주세요.");
    }
}

async function handleSignup(event) {
    event.preventDefault();
    const body = Object.fromEntries(new FormData(event.currentTarget));
    if (body.password !== body.passwordConfirm) {
        showToast("비밀번호 확인이 일치하지 않습니다.");
        return;
    }
    delete body.passwordConfirm;
    try {
        await apiClient.signup(body);
        showToast("회원가입이 완료되었습니다. 로그인해주세요.");
        event.currentTarget.reset();
    } catch (error) {
        showToast(error.message || "회원가입 요청을 확인해주세요.");
    }
}

function myProductCard(product, index) {
    const article = document.createElement("article");
    article.className = "my-product-card";
    const status = index === 3 ? "임시저장" : statusLabel(product.tradeStatus);
    article.innerHTML = `
        <img src="${productImage(product)}" alt="${escapeHtml(product.title)}">
        <div class="my-product-body">
            <span class="status-badge">${status}</span>
            <p class="product-meta">${escapeHtml(product.categoryName || "중고")}</p>
            <p class="product-title">${escapeHtml(product.title)}</p>
            <strong>${formatPrice(product.currentBid || product.initialPrice)}</strong>
            <p class="product-meta">${timeAgo(product.createdAt)} · ♡ ${product.likeCount}</p>
            <div class="card-actions">
                <button class="secondary tiny" type="button" data-route="edit" data-edit="${productId(product)}">수정</button>
                <button class="primary tiny" type="button" data-status="${productId(product)}">${product.tradeStatus === "SOLD_OUT" ? "거래완료" : "상태 변경"}</button>
                ${product.tradeStatus === "SOLD_OUT" ? `<button class="secondary tiny" type="button" data-review="${productId(product)}">리뷰 작성</button>` : ""}
            </div>
        </div>
    `;
    return article;
}

async function renderProfile() {
    mount("profileTemplate");
    const list = document.querySelector("#profileProducts");
    list.replaceChildren(...state.products.slice(0, 4).map(myProductCard));
    bindRouteButtons();
    bindProfileActions();
    try {
        const me = await apiClient.getMe();
        paintProfile(me);
    } catch {
        showToast("로그인 후 마이페이지 API를 사용할 수 있습니다.");
    }
    try {
        const data = await apiClient.listMyItems({ page: 0, size: 20 });
        const content = data?.content || [];
        if (content.length > 0) {
            list.replaceChildren(...content.map(normalizeProduct).map(myProductCard));
            bindRouteButtons();
            bindProfileActions();
        }
    } catch {
        // docs/api.md에는 있지만 현재 백엔드 컨트롤러에는 아직 없는 API다.
    }
    apiClient.listChatRooms({ page: 0, size: 20 }).catch(() => {});
    apiClient.listMyLikes({ page: 0, size: 20 }).catch(() => {});
}

function paintProfile(me) {
    document.querySelector(".profile-title h1").textContent = me.nickname || "배추도사";
    document.querySelector(".profile-main p").textContent = `가입일: ${(me.createdAt || "").slice(0, 10) || "확인 중"}`;
    const image = document.querySelector(".profile-image");
    if (me.profileImageUrl) image.src = me.profileImageUrl;
}

function bindProfileActions() {
    document.querySelectorAll("[data-status]").forEach(button => {
        button.addEventListener("click", () => updateStatus(button.dataset.status));
    });
    document.querySelectorAll("[data-review]").forEach(button => {
        button.addEventListener("click", () => createReview(button.dataset.review));
    });
}

async function updateStatus(itemId) {
    const product = state.products.find(item => String(productId(item)) === String(itemId));
    const nextStatus = product?.tradeStatus === "ON_SALE" ? "RESERVED" : "SOLD_OUT";
    const payload = { tradeStatus: nextStatus };
    if (nextStatus === "SOLD_OUT") {
        const buyerId = window.prompt("구매자 ID를 입력하세요. 비워두면 상태만 변경합니다.");
        if (buyerId) payload.buyerId = Number(buyerId);
    }
    try {
        await apiClient.updateItemStatus(itemId, payload);
        showToast("판매 상태를 변경했습니다.");
    } catch (error) {
        showToast(error.message || "판매 상태 변경에 실패했습니다.");
    }
}

async function createReview(itemId) {
    const rating = Number(window.prompt("평점을 입력하세요. (1-5)", "5"));
    if (!rating) return;
    const content = window.prompt("리뷰 내용을 입력하세요.", "좋은 거래였습니다.") || "";
    try {
        await apiClient.createReview(itemId, { rating, content });
        showToast("리뷰를 등록했습니다.");
    } catch (error) {
        showToast(error.message || "리뷰 등록에 실패했습니다.");
    }
}

async function renderForm(mode = "sell") {
    mount("formTemplate");
    await loadCategories();
    hydrateCategorySelect();
    const formTitle = document.querySelector("#formTitle");
    const submitButton = document.querySelector("#submitItemButton");
    const form = document.querySelector("#itemForm");
    form.dataset.mode = mode;
    if (mode === "edit") {
        formTitle.textContent = "상품 정보 수정";
        submitButton.textContent = "수정 완료";
        const product = state.products[3];
        state.currentItemId = productId(product);
        form.title.value = product.title;
        form.categoryId.value = state.categories[0]?.id || "1";
        form.initialPrice.value = product.initialPrice;
        form.description.value = "구매한 지 한 달 정도 된 커스텀 키보드입니다.\n사무실에서 사용하려고 샀는데 타건음이 생각보다 커서 내놓습니다.\n직거래는 강남역 인근에서 가능합니다.";
        document.querySelector("#photoCount").textContent = "(3/10)";
    }
    form.addEventListener("submit", event => submitItem(event, false));
    document.querySelector("#draftButton").addEventListener("click", event => submitItem(event, true));
    document.querySelector(".upload-box").addEventListener("click", () => document.querySelector("#itemImages").click());
    document.querySelector("#itemImages").addEventListener("change", event => {
        document.querySelector("#photoCount").textContent = `(${event.currentTarget.files.length}/10)`;
    });
    form.querySelectorAll(".choice input").forEach(input => {
        input.addEventListener("change", () => {
            document.querySelectorAll(`input[name="${input.name}"]`).forEach(peer => peer.closest(".choice").classList.toggle("active", peer.checked));
            document.querySelector("#closeDateLabel").classList.toggle("hidden", form.tradeType.value !== "AUCTION");
        });
    });
    bindRouteButtons();
}

function normalizeItemPayload(form, draft) {
    const body = Object.fromEntries(new FormData(form));
    delete body.files;
    body.categoryId = Number(body.categoryId);
    body.initialPrice = Number(body.initialPrice);
    if (!body.closeDate && (!draft || body.tradeType === "AUCTION")) {
        body.closeDate = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().slice(0, 19);
    }
    if (!body.closeDate) delete body.closeDate;
    return body;
}

async function submitItem(event, draft) {
    event.preventDefault();
    const form = document.querySelector("#itemForm");
    const body = normalizeItemPayload(form, draft);
    const files = document.querySelector("#itemImages").files;
    try {
        let result;
        if (form.dataset.mode === "edit") {
            result = await apiClient.updateItem(state.currentItemId, body);
        } else {
            result = draft ? await apiClient.createDraft(body) : await apiClient.createItem(body);
        }
        const itemId = typeof result === "number" ? result : result?.itemId || state.currentItemId;
        if (itemId && files.length > 0) {
            await apiClient.uploadItemImages(itemId, files);
        }
        showToast(draft ? "임시저장했습니다." : "상품을 저장했습니다.");
        location.hash = "profile";
    } catch (error) {
        showToast(error.message || "상품 저장 요청에 실패했습니다.");
    }
}

async function renderDetail(id = 1) {
    mount("detailTemplate");
    state.currentItemId = id;
    state.currentChatRoomId = null;
    const product = state.products.find(item => String(productId(item)) === String(id)) || state.products[0];
    paintDetail(product);
    bindDetailActions(product);
    try {
        const detail = await apiClient.getItem(id);
        const merged = normalizeProduct({ ...product, ...detail });
        paintDetail(merged);
        bindDetailActions(merged);
    } catch {
        // 공개 상세 API가 실패하면 샘플 상세로 유지한다.
    }
    renderInquiryPanel(id);
}

function paintDetail(product) {
    const images = product.images?.length > 0
        ? product.images.map(image => image.imageUrl)
        : [
            productImage(product),
            "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=720&q=80",
            "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=720&q=80",
            "https://images.unsplash.com/photo-1494438639946-1ebd1d20bf85?auto=format&fit=crop&w=720&q=80",
            "https://images.unsplash.com/photo-1540932239986-30128078f3c5?auto=format&fit=crop&w=720&q=80"
        ];
    document.querySelector("#detailHero").src = images[0];
    document.querySelector("#detailHero").alt = product.title;
    document.querySelector("#detailTitle").textContent = product.title;
    document.querySelector("#detailPrice").textContent = formatPrice(product.currentBid || product.initialPrice);
    document.querySelector("#detailMeta").textContent = `♡ ${product.likeCount} · 조회 ${product.viewCount || 0} · ${timeAgo(product.createdAt)}`;
    document.querySelector("#detailDescription").innerHTML = `
        <p>${escapeHtml(product.description)}</p>
        <ul>
            <li>거래 전 상품 상태와 구성품을 꼼꼼히 확인해주세요.</li>
            <li>직거래는 사람이 많은 장소를 권장합니다.</li>
            <li>경매 상품은 현재가보다 높은 금액으로 입찰할 수 있습니다.</li>
        </ul>
        <p>더 궁금한 점은 오른쪽 채팅으로 편하게 말씀해주세요.</p>
    `;
    document.querySelector("#thumbs").replaceChildren(...images.map(src => {
        const button = document.createElement("button");
        button.type = "button";
        button.innerHTML = `<img src="${src}" alt="상품 추가 이미지">`;
        button.addEventListener("click", () => document.querySelector("#detailHero").src = src);
        return button;
    }));
    document.querySelector(".purchase-box span").textContent = product.title;
    document.querySelector(".purchase-box strong").textContent = formatPrice(product.currentBid || product.initialPrice);
}

function bindDetailActions(product) {
    const bidButton = document.querySelector("#bidButton");
    bidButton.replaceWith(bidButton.cloneNode(true));
    document.querySelector("#bidButton").addEventListener("click", async () => {
        try {
            if (product.tradeType === "AUCTION") {
                await apiClient.bidItem(productId(product), {
                    bidPrice: Number(product.currentBid || product.initialPrice) + 1000
                });
                showToast("입찰 요청을 보냈습니다.");
            } else {
                const room = await apiClient.createChatRoom(productId(product));
                state.currentChatRoomId = room?.roomId || room?.id || state.currentChatRoomId;
                showToast("채팅방을 만들었습니다.");
                if (state.currentChatRoomId) await loadChatMessages(state.currentChatRoomId);
            }
        } catch (error) {
            showToast(error.message || "구매 요청에 실패했습니다.");
        }
    });

    const form = document.querySelector("#messageForm");
    form.replaceWith(form.cloneNode(true));
    document.querySelector("#messageForm").addEventListener("submit", event => sendMessage(event, product));
}

async function loadChatMessages(chatRoomId) {
    try {
        const data = await apiClient.listChatMessages(chatRoomId, { page: 0, size: 50 });
        const messages = data?.content || [];
        const chatWindow = document.querySelector("#chatWindow");
        chatWindow.replaceChildren();
        messages.forEach(message => appendChatBubble(message.content, true));
    } catch {
        // 채팅방이 아직 없거나 인증 전이면 기본 샘플 대화를 유지한다.
    }
}

async function sendMessage(event, product) {
    event.preventDefault();
    const input = event.currentTarget.message;
    const content = input.value.trim();
    if (!content) return;
    try {
        if (!state.currentChatRoomId) {
            const room = await apiClient.createChatRoom(productId(product));
            state.currentChatRoomId = room?.roomId || room?.id;
        }
        if (state.currentChatRoomId) {
            await sendStompMessage(state.currentChatRoomId, content);
        }
    } catch {
        // 서버 연결 전에도 입력 경험은 유지한다.
    }
    appendChatBubble(content, true);
    input.value = "";
}

function appendChatBubble(content, mine) {
    const bubble = document.createElement("p");
    bubble.className = mine ? "bubble mine" : "bubble";
    bubble.textContent = content;
    document.querySelector("#chatWindow").append(bubble);
}

let stompSocket = null;
let stompRoomId = null;

function sendFrame(command, headers = {}, body = "") {
    const headerText = Object.entries(headers).map(([key, value]) => `${key}:${value}`).join("\n");
    stompSocket.send(`${command}\n${headerText}\n\n${body}\0`);
}

function chatSocketUrl() {
    const fallbackOrigin = location.protocol.startsWith("http") ? location.origin : "http://localhost:8080";
    const origin = API_BASE ? new URL(API_BASE, fallbackOrigin).origin : fallbackOrigin;
    return origin.replace(/^http/, "ws") + "/ws/chat";
}

function connectStomp(roomId) {
    if (stompSocket?.readyState === WebSocket.OPEN && stompRoomId === roomId) {
        return Promise.resolve(stompSocket);
    }
    return new Promise((resolve, reject) => {
        stompSocket = new WebSocket(chatSocketUrl());
        stompRoomId = roomId;
        stompSocket.addEventListener("open", () => {
            const headers = { "accept-version": "1.2", host: location.host || "localhost" };
            if (state.token) headers.Authorization = `Bearer ${state.token}`;
            sendFrame("CONNECT", headers);
        });
        stompSocket.addEventListener("message", event => {
            if (String(event.data).startsWith("CONNECTED")) {
                sendFrame("SUBSCRIBE", { id: `room-${roomId}`, destination: `/sub/${roomId}/messages` });
                resolve(stompSocket);
                return;
            }
            const body = String(event.data).split("\n\n")[1]?.replace(/\0$/, "");
            if (!body) return;
            try {
                const message = JSON.parse(body);
                if (message.content) appendChatBubble(message.content, false);
            } catch {
                appendChatBubble(body, false);
            }
        });
        stompSocket.addEventListener("error", reject, { once: true });
    });
}

async function sendStompMessage(roomId, content) {
    await connectStomp(roomId);
    sendFrame("SEND", {
        destination: `/pub/${roomId}/messages`,
        "content-type": "application/json"
    }, JSON.stringify({ content, contentType: "TEXT" }));
}

async function renderInquiryPanel(itemId) {
    const host = document.querySelector("#detailDescription");
    const panel = document.createElement("section");
    panel.className = "inquiry-panel";
    panel.innerHTML = `
        <h2>상품 문의</h2>
        <form class="inquiry-form" id="inquiryForm">
            <input name="title" type="text" placeholder="문의 제목" required>
            <textarea name="contents" rows="3" placeholder="문의 내용을 입력하세요." required></textarea>
            <button class="primary" type="submit">문의 등록</button>
        </form>
        <div id="inquiryList" class="inquiry-list"></div>
    `;
    host.append(panel);
    document.querySelector("#inquiryForm").addEventListener("submit", async event => {
        event.preventDefault();
        const body = Object.fromEntries(new FormData(event.currentTarget));
        try {
            await apiClient.createInquiry(itemId, body);
            showToast("문의를 등록했습니다.");
            event.currentTarget.reset();
            await loadInquiries(itemId);
        } catch (error) {
            showToast(error.message || "문의 등록에 실패했습니다.");
        }
    });
    await loadInquiries(itemId);
}

async function loadInquiries(itemId) {
    const list = document.querySelector("#inquiryList");
    if (!list) return;
    try {
        const data = await apiClient.listInquiries(itemId, { page: 0, size: 5 });
        const inquiries = data?.itemList || data?.content || [];
        if (inquiries.length === 0) {
            list.innerHTML = "<p class=\"product-meta\">등록된 문의가 없습니다.</p>";
            return;
        }
        list.replaceChildren(...inquiries.map(inquiry => {
            const article = document.createElement("article");
            article.innerHTML = `
                <strong>${escapeHtml(inquiry.authorName || "문의자")}</strong>
                <p>${escapeHtml(inquiry.contents)}</p>
                <small>${(inquiry.date || "").slice(0, 10)}</small>
            `;
            return article;
        }));
    } catch {
        list.innerHTML = "<p class=\"product-meta\">문의 API는 로그인 후 사용할 수 있습니다.</p>";
    }
}

function bindProductActions() {
    document.querySelectorAll("[data-detail]").forEach(button => {
        button.addEventListener("click", () => {
            location.hash = `detail/${button.dataset.detail}`;
        });
    });
    document.querySelectorAll("[data-like]").forEach(button => {
        button.addEventListener("click", async () => {
            const id = button.dataset.like;
            try {
                const result = await apiClient.toggleLike(id);
                button.textContent = result?.liked === false ? "♡" : "♥";
                showToast("관심상품에 반영했습니다.");
            } catch (error) {
                showToast(error.message || "로그인 후 관심상품을 사용할 수 있습니다.");
            }
        });
    });
}

function bindRouteButtons() {
    document.querySelectorAll("[data-route]").forEach(button => {
        button.addEventListener("click", () => {
            location.hash = button.dataset.route;
        });
    });
}

function updateLoginButton() {
    const login = document.querySelector("#loginNav");
    if (login) {
        login.textContent = state.token ? "로그아웃" : "로그인";
    }
}

function route() {
    const [name, id] = (location.hash || "#home").replace("#", "").split("/");
    document.querySelectorAll(".category-nav a").forEach(link => link.classList.toggle("active", name === "home"));
    if (name === "auth") renderAuth();
    else if (name === "profile") renderProfile();
    else if (name === "sell") renderForm("sell");
    else if (name === "edit") renderForm("edit");
    else if (name === "detail") renderDetail(id);
    else renderHome();
    bindRouteButtons();
    updateLoginButton();
}

document.querySelector("#quickSearchForm").addEventListener("submit", event => {
    event.preventDefault();
    const value = document.querySelector("#quickSearch").value.trim();
    location.hash = "home";
    window.setTimeout(() => {
        const keyword = document.querySelector("#keyword");
        if (keyword) {
            keyword.value = value;
            document.querySelector("#searchForm").dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }));
        }
    }, 0);
});

document.querySelector("#loginNav").addEventListener("click", async () => {
    if (state.token) {
        try {
            await apiClient.logout();
        } catch {
            // 토큰 만료 상태라도 클라이언트 세션은 비운다.
        }
        state.token = "";
        localStorage.removeItem(ACCESS_TOKEN_KEY);
        updateLoginButton();
        showToast("로그아웃되었습니다.");
        location.hash = "home";
    } else {
        location.hash = "auth";
    }
});

window.addEventListener("hashchange", route);
route();
