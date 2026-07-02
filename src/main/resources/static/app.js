const API_BASE = window.BAECHU_API_BASE ||
    localStorage.getItem("baechuApiBase") ||
    "";
const ACCESS_TOKEN_KEY = "cabbageAccessToken";

const state = {
    token: localStorage.getItem(ACCESS_TOKEN_KEY) || "",
    categories: [],
    currentItemId: null,
    currentClientId: null,
    currentChatRoomId: null,
    products: []
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
    listClientReviews: clientId => request(`/api/clients/${clientId}/reviews`),
    createReview: (itemId, payload) => request(`/api/items/${itemId}/reviews`, { method: "POST", body: JSON.stringify(payload) }),
    listInquiries: (itemId, params) => request(`/api/items/${itemId}/inquiries${buildQuery(params)}`),
    createInquiry: (itemId, payload) => request(`/api/items/${itemId}/inquiries`, { method: "POST", body: JSON.stringify(payload) }),
    updateInquiry: (inquiryId, payload) => request(`/api/inquiries/${inquiryId}`, { method: "PUT", body: JSON.stringify(payload) }),
    deleteInquiry: inquiryId => request(`/api/inquiries/${inquiryId}`, { method: "DELETE" }),
    createInquiryAnswer: (inquiryId, payload) => request(`/api/inquiries/${inquiryId}/answer`, { method: "POST", body: JSON.stringify(payload) }),
    createChatRoom: itemId => request(`/api/chat-rooms/${itemId}`, { method: "POST" }),
    listChatRooms: params => request(`/api/chat-rooms/my${buildQuery(params)}`),
    listChatMessages: (chatRoomId, params) => request(`/api/chat-rooms/${chatRoomId}/messages${buildQuery(params)}`),
    deleteChatMessage: messageId => request(`/api/chat-rooms/${messageId}`, { method: "DELETE" }),
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

function imageId(image) {
    return image?.imageId || image?.id;
}

function normalizeImages(images = []) {
    return images.map(image => ({
        ...image,
        imageId: imageId(image),
        imageUrl: image.imageUrl || image.url,
        isThumbnail: Boolean(image.isThumbnail)
    })).filter(image => image.imageUrl);
}

function normalizeProduct(item, index = 0) {
    const fallback = state.products[index % state.products.length] || {};
    const images = normalizeImages(item.images || fallback.images || []);
    return {
        ...fallback,
        ...item,
        itemId: item.itemId || item.id || fallback.itemId,
        images,
        thumbnailUrl: item.thumbnailUrl || images.find(image => image.isThumbnail)?.imageUrl || images[0]?.imageUrl || fallback.thumbnailUrl,
        categoryName: item.categoryName || fallback.categoryName || "중고",
        categoryId: item.categoryId || fallback.categoryId,
        tradeType: item.tradeType || fallback.tradeType || "DIRECT",
        tradeStatus: item.tradeStatus || fallback.tradeStatus || "ON_SALE",
        conditionType: item.conditionType || fallback.conditionType || "USED",
        isDraft: Boolean(item.isDraft ?? item.draft ?? fallback.isDraft ?? false),
        likeCount: item.likeCount ?? fallback.likeCount ?? 0,
        viewCount: item.viewCount ?? fallback.viewCount ?? 0,
        createdAt: item.createdAt || fallback.createdAt || new Date().toISOString()
    };
}

function replaceProductState(product) {
    const id = String(productId(product));
    const normalized = normalizeProduct(product);
    const exists = state.products.some(item => String(productId(item)) === id);
    state.products = exists
        ? state.products.map(item => String(productId(item)) === id ? normalizeProduct({ ...item, ...product }) : item)
        : [...state.products, normalized];
}

function removeProductState(itemId) {
    state.products = state.products.filter(item => String(productId(item)) !== String(itemId));
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
        state.products = content.map(normalizeProduct);
    } catch (error) {
        state.products = [];
        showToast(error.message || "상품 목록 API 연결에 실패했습니다.");
    }
}

async function renderHome() {
    mount("homeTemplate");
    const grid = document.querySelector("#productGrid");
    document.querySelector("#searchForm").addEventListener("submit", handleSearch);
    document.querySelector("#loadMoreButton").addEventListener("click", () => loadMoreItems());
    renderPopularSearches();
    await loadCategories();
    await loadHomeItems();
    grid.replaceChildren(...state.products.map(productCard));
    bindProductActions();
}

