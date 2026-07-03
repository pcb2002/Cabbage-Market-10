import { api, getToken } from "../core/api.js";
import { bootstrapShell, emptyState, isAuctionClosed, showToast } from "../core/shell.js";
import { escapeHtml, formatDateTime, formatPrice, formatRelativeTime, statusLabel } from "../core/utils.js";
import { createChatGateway } from "../core/chat.js";

const detail = document.querySelector("#itemDetail");
const reviews = document.querySelector("#sellerReviews");
const purchaseReviewSection = document.querySelector("#purchaseReviewSection");
const purchaseReviewPanel = document.querySelector("#purchaseReviewPanel");
const itemId = new URLSearchParams(window.location.search).get("id");

function inferTradeLabel(item) {
    return item.closeDate ? "경매" : "직거래";
}

function resolveTradeStatusLabel(item) {
    return isAuctionClosed(item) ? "경매마감" : statusLabel(item.tradeStatus);
}

function buildBidSection(item, isOwner) {
    if (!item.closeDate) {
        return "";
    }

    if (isAuctionClosed(item) && isOwner) {
        return `
            <div class="detail-panel bid-panel">
                <p class="eyebrow">AUCTION CLOSED</p>
                <h3>경매 마감되었습니다. 다시 등록하시겠습니까?</h3>
                <p class="hero-copy">마감된 경매 상품은 새 판매글로 다시 등록하는 흐름을 권장합니다.</p>
                <div class="hero-actions">
                    <a class="button button--dark" href="sell.html">새로 등록하기</a>
                </div>
            </div>
        `;
    }

    if (isAuctionClosed(item)) {
        return `
            <div class="detail-panel bid-panel">
                <p class="eyebrow">AUCTION CLOSED</p>
                <h3>경매가 마감되었습니다.</h3>
                <p class="hero-copy">이 상품은 더 이상 비드할 수 없습니다.</p>
            </div>
        `;
    }

    if (isOwner) {
        return `
            <div class="detail-panel bid-panel">
                <p class="eyebrow">AUCTION</p>
                <h3>경매 진행중</h3>
                <p class="hero-copy">판매자는 입찰하지 않고 현재 최고가만 확인합니다.</p>
                <div class="auction-summary">
                    <article><span>현재 최고가</span><strong>${formatPrice(item.currentBid ?? item.initialPrice)}</strong></article>
                    <article><span>마감일</span><strong>${formatDateTime(item.closeDate)}</strong></article>
                </div>
            </div>
        `;
    }

    return `
        <div class="detail-panel bid-panel">
            <p class="eyebrow">PLACE BID</p>
            <h3>경매 입찰</h3>
            <p class="hero-copy">현재 입찰가보다 높은 금액만 비드할 수 있습니다.</p>
            <div class="auction-summary">
                <article><span>현재 최고가</span><strong>${formatPrice(item.currentBid ?? item.initialPrice)}</strong></article>
                <article><span>마감일</span><strong>${formatDateTime(item.closeDate)}</strong></article>
            </div>
            <form id="bidForm" class="bid-form">
                <label>
                    비드 금액
                    <input name="bidPrice" type="number" min="${(item.currentBid ?? item.initialPrice) + 1}" required>
                </label>
                <button type="submit" class="button button--dark button--block">비드하기</button>
            </form>
        </div>
    `;
}

function renderDetail(item, seller, isOwner) {
    const images = item.images?.length
        ? item.images
        : [{ imageUrl: "https://placehold.co/1200x900/f0ebe6/7b7069?text=Baechu" }];

    detail.innerHTML = `
        <div class="detail-gallery">
            <img src="${images[0].imageUrl}" alt="${escapeHtml(item.title)}">
            <div class="thumb-row">
                ${images.slice(1, 4).map(image => `<img src="${image.imageUrl}" alt="${escapeHtml(item.title)}">`).join("") || `<img src="${images[0].imageUrl}" alt="${escapeHtml(item.title)}">`}
            </div>
        </div>
        <div class="detail-side">
            <div>
                <p class="eyebrow">ITEM DETAIL</p>
                <h1>${escapeHtml(item.title)}</h1>
                <p class="detail-meta">${resolveTradeStatusLabel(item)} · ${inferTradeLabel(item)}</p>
                <div class="detail-price">${formatPrice(item.currentBid ?? item.initialPrice)}</div>
                <p class="hero-copy">${escapeHtml(item.description || "등록된 설명이 없습니다.").replace(/\n/g, "<br>")}</p>
            </div>
            <div class="detail-panel">
                <div class="hero-actions">
                    <button id="likeButton" type="button" class="button button--line">관심 등록</button>
                    <button id="chatButton" type="button" class="button button--dark">${isOwner ? "채팅 확인" : "채팅 시작"}</button>
                    ${isOwner ? `<a class="button button--line" href="edit-item.html?id=${item.itemId}">수정하기</a>` : ""}
                </div>
                <p class="detail-meta">조회 ${item.viewCount ?? 0} · 관심 ${item.likeCount ?? 0} · 문의 ${item.inquiryCount ?? 0}</p>
                ${item.closeDate ? `<p class="detail-meta">경매 마감 ${formatDateTime(item.closeDate)}</p>` : ""}
            </div>
            ${buildBidSection(item, isOwner)}
            ${seller ? `
                <div class="detail-panel">
                    <p class="eyebrow">SELLER</p>
                    <h3>${escapeHtml(seller.nickname)}</h3>
                    <div class="seller-summary">
                        <article><span>평점</span><strong>${Number(seller.averageRating ?? 0).toFixed(1)}</strong></article>
                        <article><span>리뷰 수</span><strong>${seller.reviewCount ?? 0}</strong></article>
                        <article><span>판매중</span><strong>${seller.sellingItemCount ?? 0}</strong></article>
                        <article><span>거래완료</span><strong>${seller.soldItemCount ?? 0}</strong></article>
                    </div>
                </div>
            ` : ""}
        </div>
    `;
}

