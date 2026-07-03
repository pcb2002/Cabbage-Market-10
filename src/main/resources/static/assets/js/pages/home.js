import { api } from "../core/api.js";
import { bootstrapShell, createItemCard, emptyState, showToast } from "../core/shell.js";

const state = {
    me: null,
    categories: [],
    items: [],
    page: 0,
    size: 9,
    last: false,
    categoryId: "",
    tradeStatus: "",
    keyword: "",
    likedOnly: false
};

const elements = {
    heroAuthLabel: document.querySelector("#heroAuthLabel"),
    heroItemCount: document.querySelector("#heroItemCount"),
    categoryFilter: document.querySelector("#categoryFilter"),
    itemGrid: document.querySelector("#itemGrid"),
    feedMeta: document.querySelector("#feedMeta"),
    searchForm: document.querySelector("#searchForm"),
    keywordInput: document.querySelector("#keywordInput"),
    loadMoreButton: document.querySelector("#loadMoreButton"),
    likedOnlyInput: document.querySelector("#likedOnlyInput")
};

function renderCategories() {
    const chips = [{ id: "", name: "전체" }, ...state.categories.map(category => ({
        id: String(category.id),
        name: category.name
    }))];

    elements.categoryFilter.innerHTML = chips.map(chip => `
        <button type="button" class="category-pill ${state.categoryId === chip.id ? "is-active" : ""}" data-category-id="${chip.id}">
            ${chip.name}
        </button>
    `).join("");
}

function renderItems() {
    elements.heroItemCount.textContent = `${state.items.length}개 표시`;

    if (!state.items.length) {
        elements.itemGrid.innerHTML = emptyState("조건에 맞는 상품이 없습니다.");
        elements.feedMeta.textContent = "표시할 상품이 없습니다.";
        elements.loadMoreButton.disabled = true;
        return;
    }

    elements.itemGrid.innerHTML = state.items.map(item => createItemCard(item, { showFavorite: true })).join("");
    const keywordText = state.keyword ? `검색어 "${state.keyword}"` : "전체";
    const likedText = state.likedOnly ? " · 좋아요만" : "";
    elements.feedMeta.textContent = `${state.items.length}개 상품 · ${keywordText}${likedText}`;
    elements.loadMoreButton.disabled = state.last;
}

async function loadCategories() {
    const response = await api.listCategories({ page: 0, size: 50 });
    state.categories = (response.content ?? []).filter(category => category.isActive !== false);
    renderCategories();
}

async function loadItems(reset = false) {
    if (reset) {
        state.page = 0;
        state.items = [];
        state.last = false;
    }

    if (state.last) {
        return;
    }

    const params = { page: state.page, size: state.size };
    if (state.categoryId) {
        params.categoryId = state.categoryId;
    }
    if (state.tradeStatus) {
        params.tradeStatus = state.tradeStatus;
    }

    if (state.keyword) {
        params.keyword = state.keyword;
    }
    if (state.likedOnly && state.me) {
        params.likedOnly = true;
    }

    const response = await api.searchItems(params);

    const content = response.content ?? [];
    state.items = reset ? content : state.items.concat(content);
    state.last = response.totalPages ? state.page >= response.totalPages - 1 : true;
    state.page += 1;
    renderItems();
}

function bindEvents() {
    document.addEventListener("click", async event => {
        const categoryButton = event.target.closest("[data-category-id]");
        if (categoryButton) {
            state.categoryId = categoryButton.dataset.categoryId;
            renderCategories();
            try {
                await loadItems(true);
            } catch (error) {
                showToast(error.message, true);
            }
        }

        const statusButton = event.target.closest("[data-status]");
        if (statusButton) {
            state.tradeStatus = statusButton.dataset.status;
            document.querySelectorAll("[data-status]").forEach(button => {
                button.classList.toggle("is-active", button.dataset.status === state.tradeStatus);
            });
            try {
                await loadItems(true);
            } catch (error) {
                showToast(error.message, true);
            }
        }

        const likeButton = event.target.closest("[data-like-item-id]");
        if (likeButton) {
            event.preventDefault();
            event.stopPropagation();
            if (!state.me) {
                showToast("로그인 후 좋아요할 수 있습니다.", true);
                return;
            }
            try {
                await api.toggleLike(likeButton.dataset.likeItemId);
                await loadItems(true);
            } catch (error) {
                showToast(error.message, true);
            }
        }
    });

    elements.searchForm.addEventListener("submit", async event => {
        event.preventDefault();
        state.keyword = elements.keywordInput.value.trim();
        try {
            await loadItems(true);
        } catch (error) {
            showToast(error.message, true);
        }
    });

    elements.loadMoreButton.addEventListener("click", async () => {
        try {
            await loadItems(false);
        } catch (error) {
            showToast(error.message, true);
        }
    });

    elements.likedOnlyInput?.addEventListener("change", async event => {
        state.likedOnly = Boolean(event.target.checked);
        try {
            await loadItems(true);
        } catch (error) {
            showToast(error.message, true);
        }
    });
}

async function bootstrap() {
    const shell = await bootstrapShell();
    state.me = shell.me;
    if (shell.me) {
        elements.heroAuthLabel.textContent = `${shell.me.nickname}님`;
        elements.heroAuthLabel.classList.remove("hidden");
    } else {
        elements.heroAuthLabel.textContent = "";
        elements.heroAuthLabel.classList.add("hidden");
    }
    if (!shell.me && elements.likedOnlyInput) {
        elements.likedOnlyInput.disabled = true;
        const checkLine = elements.likedOnlyInput.closest(".check-line");
        if (checkLine) {
            checkLine.classList.add("hidden");
        }
    }
    // URL 쿼리 파라미터로부터 keyword 추출 및 바인딩
    const urlParams = new URLSearchParams(window.location.search);
    const queryKeyword = urlParams.get("keyword");
    if (queryKeyword) {
        state.keyword = queryKeyword.trim();
        if (elements.keywordInput) {
            elements.keywordInput.value = state.keyword;
        }
    }

    bindEvents();
    try {
        await Promise.all([loadCategories(), loadItems(true)]);
    } catch (error) {
        showToast(error.message, true);
    }
}

bootstrap();