async function renderPopularSearches() {
    try {
        const popular = await apiClient.searchPopular();
        const keywords = (Array.isArray(popular) ? popular : popular?.content || popular?.keywords || [])
            .map(item => item.keyword || item.searchKeyword || item.word || item)
            .filter(Boolean)
            .slice(0, 8);
        if (keywords.length === 0) return;

        const searchBox = document.querySelector(".hero-search");
        const row = document.createElement("div");
        row.className = "popular-searches";
        row.innerHTML = `<span>인기 검색어</span>`;
        keywords.forEach(keyword => {
            const button = document.createElement("button");
            button.type = "button";
            button.textContent = keyword;
            button.addEventListener("click", () => {
                document.querySelector("#keyword").value = keyword;
                document.querySelector("#searchForm").dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }));
            });
            row.append(button);
        });
        searchBox.append(row);
    } catch {
        // 인기 검색어 API가 비어 있거나 인증 전이면 홈 화면만 유지한다.
    }
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
    } catch (error) {
        showToast(error.message || "추가 상품을 불러오지 못했습니다.");
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
    } catch (error) {
        state.products = [];
        showToast(error.message || "검색 API 연결에 실패했습니다.");
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
    const isDraft = Boolean(product.isDraft || product.draft);
    const status = isDraft ? "임시저장" : statusLabel(product.tradeStatus);
    article.innerHTML = `
        <img src="${productImage(product)}" alt="${escapeHtml(product.title)}">
        <div class="my-product-body">
            <span class="status-badge">${status}</span>
            <p class="product-meta">${escapeHtml(product.categoryName || "중고")}</p>
            <p class="product-title">${escapeHtml(product.title)}</p>
            <strong>${formatPrice(product.currentBid || product.initialPrice)}</strong>
            <p class="product-meta">${timeAgo(product.createdAt)} · ♡ ${product.likeCount}</p>
            <div class="card-actions">
                <button class="secondary tiny" type="button" data-detail="${productId(product)}">보기</button>
                <button class="secondary tiny" type="button" data-route="edit" data-edit="${productId(product)}">수정</button>
                ${isDraft ? `<button class="primary tiny" type="button" data-publish="${productId(product)}">게시</button>` : `<button class="primary tiny" type="button" data-status="${productId(product)}">${product.tradeStatus === "SOLD_OUT" ? "거래완료" : "상태 변경"}</button>`}
                <button class="danger-button tiny" type="button" data-delete-item="${productId(product)}">삭제</button>
                ${product.tradeStatus === "SOLD_OUT" ? `<button class="secondary tiny" type="button" data-review="${productId(product)}">리뷰 작성</button>` : ""}
            </div>
        </div>
    `;
    return article;
}

async function renderProfile() {
    mount("profileTemplate");
    const list = document.querySelector("#profileProducts");
    list.replaceChildren();
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
        list.replaceChildren(...content.map(normalizeProduct).map(myProductCard));
        bindRouteButtons();
        bindProfileActions();
    } catch {
        // docs/api.md에는 있지만 현재 백엔드 컨트롤러에는 아직 없는 API다.
    }
    apiClient.listChatRooms({ page: 0, size: 20 }).catch(() => {});
    apiClient.listMyLikes({ page: 0, size: 20 }).catch(() => {});
}

function paintProfile(me) {
    state.currentClientId = me.clientId || me.id || state.currentClientId;
    document.querySelector(".profile-title h1").textContent = me.nickname || "배추도사";
    document.querySelector(".profile-main p").textContent = `가입일: ${(me.createdAt || "").slice(0, 10) || "확인 중"}`;
    const image = document.querySelector(".profile-image");
    if (me.profileImageUrl) image.src = me.profileImageUrl;
}

function bindProfileActions() {
    document.querySelector(".profile-title .secondary")?.addEventListener("click", editMyProfile);
    document.querySelector(".camera-button")?.addEventListener("click", editProfileImage);
    document.querySelector(".verified")?.addEventListener("click", viewClientProfileById);
    document.querySelectorAll("[data-status]").forEach(button => {
        button.addEventListener("click", () => updateStatus(button.dataset.status));
    });
    document.querySelectorAll("[data-publish]").forEach(button => {
        button.addEventListener("click", () => publishDraft(button.dataset.publish));
    });
    document.querySelectorAll("[data-delete-item]").forEach(button => {
        button.addEventListener("click", () => deleteMyItem(button.dataset.deleteItem));
    });
    document.querySelectorAll("[data-review]").forEach(button => {
        button.addEventListener("click", () => createReview(button.dataset.review));
    });
    bindProductActions();
}

