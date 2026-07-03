import { api } from "../core/api.js";
import { bootstrapShell, emptyState, showToast } from "../core/shell.js";

const itemId = new URLSearchParams(window.location.search).get("id");
const form = document.querySelector("#editForm");
const categorySelect = document.querySelector("#editCategorySelect");
const imageFiles = document.querySelector("#editImageFiles");
const closeDateField = document.querySelector("#editCloseDateField");
const tradeStatusField = document.querySelector("#editTradeStatusField");
const tradeStatusSelect = document.querySelector("#editTradeStatusSelect");
const editRuleBox = document.querySelector("#editRuleBox");
const editPageTitle = document.querySelector("#editPageTitle");
const editPageGuide = document.querySelector("#editPageGuide");

const state = {
    item: null,
    myItemMeta: null
};

function toDatetimeLocal(value) {
    if (!value) {
        return "";
    }
    return String(value).slice(0, 16);
}

function isAuctionItem(item) {
    return Boolean(item.closeDate);
}

function applyRules(item) {
    const auction = isAuctionItem(item);
    closeDateField.classList.toggle("hidden", !auction);
    tradeStatusField.classList.toggle("hidden", auction);

    if (auction) {
        editPageTitle.textContent = "경매 거래 수정";
        editPageGuide.textContent = "등록된 경매 상품은 수정이 막힐 수 있습니다. 임시저장 경매만 가격과 마감일 변경이 가능합니다.";
        editRuleBox.textContent = "이미 비드가 시작된 경매라면 서버 규칙에 따라 수정이 거절될 수 있습니다.";
        return;
    }

    editPageTitle.textContent = "직거래 수정";
    editPageGuide.textContent = "직거래 상품은 제목, 카테고리, 설명, 가격, 판매 상태를 수정할 수 있습니다.";
    editRuleBox.textContent = "직거래 상품은 마감일 없이 수정됩니다.";
}

function fillForm(item) {
    form.elements.title.value = item.title ?? "";
    form.elements.categoryId.value = String(state.myItemMeta?.categoryId ?? "");
    form.elements.initialPrice.value = item.initialPrice ?? 0;
    form.elements.description.value = item.description ?? "";
    if (form.elements.closeDate) {
        form.elements.closeDate.value = toDatetimeLocal(item.closeDate);
    }
    const currentStatus = state.myItemMeta?.tradeStatus ?? item.tradeStatus ?? "ON_SALE";
    if (tradeStatusSelect) {
        tradeStatusSelect.value = currentStatus;
    }
}

function renderEditSummary() {
    if (!state.myItemMeta) {
        return;
    }
    editRuleBox.innerHTML = `
        <strong>${state.myItemMeta.isDraft ? "임시저장 상품" : "등록된 상품"}</strong><br>
        거래 방식: ${state.myItemMeta.tradeType === "AUCTION" ? "경매" : "직거래"}<br>
        현재 상태: ${state.myItemMeta.tradeStatus}<br>
        ${state.myItemMeta.tradeType === "AUCTION"
            ? "경매 상품은 상태와 입찰 여부에 따라 수정이 제한될 수 있습니다."
            : "직거래 상품은 종료일 없이 가격과 설명을 바로 고칠 수 있습니다."}
    `;
}

async function loadCategories() {
    const response = await api.listCategories({ page: 0, size: 50 });
    const categories = (response.content ?? []).filter(category => category.isActive !== false);
    categorySelect.innerHTML = categories.map(category => `
        <option value="${category.id}">${category.name}</option>
    `).join("");
}

async function loadItem() {
    const [item, myItemsResponse] = await Promise.all([
        api.getItem(itemId),
        api.listMyItems({ page: 0, size: 100 })
    ]);
    state.myItemMeta = (myItemsResponse.content ?? []).find(candidate => String(candidate.itemId) === String(itemId)) ?? null;
    if (!state.myItemMeta) {
        throw new Error("내 상품만 수정할 수 있습니다.");
    }
    state.item = item;
    applyRules(item);
    fillForm(item);
    renderEditSummary();
}

async function submitForm() {
    const formData = new FormData(form);
    const payload = {
        categoryId: Number(formData.get("categoryId")),
        title: formData.get("title"),
        description: formData.get("description"),
        initialPrice: Number(formData.get("initialPrice"))
    };

    if (isAuctionItem(state.item) && formData.get("closeDate")) {
        payload.closeDate = formData.get("closeDate");
    }

    await api.updateItem(itemId, payload);

    // 직거래: 판매 상태가 기존과 다를 경우 별도 PATCH 요청
    if (!isAuctionItem(state.item) && tradeStatusSelect) {
        const newStatus = tradeStatusSelect.value;
        const currentStatus = state.myItemMeta?.tradeStatus ?? "ON_SALE";
        if (newStatus && newStatus !== currentStatus) {
            await api.updateItemStatus(itemId, { tradeStatus: newStatus });
        }
    }

    if (imageFiles.files?.length) {
        await api.uploadItemImages(itemId, imageFiles.files);
    }
    showToast("거래 수정을 저장했습니다.");
    window.location.href = `item.html?id=${itemId}`;
}

async function bootstrap() {
    const shell = await bootstrapShell({ requireAuth: true });
    if (!shell.me) {
        return;
    }

    if (!itemId) {
        form.outerHTML = emptyState("수정할 상품 ID가 없습니다.");
        return;
    }

    form.addEventListener("submit", async event => {
        event.preventDefault();
        try {
            await submitForm();
        } catch (error) {
            showToast(error.message, true);
        }
    });

    try {
        await loadCategories();
        await loadItem();
    } catch (error) {
        form.outerHTML = emptyState(error.message);
    }
}

bootstrap();