function renderReviews(list) {
    if (!list.length) {
        reviews.innerHTML = emptyState("등록된 리뷰가 없습니다.");
        return;
    }

    reviews.innerHTML = list.map(review => `
        <article class="review-card">
            <p class="eyebrow">RATING ${review.rating}/5</p>
            <h3>${escapeHtml(review.reviewerNickname)}</h3>
            <p>${escapeHtml(review.content || "내용 없음")}</p>
            <time>${formatRelativeTime(review.createdAt)}</time>
        </article>
    `).join("");
}

function renderPurchaseReview(item, shell, writtenReviews) {
    const alreadyReviewed = writtenReviews.some(review => Number(review.itemId) === Number(item.itemId));
    const canOpen = Boolean(shell.me && shell.me.clientId !== item.sellerId && item.tradeStatus === "SOLD_OUT");

    if (!canOpen) {
        purchaseReviewSection.classList.add("hidden");
        purchaseReviewPanel.innerHTML = "";
        return;
    }

    purchaseReviewSection.classList.remove("hidden");

    if (alreadyReviewed) {
        purchaseReviewPanel.innerHTML = `<div class="empty-box">이 상품의 리뷰는 이미 작성하셨습니다.</div>`;
        return;
    }

    purchaseReviewPanel.innerHTML = `
        <form id="purchaseReviewForm" class="panel-form">
            <div class="form-grid">
                <label>
                    평점
                    <select name="rating" required>
                        <option value="5">5점</option>
                        <option value="4">4점</option>
                        <option value="3">3점</option>
                        <option value="2">2점</option>
                        <option value="1">1점</option>
                    </select>
                </label>
            </div>
            <label>
                리뷰 내용
                <textarea name="content" rows="5" maxlength="500" placeholder="거래 경험을 남겨 주세요."></textarea>
            </label>
            <div class="hero-actions">
                <button type="submit" class="button button--dark">리뷰 등록</button>
            </div>
        </form>
    `;

    document.querySelector("#purchaseReviewForm")?.addEventListener("submit", async event => {
        event.preventDefault();
        try {
            const formData = new FormData(event.currentTarget);
            await api.createReview(item.itemId, {
                rating: Number(formData.get("rating")),
                content: formData.get("content")
            });
            showToast("리뷰를 등록했습니다.");
            window.location.reload();
        } catch (error) {
            showToast(error.message, true);
        }
    });
}