async function viewClientProfileById() {
    const clientId = window.prompt("조회할 회원 ID를 입력하세요.");
    if (!clientId) return;
    try {
        const profile = await apiClient.getClientProfile(clientId);
        const reviews = await apiClient.listClientReviews(clientId).catch(() => null);
        const reviewCount = reviews?.content?.length ?? profile.reviewCount ?? 0;
        const message = `${profile.nickname || "회원"} · 평점 ${profile.averageRating ?? "-"} · 후기 ${reviewCount}개`;
        showToast(message);
    } catch (error) {
        showToast(error.message || "회원 프로필 조회에 실패했습니다.");
    }
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
        const result = await apiClient.updateItemStatus(itemId, payload);
        replaceProductState({ itemId, tradeStatus: result?.tradeStatus || nextStatus });
        showToast("판매 상태를 변경했습니다.");
        await renderProfile();
    } catch (error) {
        showToast(error.message || "판매 상태 변경에 실패했습니다.");
    }
}

async function publishDraft(itemId) {
    if (!window.confirm("임시저장 상품을 게시할까요?")) return;
    try {
        await apiClient.publishItem(itemId);
        replaceProductState({ itemId, isDraft: false, tradeStatus: "ON_SALE" });
        showToast("상품을 게시했습니다.");
        await renderProfile();
    } catch (error) {
        showToast(error.message || "상품 게시에 실패했습니다.");
    }
}

async function deleteMyItem(itemId) {
    if (!window.confirm("상품을 삭제할까요? 삭제 후 목록에서 보이지 않습니다.")) return;
    try {
        await apiClient.deleteItem(itemId);
        removeProductState(itemId);
        showToast("상품을 삭제했습니다.");
        await renderProfile();
    } catch (error) {
        showToast(error.message || "상품 삭제에 실패했습니다.");
    }
}

async function editMyProfile() {
    const nickname = window.prompt("닉네임을 입력하세요.", document.querySelector(".profile-title h1")?.textContent || "");
    if (!nickname) return;
    const name = window.prompt("이름을 입력하세요. 비워두면 변경하지 않습니다.", "") || undefined;
    const phone = window.prompt("전화번호를 입력하세요. 비워두면 변경하지 않습니다.", "") || undefined;
    try {
        const me = await apiClient.updateMe({ nickname, name, phone });
        paintProfile(me);
        showToast("프로필을 수정했습니다.");
    } catch (error) {
        showToast(error.message || "프로필 수정에 실패했습니다.");
    }
}

