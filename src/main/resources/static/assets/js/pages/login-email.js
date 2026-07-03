import { bootstrapShell, savePendingLoginEmail, showToast } from "../core/shell.js";

const form = document.querySelector("#loginEmailForm");

form?.addEventListener("submit", event => {
    event.preventDefault();
    const email = String(new FormData(form).get("email") || "").trim();
    if (!email) {
        showToast("이메일을 입력해 주세요.", true);
        return;
    }
    savePendingLoginEmail(email);
    window.location.href = "login-password.html";
});

bootstrapShell().catch(error => showToast(error.message, true));
