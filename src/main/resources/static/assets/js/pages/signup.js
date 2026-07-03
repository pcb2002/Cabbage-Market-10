import { api } from "../core/api.js";
import { bootstrapShell, showToast } from "../core/shell.js";

const form = document.querySelector("#signupForm");

form?.addEventListener("submit", async event => {
    event.preventDefault();
    const payload = Object.fromEntries(new FormData(form).entries());

    try {
        await api.signup(payload);
        await api.login({ email: payload.email, password: payload.password });
        showToast("가입과 로그인이 완료되었습니다.");
        window.location.href = "index.html";
    } catch (error) {
        showToast(error.message, true);
    }
});

bootstrapShell().catch(error => showToast(error.message, true));
