export function formatPrice(value) {
    return `${Number(value || 0).toLocaleString("ko-KR")}원`;
}

export function formatDateTime(value) {
    if (!value) {
        return "-";
    }
    return new Date(value).toLocaleString("ko-KR", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit"
    });
}

export function formatRelativeTime(value) {
    if (!value) {
        return "방금 전";
    }
    const diff = Date.now() - new Date(value).getTime();
    const minute = 60 * 1000;
    const hour = 60 * minute;
    const day = 24 * hour;

    if (diff < hour) {
        return `${Math.max(1, Math.floor(diff / minute))}분 전`;
    }
    if (diff < day) {
        return `${Math.floor(diff / hour)}시간 전`;
    }
    return `${Math.floor(diff / day)}일 전`;
}

export function statusLabel(status) {
    return {
        ON_SALE: "판매중",
        RESERVED: "예약중",
        SOLD_OUT: "거래완료"
    }[status] || "상태 미정";
}

export function tradeLabel(tradeType) {
    return {
        DIRECT: "직거래",
        AUCTION: "경매"
    }[tradeType] || "거래";
}

export function escapeHtml(value = "") {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}
