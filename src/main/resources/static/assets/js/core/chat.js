function buildFrame(command, headers = {}, body = "") {
    const headerLines = Object.entries(headers).map(([key, value]) => `${key}:${value}`);
    return `${command}\n${headerLines.join("\n")}\n\n${body}\0`;
}

function parseFrame(raw) {
    const cleaned = raw.replace(/\0/g, "");
    const [headerBlock, ...bodyParts] = cleaned.split("\n\n");
    const [command, ...headerLines] = headerBlock.split("\n");
    const headers = {};

    headerLines.forEach(line => {
        const separatorIndex = line.indexOf(":");
        if (separatorIndex > -1) {
            headers[line.slice(0, separatorIndex)] = line.slice(separatorIndex + 1);
        }
    });

    return {
        command,
        headers,
        body: bodyParts.join("\n\n")
    };
}

export function createChatGateway({ getToken, onConnect, onDisconnect, onError, onMessage }) {
    let socket = null;
    let subscriptionId = null;
    let activeRoomId = null;
    let isConnected = false;
    let connectPromise = null;

    function ensureSocket(roomId) {
        if (socket && isConnected && activeRoomId === roomId) {
            return Promise.resolve();
        }

        disconnect();

        const token = getToken();
        if (!token) {
            throw new Error("채팅 연결을 위해 로그인이 필요합니다.");
        }

        socket = new WebSocket(`${window.location.origin.replace(/^http/, "ws")}/ws/chat`);
        connectPromise = new Promise((resolve, reject) => {
            socket.addEventListener("open", () => {
                socket.send(buildFrame("CONNECT", {
                    "accept-version": "1.2",
                    "heart-beat": "10000,10000",
                    Authorization: `Bearer ${token}`
                }));
            });

            socket.addEventListener("message", event => {
                const frame = parseFrame(event.data);

                if (frame.command === "CONNECTED") {
                    isConnected = true;
                    activeRoomId = roomId;
                    subscriptionId = `sub-${roomId}`;
                    socket.send(buildFrame("SUBSCRIBE", {
                        id: subscriptionId,
                        destination: `/sub/${roomId}/messages`
                    }));
                    onConnect?.();
                    resolve();
                    return;
                }

                if (frame.command === "MESSAGE") {
                    onMessage?.(frame);
                    return;
                }

                if (frame.command === "ERROR") {
                    const message = frame.body || "채팅 연결 중 오류가 발생했습니다.";
                    onError?.(message);
                    reject(new Error(message));
                }
            });

            socket.addEventListener("error", () => {
                reject(new Error("채팅 서버 연결에 실패했습니다."));
            });
        });

        socket.addEventListener("close", () => {
            isConnected = false;
            activeRoomId = null;
            subscriptionId = null;
            connectPromise = null;
            onDisconnect?.();
        });

        return connectPromise;
    }

    function subscribeToRoom(roomId) {
        return ensureSocket(roomId);
    }

    async function sendMessage(roomId, content) {
        if (!socket || !isConnected || activeRoomId !== roomId) {
            await subscribeToRoom(roomId);
        }

        if (!socket || !isConnected) {
            throw new Error("채팅 서버에 연결되지 않았습니다.");
        }

        socket.send(buildFrame("SEND", {
            destination: `/pub/${roomId}/messages`,
            "content-type": "application/json"
        }, JSON.stringify({
            content,
            contentType: "TEXT"
        })));
    }

    function disconnect() {
        if (socket && socket.readyState === WebSocket.OPEN && subscriptionId) {
            socket.send(buildFrame("UNSUBSCRIBE", { id: subscriptionId }));
            socket.send(buildFrame("DISCONNECT"));
        }
        socket?.close();
        socket = null;
        isConnected = false;
        activeRoomId = null;
        subscriptionId = null;
        connectPromise = null;
    }

    return {
        subscribeToRoom,
        sendMessage,
        disconnect
    };
}