async function bootstrap() {
    const shell = await bootstrapShell();
    if (!itemId) {
        detail.innerHTML = emptyState("상품 ID가 없습니다.");
        return;
    }

    try {
        const item = await api.getItem(itemId);
        const seller = item.sellerId ? await api.getClientProfile(item.sellerId).catch(() => null) : null;
        const isOwner = Boolean(shell.me && item.sellerId === shell.me.clientId);
        const writtenReviewsResponse = shell.me
            ? await api.listWrittenReviews({ page: 0, size: 100 }).catch(() => ({ content: [] }))
            : { content: [] };
        const sellerReviewResponse = item.sellerId
            ? await api.listClientReviews(item.sellerId, { page: 0, size: 6 }).catch(() => ({ content: [] }))
            : { content: [] };

        renderDetail(item, seller, isOwner);
        renderReviews(sellerReviewResponse.content ?? []);
        renderPurchaseReview(item, shell, writtenReviewsResponse.content ?? []);

        document.querySelector("#likeButton")?.addEventListener("click", async () => {
            if (!shell.me) {
                showToast("로그인 후 사용할 수 있습니다.", true);
                window.location.href = "login.html";
                return;
            }
            try {
                await api.toggleLike(item.itemId);
                showToast("관심 상태를 변경했습니다.");
                window.location.reload();
            } catch (error) {
                showToast(error.message, true);
            }
        });

        document.querySelector("#chatButton")?.addEventListener("click", async () => {
            if (!shell.me) {
                showToast("로그인 후 사용할 수 있습니다.", true);
                window.location.href = "login.html";
                return;
            }
            if (isOwner) {
                // 판매자: 이 상품의 채팅방 목록 사이드바 열기
                document.dispatchEvent(new CustomEvent("openItemChatSidebar", {
                    detail: { clientId: shell.me.clientId }
                }));
                return;
            }
            // 구매자: 채팅방 생성(또는 기존 방 반환) 후 해당 방 메시지 뷰로 바로 열기
            try {
                const created = await api.createChatRoom(item.itemId);
                document.dispatchEvent(new CustomEvent("openItemChatSidebar", {
                    detail: { clientId: shell.me.clientId, roomId: created.roomId }
                }));
            } catch (error) {
                showToast(error.message, true);
            }
        });

        document.querySelector("#bidForm")?.addEventListener("submit", async event => {
            event.preventDefault();
            if (!shell.me) {
                showToast("로그인 후 사용할 수 있습니다.", true);
                window.location.href = "login.html";
                return;
            }
            try {
                const bidPrice = Number(new FormData(event.currentTarget).get("bidPrice"));
                await api.bidItem(item.itemId, { bidPrice });
                showToast("비드가 등록되었습니다.");
                window.location.reload();
            } catch (error) {
                showToast(error.message, true);
            }
        });
    } catch (error) {
        detail.innerHTML = emptyState(error.message);
        reviews.innerHTML = "";
    }
}

bootstrap();

