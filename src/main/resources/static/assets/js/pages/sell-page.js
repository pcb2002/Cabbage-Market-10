import { api } from "../core/api.js";
import { bootstrapShell, showToast } from "../core/shell.js";

const form = document.querySelector("#sellForm");
const categorySelect = document.querySelector("#categorySelect");
const tradeTypeSelect = document.querySelector("#tradeTypeSelect");
const closeDateField = document.querySelector("#closeDateField");
const imageFiles = document.querySelector("#imageFiles");
const draftButton = document.querySelector("#draftButton");
const tradeGuide = document.querySelector("#tradeGuide");

function syncTradeMode(mode) {
    tradeTypeSelect.value = mode;
    closeDateField.classList.toggle("hidden", mode !== "AUCTION");
    document.querySelectorAll("[data-trade-mode]").forEach(button => {
        button.classList.toggle("is-active", button.dataset.tradeMode === mode);
    });
    tradeGuide.textContent = mode === "AUCTION"
        ? "경매는 시작가보다 높은 비드가 들어오며, 마감일이 반드시 필요합니다."
        : "직거래는 즉시 거래 가능한 상품에 적합합니다.";
}

async function loadCategories() {
    const response = await api.listCategories({ page: 0, size: 50 });
    const categories = (response.content ?? []).filter(category => category.isActive !== false);
    categorySelect.innerHTML = categories.map(category => `
        <option value="${category.id}">${category.name}</option>
    `).join("");
}

async function submitForm(saveDraft = false) {
    const formData = new FormData(form);
    const payload = {
        title: formData.get("title"),
        categoryId: Number(formData.get("categoryId")),
        tradeType: formData.get("tradeType"),
        conditionType: formData.get("conditionType"),
        description: formData.get("description"),
        initialPrice: Number(formData.get("initialPrice"))
    };

    if (payload.tradeType === "AUCTION" && formData.get("closeDate")) {
        payload.closeDate = formData.get("closeDate");
    }

    const response = saveDraft ? await api.createDraft(payload) : await api.createItem(payload);
    if (response.itemId && imageFiles.files?.length) {
        await api.uploadItemImages(response.itemId, imageFiles.files);
    }
    showToast(saveDraft ? "임시저장했습니다." : "상품을 등록했습니다.");
    form.reset();
    syncTradeMode("DIRECT");
    if (!saveDraft) {
        window.location.href = "index.html";
    }
}

function bindEvents() {
    document.addEventListener("click", event => {
        const tradeModeButton = event.target.closest("[data-trade-mode]");
        if (!tradeModeButton) {
            return;
        }
        syncTradeMode(tradeModeButton.dataset.tradeMode);
    });

    tradeTypeSelect.addEventListener("change", event => {
        syncTradeMode(event.target.value);
    });

    form.addEventListener("submit", async event => {
        event.preventDefault();
        try {
            await submitForm(false);
        } catch (error) {
            showToast(error.message, true);
        }
    });

    draftButton.addEventListener("click", async () => {
        try {
            await submitForm(true);
        } catch (error) {
            showToast(error.message, true);
        }
    });
}

async function bootstrap() {
    const shell = await bootstrapShell({ requireAuth: true });
    if (!shell.me) {
        return;
    }
    bindEvents();
    try {
        await loadCategories();
        syncTradeMode("DIRECT");
    } catch (error) {
        showToast(error.message, true);
    }
}

bootstrap();