async function editProfileImage() {
    const profileImageUrl = window.prompt("프로필 이미지 URL을 입력하세요.");
    if (!profileImageUrl) return;
    try {
        const me = await apiClient.updateMe({ profileImageUrl });
        paintProfile(me);
        showToast("프로필 이미지를 수정했습니다.");
    } catch (error) {
        showToast(error.message || "프로필 이미지 수정에 실패했습니다.");
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

async function renderForm(mode = "sell", id = null) {
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
        const selectedId = id || state.currentItemId;
        if (!selectedId) {
            showToast("수정할 상품 ID가 없습니다.");
            location.hash = "profile";
            return;
        }
        let product = state.products.find(item => String(productId(item)) === String(selectedId)) || {};
        state.currentItemId = selectedId;
        try {
            const detail = await apiClient.getItem(state.currentItemId);
            product = normalizeProduct({ ...product, ...detail });
            replaceProductState(product);
        } catch (error) {
            showToast(error.message || "상품 상세 API 연결에 실패했습니다.");
            location.hash = "profile";
            return;
        }
        fillItemForm(form, product);
        renderImageManager(product);
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

function fillItemForm(form, product) {
    form.title.value = product.title || "";
    form.categoryId.value = product.categoryId || state.categories.find(category => category.name === product.categoryName)?.id || state.categories[0]?.id || "";
    form.initialPrice.value = product.initialPrice || 0;
    form.description.value = product.description || "";
    form.conditionType.value = product.conditionType || "USED";
    form.tradeType.value = product.tradeType || "DIRECT";
    if (product.closeDate) {
        form.closeDate.value = String(product.closeDate).slice(0, 16);
    }
    form.querySelectorAll(".choice input").forEach(input => {
        input.closest(".choice").classList.toggle("active", input.checked);
    });
    document.querySelector("#closeDateLabel").classList.toggle("hidden", form.tradeType.value !== "AUCTION");
}

function renderImageManager(product) {
    const uploadRow = document.querySelector(".upload-row");
    const oldManager = document.querySelector(".image-manager");
    if (oldManager) oldManager.remove();

    const images = normalizeImages(product.images || []);
    document.querySelector("#photoCount").textContent = `(${images.length}/10)`;
    if (images.length === 0) return;

    const manager = document.createElement("div");
    manager.className = "image-manager";
    manager.innerHTML = `<p class="product-meta">등록된 이미지</p>`;
    images.forEach(image => {
        const card = document.createElement("article");
        card.className = "image-manager-card";
        card.innerHTML = `
            <img src="${image.imageUrl}" alt="등록된 상품 이미지">
            <div>
                <strong>${image.isThumbnail ? "대표 이미지" : "상품 이미지"}</strong>
                <div class="card-actions">
                    <button class="secondary tiny" type="button" data-set-thumbnail="${image.imageId}" ${image.isThumbnail ? "disabled" : ""}>대표 설정</button>
                    <button class="danger-button tiny" type="button" data-delete-image="${image.imageId}">삭제</button>
                </div>
            </div>
        `;
        manager.append(card);
    });
    uploadRow.after(manager);
    bindImageActions(productId(product));
}

function bindImageActions(itemId) {
    document.querySelectorAll("[data-set-thumbnail]").forEach(button => {
        button.addEventListener("click", () => setItemThumbnail(itemId, button.dataset.setThumbnail));
    });
    document.querySelectorAll("[data-delete-image]").forEach(button => {
        button.addEventListener("click", () => deleteItemImage(itemId, button.dataset.deleteImage));
    });
}

async function setItemThumbnail(itemId, imageIdValue) {
    try {
        await apiClient.setThumbnail(itemId, imageIdValue);
        showToast("대표 이미지를 변경했습니다.");
        await renderForm("edit", itemId);
    } catch (error) {
        showToast(error.message || "대표 이미지 설정에 실패했습니다.");
    }
}

async function deleteItemImage(itemId, imageIdValue) {
    if (!window.confirm("이 이미지를 삭제할까요?")) return;
    try {
        await apiClient.deleteImage(itemId, imageIdValue);
        showToast("이미지를 삭제했습니다.");
        await renderForm("edit", itemId);
    } catch (error) {
        showToast(error.message || "이미지 삭제에 실패했습니다.");
    }
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
    try {
        const detail = await apiClient.getItem(id);
        const cachedProduct = state.products.find(item => String(productId(item)) === String(id)) || {};
        const merged = normalizeProduct({ ...cachedProduct, ...detail });
        replaceProductState(merged);
        paintDetail(merged);
        bindDetailActions(merged);
    } catch (error) {
        showToast(error.message || "상품 상세 API 연결에 실패했습니다.");
        location.hash = "home";
        return;
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
        const messages = data?.content || data?.messages || [];
        const chatWindow = document.querySelector("#chatWindow");
        chatWindow.replaceChildren();
        messages.forEach(message => appendChatBubble(message.content, isMineMessage(message), message.messageId || message.id));
        bindChatMessageActions();
    } catch (error) {
        document.querySelector("#chatWindow")?.replaceChildren();
        showToast(error.message || "채팅 메시지를 불러오지 못했습니다.");
    }
}

async function sendMessage(event, product) {
    event.preventDefault();
    const input = event.currentTarget.message;
    const content = input.value.trim();
    if (!content) return;
    input.value = "";
    try {
        if (!state.currentChatRoomId) {
            const room = await apiClient.createChatRoom(productId(product));
            state.currentChatRoomId = room?.roomId || room?.id;
        }
        if (state.currentChatRoomId) {
            await sendStompMessage(state.currentChatRoomId, content);
            return;
        }
    } catch (error) {
        showToast(error.message || "서버 연결 전이라 화면에만 메시지를 표시합니다.");
    }
    appendChatBubble(content, true);
}

function isMineMessage(message) {
    if (message.mine !== undefined || message.isMine !== undefined) {
        return Boolean(message.mine ?? message.isMine);
    }
    return state.currentClientId != null && String(message.senderId) === String(state.currentClientId);
}

function appendChatBubble(content, mine, messageId = null) {
    const bubble = document.createElement("p");
    bubble.className = mine ? "bubble mine" : "bubble";
    bubble.textContent = content;
    if (messageId && mine) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "message-delete";
        button.dataset.deleteMessage = messageId;
        button.textContent = "삭제";
        bubble.append(button);
    }
    document.querySelector("#chatWindow").append(bubble);
}

function bindChatMessageActions() {
    document.querySelectorAll("[data-delete-message]").forEach(button => {
        button.addEventListener("click", async () => {
            try {
                await apiClient.deleteChatMessage(button.dataset.deleteMessage);
                button.closest(".bubble")?.remove();
                showToast("메시지를 삭제했습니다.");
            } catch (error) {
                showToast(error.message || "메시지 삭제에 실패했습니다.");
            }
        });
    });
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
                if (message.content) appendChatBubble(message.content, isMineMessage(message), message.messageId || message.id);
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
            const inquiryId = inquiry.inquiryID || inquiry.inquiryId || inquiry.id;
            article.innerHTML = `
                <strong>${escapeHtml(inquiry.authorName || "문의자")}</strong>
                <p>${escapeHtml(inquiry.contents)}</p>
                <small>${(inquiry.date || "").slice(0, 10)}</small>
                ${inquiryId ? `
                    <div class="card-actions">
                        <button class="secondary tiny" type="button" data-update-inquiry="${inquiryId}">수정</button>
                        <button class="danger-button tiny" type="button" data-delete-inquiry="${inquiryId}">삭제</button>
                        <button class="primary tiny" type="button" data-answer-inquiry="${inquiryId}">답변</button>
                    </div>
                ` : ""}
            `;
            return article;
        }));
        bindInquiryActions(itemId);
    } catch {
        list.innerHTML = "<p class=\"product-meta\">문의 API는 로그인 후 사용할 수 있습니다.</p>";
    }
}