/* ── 채팅 사이드바 (판매자 전용) ── */
(function initItemChatSidebar() {
    const overlay    = document.getElementById("itemChatOverlay");
    const sidebar    = document.getElementById("itemChatSidebar");
    const closeBtn   = document.getElementById("itemChatClose");
    const roomList   = document.getElementById("itemChatRoomList");
    const msgView    = document.getElementById("itemChatMsgView");
    const messages   = document.getElementById("itemChatMessages");
    const backBtn    = document.getElementById("itemChatBack");
    const partnerEl  = document.getElementById("itemChatPartnerName");
    const openFull   = document.getElementById("itemChatOpenFull");
    const input      = document.getElementById("itemChatInput");
    const sendBtn    = document.getElementById("itemChatSend");

    if (!sidebar) return;

    let activeRoomId = null;
    let gateway = null;
    let myClientId = null;  // openSidebar 이벤트에서 주입
    const pendingSent = new Set(); // 내가 보낸 메시지 추적 (JS-only isMine 판별)

    function escHtml(s) {
        return String(s ?? "").replace(/[&<>"']/g, c =>
            ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
    }

    function fmtTime(iso) {
        if (!iso) return "";
        const d = new Date(iso);
        return d.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" });
    }

    function openSidebar(e) {
        myClientId = e?.detail?.clientId ?? myClientId;
        overlay.classList.add("is-open");
        sidebar.classList.add("is-open");
        document.body.style.overflow = "hidden";
        const roomId = e?.detail?.roomId;
        if (roomId) {
            // 구매자: 특정 방으로 바로 진입
            showMsgView(roomId, "채팅");
        } else {
            // 판매자: 이 상품의 채팅방 목록 표시
            loadRooms();
        }
    }

    function closeSidebar() {
        overlay.classList.remove("is-open");
        sidebar.classList.remove("is-open");
        document.body.style.overflow = "";
        gateway?.disconnect();
        gateway = null;
        activeRoomId = null;
        showRoomList();
    }

    function showRoomList() {
        roomList.classList.remove("hidden");
        msgView.classList.add("hidden");
    }

    function showMsgView(roomId, partnerName) {
        activeRoomId = roomId;
        partnerEl.textContent = partnerName;
        openFull.onclick = () => { window.location.href = `chat.html?roomId=${roomId}`; };
        roomList.classList.add("hidden");
        msgView.classList.remove("hidden");
        loadMessages(roomId);
        connectGateway(roomId);
        input.focus();
    }

    async function loadRooms() {
        roomList.innerHTML = `<div class="item-chat-empty"><span class="icon">⏳</span>불러오는 중…</div>`;
        try {
            const res = await api.listChatRooms({ page: 0, size: 50 });
            const allRooms = res?.content ?? res ?? [];
            const currentItemId = new URLSearchParams(window.location.search).get("id");

            // 이 상품에 대한 채팅방만 필터
            const rooms = allRooms.filter(r =>
                String(r.itemId ?? r.item?.id ?? "") === String(currentItemId)
            );

            if (!rooms.length) {
                roomList.innerHTML = `
                    <div class="item-chat-empty">
                        <span class="icon">💬</span>
                        이 상품에 대한 채팅이 없습니다.
                    </div>`;
                return;
            }

            roomList.innerHTML = `<div class="chat-room-list">` +
                rooms.map(r => {
                    const label = escHtml(r.itemName ?? "구매자");
                    return `
                    <button class="item-chat-room-item" data-room-id="${r.id}" data-partner="${label}">
                        <div class="item-chat-room-item__avatar">👤</div>
                        <div class="item-chat-room-item__info">
                            <div class="item-chat-room-item__name">${label}</div>
                            <div class="item-chat-room-item__last">${escHtml(r.date ? new Date(r.date).toLocaleDateString("ko-KR") : "메시지 없음")}</div>
                        </div>
                        <div class="item-chat-room-item__arrow">›</div>
                    </button>`;
                }).join("") +
            `</div>`;

            roomList.querySelectorAll(".item-chat-room-item").forEach(btn => {
                btn.addEventListener("click", () => {
                    showMsgView(btn.dataset.roomId, btn.dataset.partner);
                });
            });
        } catch (e) {
            roomList.innerHTML = `<div class="item-chat-empty"><span class="icon">⚠️</span>${escHtml(e.message)}</div>`;
        }
    }

    async function loadMessages(roomId) {
        messages.innerHTML = `<div class="item-chat-empty"><span class="icon">⏳</span>메시지 불러오는 중…</div>`;
        try {
            const res = await api.listChatMessages(roomId, { page: 0, size: 50 });
            const list = res?.content ?? res ?? [];
            renderMessages(list);
        } catch (e) {
            messages.innerHTML = `<div class="item-chat-empty"><span class="icon">⚠️</span>${escHtml(e.message)}</div>`;
        }
    }

    function renderMessages(list) {
        if (!list.length) {
            messages.innerHTML = `<div class="item-chat-empty"><span class="icon">💬</span>아직 메시지가 없습니다.</div>`;
            return;
        }
        messages.innerHTML = list.map(m => {
            const isMine = myClientId ? (Number(m.senderId) === Number(myClientId)) : (m.isMine ?? false);
            return `
            <div class="item-chat-bubble ${isMine ? "mine" : ""}">
                ${escHtml(m.content ?? m.message ?? "")}
                <div class="item-chat-bubble__time">${fmtTime(m.createdAt ?? m.sentAt)}</div>
            </div>`;
        }).join("");
        scrollToBottom();
    }

    function appendMessage(content, isMine) {
        const existing = messages.querySelector(".item-chat-empty");
        if (existing) existing.remove();

        const div = document.createElement("div");
        div.className = `item-chat-bubble${isMine ? " mine" : ""}`;
        div.innerHTML = `${escHtml(content)}<div class="item-chat-bubble__time">${fmtTime(new Date().toISOString())}</div>`;
        messages.appendChild(div);
        scrollToBottom();
    }

    function scrollToBottom() {
        setTimeout(() => { messages.scrollTop = messages.scrollHeight; }, 50);
    }

    function connectGateway(roomId) {
        gateway?.disconnect();
        gateway = createChatGateway({
            getToken,
            onMessage: frame => {
                try {
                    const data = JSON.parse(frame.body);
                    const content = data.content ?? data.message ?? "";
                    // pendingSent에 있으면 내가 보낸 메시지 → 오른쪽(mine)
                    let isMine = false;
                    if (pendingSent.has(content)) {
                        isMine = true;
                        pendingSent.delete(content);
                    }
                    appendMessage(content, isMine);
                } catch {}
            },
            onError: () => {},
            onDisconnect: () => {}
        });
        gateway.subscribeToRoom(roomId).catch(() => {});
    }

    async function sendMessage() {
        const text = input.value.trim();
        if (!text || !activeRoomId) return;
        input.value = "";
        // 내가 보낸 메시지를 WebSocket echo 수신 시 판별하기 위해 추적
        pendingSent.add(text);
        try {
            await gateway.sendMessage(activeRoomId, text);
        } catch {
            pendingSent.delete(text); // 전송 실패 시 제거
        }
    }

    /* 이벤트 바인딩 */
    document.addEventListener("openItemChatSidebar", openSidebar);
    overlay.addEventListener("click", closeSidebar);
    closeBtn.addEventListener("click", closeSidebar);
    backBtn.addEventListener("click", showRoomList);
    sendBtn.addEventListener("click", sendMessage);
    input.addEventListener("keydown", e => { if (e.key === "Enter" && !e.isComposing) sendMessage(); });
    document.addEventListener("keydown", e => {
        if (e.key === "Escape" && sidebar.classList.contains("is-open")) closeSidebar();
    });
})();
