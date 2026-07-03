import { api } from "../core/api.js";
import { bootstrapShell, clearPendingLoginEmail, getPendingLoginEmail, showToast } from "../core/shell.js";

const form = document.querySelector("#loginPasswordForm");
const selectedEmailLabel = document.querySelector("#selectedEmailLabel");
const selectedEmailText = document.querySelector("#selectedEmailText");
const email = getPendingLoginEmail();

if (!email) {
    window.location.href = "login.html";
}

if (selectedEmailLabel) {
    selectedEmailLabel.textContent = email;
}
if (selectedEmailText) {
    selectedEmailText.textContent = `${email} 계정으로 로그인합니다.`;
}

form?.addEventListener("submit", async event => {
    event.preventDefault();
    try {
        await api.login({
            email,
            password: new FormData(form).get("password")
        });
        clearPendingLoginEmail();
        showToast("로그인되었습니다.");
        window.location.href = "index.html";
    } catch (error) {
        showToast(error.message, true);
    }
});

bootstrapShell().catch(error => showToast(error.message, true));