function bindInquiryActions(itemId) {
    document.querySelectorAll("[data-update-inquiry]").forEach(button => {
        button.addEventListener("click", () => updateInquiry(itemId, button.dataset.updateInquiry));
    });
    document.querySelectorAll("[data-delete-inquiry]").forEach(button => {
        button.addEventListener("click", () => deleteInquiry(itemId, button.dataset.deleteInquiry));
    });
    document.querySelectorAll("[data-answer-inquiry]").forEach(button => {
        button.addEventListener("click", () => createInquiryAnswer(itemId, button.dataset.answerInquiry));
    });
}

async function updateInquiry(itemId, inquiryIdValue) {
    const contents = window.prompt("수정할 문의 내용을 입력하세요.");
    if (!contents) return;
    const title = window.prompt("문의 제목을 입력하세요. 비워두면 기존 제목을 유지합니다.", "") || undefined;
    try {
        await apiClient.updateInquiry(inquiryIdValue, { title, contents });
        showToast("문의를 수정했습니다.");
        await loadInquiries(itemId);
    } catch (error) {
        showToast(error.message || "문의 수정에 실패했습니다.");
    }
}

async function deleteInquiry(itemId, inquiryIdValue) {
    if (!window.confirm("문의를 삭제할까요?")) return;
    try {
        await apiClient.deleteInquiry(inquiryIdValue);
        showToast("문의를 삭제했습니다.");
        await loadInquiries(itemId);
    } catch (error) {
        showToast(error.message || "문의 삭제에 실패했습니다.");
    }
}

async function createInquiryAnswer(itemId, inquiryIdValue) {
    const contents = window.prompt("답변 내용을 입력하세요.");
    if (!contents) return;
    const title = window.prompt("답변 제목을 입력하세요.", "답변") || "답변";
    try {
        await apiClient.createInquiryAnswer(inquiryIdValue, { title, contents });
        showToast("답변을 등록했습니다.");
        await loadInquiries(itemId);
    } catch (error) {
        showToast(error.message || "답변 등록에 실패했습니다.");
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
            location.hash = button.dataset.edit ? `${button.dataset.route}/${button.dataset.edit}` : button.dataset.route;
        });
    });
}

function updateLoginButton() {
    const login = document.querySelector("#loginNav");
    if (login) {
        login.textContent = state.token ? "로그아웃" : "로그인";
    }
}

async function restoreSession() {
    if (!state.token) return;
    try {
        await apiClient.refresh();
    } catch {
        state.token = "";
        localStorage.removeItem(ACCESS_TOKEN_KEY);
    }
}

function route() {
    const [name, id] = (location.hash || "#home").replace("#", "").split("/");
    document.querySelectorAll(".category-nav a").forEach(link => link.classList.toggle("active", name === "home"));
    if (name === "auth") renderAuth();
    else if (name === "profile") renderProfile();
    else if (name === "sell") renderForm("sell");
    else if (name === "edit") renderForm("edit", id);
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
restoreSession().finally(route);
