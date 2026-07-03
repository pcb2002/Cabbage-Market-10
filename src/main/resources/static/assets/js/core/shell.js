import { api, clearToken, getToken } from "./api.js";
import { escapeHtml, formatPrice, formatRelativeTime, statusLabel, tradeLabel } from "./utils.js";

const PENDING_LOGIN_EMAIL_KEY = "baechuPendingLoginEmail";

let toastTimer = null;

export function showToast(message, isError = false) {
    const toast = document.querySelector("#toast");
    if (!toast) {
        return;
    }
    clearTimeout(toastTimer);
    toast.textContent = message;
    toast.style.background = isError ? "rgba(179, 38, 30, 0.94)" : "rgba(17, 17, 17, 0.92)";
    toast.classList.add("is-visible");
    toastTimer = window.setTimeout(() => toast.classList.remove("is-visible"), 2600);
}

export async function bootstrapShell({ requireAuth = false } = {}) {
    const me = await loadSession();
    syncHeader(me);

    document.querySelector("#headerLogoutButton")?.addEventListener("click", async () => {
        try {
            await api.logout();
        } catch {
            // ignore
        }
        clearToken();
        syncHeader(null);
        showToast("로그아웃했습니다.");
        window.location.href = "index.html";
    });

    if (requireAuth && !me) {
        showToast("로그인 후 접근할 수 있습니다.", true);
        window.location.href = "login.html";
        return { me: null };
    }

    return { me };
}

async function loadSession() {
    if (!getToken()) {
        return null;
    }

    try {
        return await api.getMe();
    } catch {
        clearToken();
        return null;
    }
}

function syncHeader(me) {
    const userLabel = document.querySelector("#headerUserLabel");
    const loginLink = document.querySelector("#headerLoginLink");
    const logoutButton = document.querySelector("#headerLogoutButton");

    if (userLabel) {
        if (me) {
            userLabel.textContent = `${me.nickname}님`;
            userLabel.classList.remove("hidden");
        } else {
            userLabel.textContent = "";
            userLabel.classList.add("hidden");
        }
    }
    if (loginLink) {
        loginLink.classList.toggle("hidden", Boolean(me));
    }
    if (logoutButton) {
        logoutButton.classList.toggle("hidden", !me);
    }
}

export function savePendingLoginEmail(email) {
    sessionStorage.setItem(PENDING_LOGIN_EMAIL_KEY, email);
}

export function getPendingLoginEmail() {
    return sessionStorage.getItem(PENDING_LOGIN_EMAIL_KEY) || "";
}

export function clearPendingLoginEmail() {
    sessionStorage.removeItem(PENDING_LOGIN_EMAIL_KEY);
}

export function emptyState(message) {
    return `<div class="empty-box">${escapeHtml(message)}</div>`;
}

export function isAuctionClosed(item) {
    return Boolean(item?.closeDate && new Date(item.closeDate).getTime() <= Date.now() && item.tradeStatus !== "SOLD_OUT");
}

function resolveItemStatus(item) {
    return isAuctionClosed(item) ? "경매마감" : statusLabel(item.tradeStatus);
}

export function createItemCard(item, options = {}) {
    const showFavorite = options.showFavorite !== false;
    const liked = Boolean(item.likedByMe);
    return `
        <article class="item-card">
            ${showFavorite ? `
                <button type="button"
                        class="favorite-button ${liked ? "is-liked" : ""}"
                        data-like-item-id="${item.itemId}"
                        aria-label="좋아요">
                    ♥
                </button>
            ` : ""}
            <a class="item-card__link" href="item.html?id=${item.itemId}">
            <img src="${item.thumbnailUrl || "https://placehold.co/800x700/f0ebe6/7b7069?text=Baechu"}" alt="${escapeHtml(item.title)}">
            <div class="item-card__body">
                <p class="item-card__meta">${escapeHtml(item.categoryName ?? "카테고리")} · ${formatRelativeTime(item.createdAt)}</p>
                <h3>${escapeHtml(item.title)}</h3>
                <div class="item-card__price">${formatPrice(item.currentBid ?? item.initialPrice)}</div>
                <div class="item-card__footer">
                    <span>${tradeLabel(item.tradeType)}</span>
                    <span>${resolveItemStatus(item)}</span>
                    <span>관심 ${item.likeCount ?? 0}</span>
                </div>
            </div>
            </a>
        </article>
    `;
}
