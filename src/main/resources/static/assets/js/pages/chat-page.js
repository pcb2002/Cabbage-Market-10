import { api, getToken } from "../core/api.js";
import { createChatGateway } from "../core/chat.js";
import { bootstrapShell, emptyState, showToast } from "../core/shell.js";
import { escapeHtml, formatDateTime, formatRelativeTime } from "../core/utils.js";

const state = {
    me: null,
    rooms: [],
    activeRoomId: new URLSearchParams(window.location.search).get("roomId") || "",
    activeRoomName: "",
    messages: [],
    gateway: null,
    messagePage: 0,
    messageLast: false,
    loadingOlder: false
};

const elements = {
    connection: document.querySelector("#chatConnectionState"),
    roomList: document.querySelector("#chatRoomList"),
    roomTitle: document.querySelector("#chatRoomTitle"),
    messages: document.querySelector("#chatMessages"),
    form: document.querySelector("#chatForm"),
    input: document.querySelector("#chatMessageInput"),
    loadState: document.querySelector("#chatLoadState")
};

function setConnectionState(text) {
    elements.connection.textContent = text;
}

function renderRooms() {
    if (!state.rooms.length) {
        elements.roomList.innerHTML = emptyState("아직 생성된 채팅방이 없습니다.");
        return;
    }

    elements.roomList.innerHTML = state.rooms.map(room => `
        <button type="button" class="chat-room-card ${String(room.id) === String(state.activeRoomId) ? "is-active" : ""}" data-room-id="${room.id}" data-room-name="${escapeHtml(room.itemName)}">
            <strong>${escapeHtml(room.itemName)}</strong>
            <div class="chat-room-meta">
                <div>${room.id}</div>
                <time>${formatRelativeTime(room.date)}</time>
            </div>
        </button>
    `).join("");
}

function renderMessages() {
    if (!state.activeRoomId) {
        elements.roomTitle.textContent = "채팅방을 선택하세요";
        elements.messages.className = "chat-messages empty-box";
        elements.messages.textContent = "채팅방을 고르면 메시지가 표시됩니다.";
        return;
    }

    elements.roomTitle.textContent = state.activeRoomName || `채팅방 ${state.activeRoomId}`;

    if (!state.messages.length) {
        elements.messages.className = "chat-messages empty-box";
        elements.messages.textContent = "첫 메시지를 보내보세요.";
        return;
    }

    elements.messages.className = "chat-messages";
    elements.messages.innerHTML = state.messages.map(message => `
        <article class="chat-message ${state.me && message.senderId === state.me.clientId ? "mine" : ""}">
            <strong>${escapeHtml(message.senderName)}</strong>
            <p>${escapeHtml(message.content)}</p>
            <time>${formatDateTime(message.createdAt)}</time>
        </article>
    `).join("");
}

function sortMessagesAscending(messages) {
    return [...messages].sort((left, right) => new Date(left.createdAt) - new Date(right.createdAt));
}

function mergeMessages(current, incoming) {
    const byId = new Map(current.map(message => [message.messageId, message]));
    incoming.forEach(message => byId.set(message.messageId, message));
    return sortMessagesAscending([...byId.values()]);
}

function setLoadState(visible) {
    elements.loadState?.classList.toggle("hidden", !visible);
}

async function loadRooms() {
    const response = await api.listChatRooms({ page: 0, size: 20 });
    state.rooms = response.content ?? [];
    if (!state.activeRoomId && state.rooms[0]) {
        state.activeRoomId = state.rooms[0].id;
        state.activeRoomName = state.rooms[0].itemName;
    }
    renderRooms();
}

async function loadMessages(roomId, { reset = false } = {}) {
    const page = reset ? 0 : state.messagePage;
    const response = await api.listChatMessages(roomId, { page, size: 30 });
    const pageContent = sortMessagesAscending(response.content ?? []);

    if (reset) {
        state.messages = pageContent;
        state.messagePage = 1;
    } else {
        state.messages = mergeMessages(pageContent, state.messages);
        state.messagePage += 1;
    }

    state.messageLast = response.totalPages ? page >= response.totalPages - 1 : true;
    renderMessages();
}

async function loadOlderMessages() {
    if (!state.activeRoomId || state.loadingOlder || state.messageLast) {
        return;
    }

    state.loadingOlder = true;
    setLoadState(true);
    const previousHeight = elements.messages.scrollHeight;

    try {
        await loadMessages(state.activeRoomId, { reset: false });
        const nextHeight = elements.messages.scrollHeight;
        elements.messages.scrollTop = nextHeight - previousHeight + elements.messages.scrollTop;
    } finally {
        state.loadingOlder = false;
        setLoadState(false);
    }
}

async function connectRoom(roomId) {
    if (!state.gateway) {
        state.gateway = createChatGateway({
            getToken,
            onConnect: () => setConnectionState("실시간 연결됨"),
            onDisconnect: () => setConnectionState("연결 끊김"),
            onError: message => showToast(message, true),
            onMessage: async () => {
                await Promise.all([loadRooms(), loadMessages(state.activeRoomId, { reset: true })]);
                elements.messages.scrollTop = elements.messages.scrollHeight;
            }
        });
    }

    await state.gateway.subscribeToRoom(roomId);
}

async function openRoom(roomId, roomName) {
    state.activeRoomId = roomId;
    state.activeRoomName = roomName;
    state.messagePage = 0;
    state.messageLast = false;
    renderRooms();
    await loadMessages(roomId, { reset: true });
    elements.messages.scrollTop = elements.messages.scrollHeight;
    await connectRoom(roomId);
}

function bindEvents() {
    document.addEventListener("click", async event => {
        const roomButton = event.target.closest("[data-room-id]");
        if (!roomButton) {
            return;
        }
        try {
            await openRoom(roomButton.dataset.roomId, roomButton.dataset.roomName);
        } catch (error) {
            showToast(error.message, true);
        }
    });

    elements.messages.addEventListener("scroll", async () => {
        if (elements.messages.scrollTop <= 40) {
            try {
                await loadOlderMessages();
            } catch (error) {
                showToast(error.message, true);
            }
        }
    });

    elements.form.addEventListener("submit", async event => {
        event.preventDefault();
        const message = elements.input.value.trim();
        if (!message || !state.activeRoomId || !state.gateway) {
            return;
        }
        try {
            await state.gateway.sendMessage(state.activeRoomId, message);
            elements.input.value = "";
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
    state.me = shell.me;
    bindEvents();
    setConnectionState("연결 대기");

    try {
        await loadRooms();
        if (state.activeRoomId) {
            const room = state.rooms.find(candidate => String(candidate.id) === String(state.activeRoomId));
            if (room) {
                state.activeRoomName = room.itemName;
            }
            await openRoom(state.activeRoomId, state.activeRoomName);
        } else {
            renderMessages();
        }
    } catch (error) {
        showToast(error.message, true);
    }
}

bootstrap();
