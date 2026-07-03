import { api } from "../core/api.js";
import { bootstrapShell, createItemCard, emptyState, showToast } from "../core/shell.js";
import { escapeHtml, formatPrice, formatRelativeTime, statusLabel, tradeLabel } from "../core/utils.js";

const state = {
    me: null,
    tab: "items",
    items: [],
    likes: [],
    reviews: []
};

const elements = {
    title: document.querySelector("#myPageTitle"),
    subtitle: document.querySelector("#myPageSubtitle"),
    stats: document.querySelector("#myStats"),
    content: document.querySelector("#myContent")
};

function renderStats() {
    const selling = state.items.filter(item => item.tradeStatus === "ON_SALE").length;
    const sold = state.items.filter(item => item.tradeStatus === "SOLD_OUT").length;
    elements.stats.innerHTML = `
        <article><span>판매중</span><strong>${selling}</strong></article>
        <article><span>거래완료</span><strong>${sold}</strong></article>
        <article><span>관심상품</span><strong>${state.likes.length}</strong></article>
        <article><span>작성리뷰</span><strong>${state.reviews.length}</strong></article>
    `;
}

function renderContent() {
    document.querySelectorAll("[data-tab]").forEach(button => {
        button.classList.toggle("is-active", button.dataset.tab === state.tab);
    });

    if (state.tab === "items") {
        elements.content.innerHTML = state.items.length
            ? state.items.map(item => `
                <article class="list-card list-card--action">
                    <img class="list-card__thumb" src="${item.thumbnailUrl || "https://placehold.co/320x260/f0ebe6/7b7069?text=Baechu"}" alt="${escapeHtml(item.title)}">
                    <div>
                        <p class="eyebrow">${tradeLabel(item.tradeType)} · ${statusLabel(item.tradeStatus)}</p>
                        <h3>${escapeHtml(item.title)}</h3>
                        <p>${formatPrice(item.currentBid ?? item.initialPrice)} · ${formatRelativeTime(item.createdAt)}</p>
                    </div>
                    <div class="hero-actions">
                        <a class="button button--line" href="item.html?id=${item.itemId}">상세보기</a>
                        <a class="button button--dark" href="edit-item.html?id=${item.itemId}">수정하기</a>
                    </div>
                </article>
            `).join("")
            : emptyState("등록한 판매글이 없습니다.");
        return;
    }

    if (state.tab === "likes") {
        elements.content.innerHTML = state.likes.length
            ? state.likes.map(item => `
                <article class="list-card list-card--action">
                    <img class="list-card__thumb" src="${item.thumbnailUrl || "https://placehold.co/320x260/f0ebe6/7b7069?text=Baechu"}" alt="${escapeHtml(item.title)}">
                    <div>
                        <p class="eyebrow">${tradeLabel(item.tradeType)} · ${statusLabel(item.tradeStatus)}</p>
                        <h3>${escapeHtml(item.title)}</h3>
                        <p>${formatPrice(item.currentBid ?? item.initialPrice)} · 관심 ${item.likeCount ?? 0}</p>
                    </div>
                    <div class="hero-actions">
                        <a class="button button--line" href="item.html?id=${item.itemId}">상세보기</a>
                    </div>
                </article>
            `).join("")
            : emptyState("관심 상품이 없습니다.");
        return;
    }

    elements.content.innerHTML = state.reviews.length
        ? state.reviews.map(review => `
            <a href="item.html?id=${review.itemId}" class="review-card-link" style="text-decoration: none; color: inherit; display: block;">
                <article class="review-card">
                    <p class="eyebrow">WRITTEN REVIEW</p>
                    <h3>${escapeHtml(review.revieweeNickname)}님께 작성한 리뷰</h3>
                    <p>상품 ID ${review.itemId}</p>
                    <p>${escapeHtml(review.content || "작성 내용 없음")}</p>
                    <time>${formatRelativeTime(review.createdAt)}</time>
                </article>
            </a>
        `).join("")
        : emptyState("작성한 리뷰가 없습니다.");
}

function bindEvents() {
    document.addEventListener("click", event => {
        const tabButton = event.target.closest("[data-tab]");
        if (!tabButton) {
            return;
        }
        state.tab = tabButton.dataset.tab;
        renderContent();
    });
}

async function bootstrap() {
    const shell = await bootstrapShell({ requireAuth: true });
    if (!shell.me) {
        return;
    }
    state.me = shell.me;
    elements.title.textContent = `${shell.me.nickname}님의 거래 관리`;
    elements.subtitle.textContent = `${shell.me.email} · ${shell.me.phone}`;
    bindEvents();

    try {
        const [items, likes, reviews] = await Promise.all([
            api.listMyItems({ page: 0, size: 12 }),
            api.listMyLikes({ page: 0, size: 12 }),
            api.listWrittenReviews({ page: 0, size: 12 })
        ]);
        state.items = items.content ?? [];
        state.likes = likes.content ?? [];
        state.reviews = reviews.content ?? [];
        renderStats();
        renderContent();
    } catch (error) {
        showToast(error.message, true);
        elements.content.innerHTML = emptyState(error.message);
    }
}

bootstrap();
