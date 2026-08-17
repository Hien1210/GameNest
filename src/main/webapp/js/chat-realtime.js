(function () {
    "use strict";

    // Realtime transport for Chat Detail only (task scope: SEND_MESSAGE +
    // MESSAGE_CREATED delivery, plus MARK_READ + READ_UPDATED for Realtime
    // Read/Seen). Both the send form and the "Đánh dấu đã đọc" form below
    // still work exactly as before over plain HTTP — this script only
    // intercepts their submit when a WebSocket connection is actually open,
    // and otherwise gets out of the way so POST /account/chat/send and
    // POST /account/chat/read keep working unmodified.

    var config = window.GameNestChat;
    if (!config || !config.conversationId || !config.accountId) {
        return;
    }

    var messageList = document.getElementById("messageList");
    var sendForm = document.querySelector(".send-form");
    var contentInput = sendForm ? sendForm.querySelector("textarea[name=content]") : null;
    var markReadForm = document.querySelector('form[action$="/account/chat/read"]');
    var markReadInput = markReadForm ? markReadForm.querySelector("input[name=messageId]") : null;

    if (!messageList || !sendForm || !contentInput || !("WebSocket" in window)) {
        return;
    }

    // "Đã xem" indicator is DIRECT-only (task spec §12/§15 — one Team
    // member's read position must never be shown as if the whole Team had
    // read it). Realtime-only: there is no server-rendered initial state,
    // so a message already seen before this page load won't show "Đã xem"
    // until a fresh READ_UPDATED arrives during this session (documented
    // limitation, not a bug).
    var trackSeenIndicator = config.conversationType === "DIRECT";
    var latestOwnMessageId = null;
    var otherReadMessageId = 0;

    if (trackSeenIndicator) {
        var initialOwnCard = messageList.querySelector(".message-card.own");
        if (initialOwnCard) {
            latestOwnMessageId = Number(initialOwnCard.getAttribute("data-message-id"));
        }
    }

    var socket = null;
    var socketOpen = false;

    function connect() {
        var scheme = window.location.protocol === "https:" ? "wss:" : "ws:";
        var url = scheme + "//" + window.location.host + config.contextPath + "/ws/chat";

        try {
            socket = new WebSocket(url);
        } catch (e) {
            return;
        }

        socket.onopen = function () {
            socketOpen = true;
        };
        socket.onclose = function () {
            socketOpen = false;
        };
        socket.onerror = function () {
            socketOpen = false;
        };
        socket.onmessage = function (event) {
            handleServerEvent(event.data);
        };
    }

    function handleServerEvent(raw) {
        var data;
        try {
            data = JSON.parse(raw);
        } catch (e) {
            return;
        }
        if (!data || typeof data.type !== "string") {
            return;
        }
        if (data.type === "MESSAGE_CREATED" && data.message) {
            handleMessageCreated(data.message);
        } else if (data.type === "READ_UPDATED") {
            handleReadUpdated(data);
        } else if (data.type === "ERROR") {
            handleError(data);
        }
        // CONNECTION_READY / PRESENCE_CHANGED: nothing for this module to render.
    }

    function handleError(data) {
        // Realtime send failed (task spec §11) — content stays in the
        // textarea so nothing typed is lost; the normal "Gửi" button (HTTP
        // fallback) still works if the user tries again.
        window.alert(data.message || "Không thể gửi tin nhắn.");
    }

    function handleMessageCreated(message) {
        if (Number(message.conversationId) !== Number(config.conversationId)) {
            // This account has a realtime session for another conversation
            // it also belongs to — not relevant to what's on screen here.
            return;
        }
        var isOwn = Number(message.senderAccountId) === Number(config.accountId);
        prependMessageCard(message, isOwn);
        if (markReadInput) {
            markReadInput.value = message.messageId;
        }
        if (trackSeenIndicator && isOwn) {
            // A message you just sent hasn't been seen by the other side
            // yet — advance the tracked id but don't carry over the old
            // "Đã xem" tag onto it.
            latestOwnMessageId = Number(message.messageId);
            refreshSeenIndicator();
        }
    }

    function prependMessageCard(message, isOwn) {
        var placeholder = document.getElementById("emptyMessagesPlaceholder");
        if (placeholder && placeholder.parentNode) {
            placeholder.parentNode.removeChild(placeholder);
        }

        var senderLabel = isOwn ? "Bạn" : (message.senderDisplayName || message.senderUsername || "");

        var card = document.createElement("div");
        card.className = isOwn ? "message-card own" : "message-card";
        card.setAttribute("data-message-id", String(message.messageId));

        var header = document.createElement("div");
        header.className = "message-header";

        var senderEl = document.createElement("span");
        senderEl.className = "message-sender";
        senderEl.textContent = senderLabel;
        header.appendChild(senderEl);

        var timeEl = document.createElement("span");
        timeEl.className = "message-time";
        timeEl.textContent = message.createdAt || "";
        header.appendChild(timeEl);

        card.appendChild(header);

        var contentEl = document.createElement("div");
        contentEl.className = "message-content";
        contentEl.textContent = message.content || "";
        card.appendChild(contentEl);

        if (trackSeenIndicator && isOwn) {
            var readStatusEl = document.createElement("span");
            readStatusEl.className = "read-status";
            card.appendChild(readStatusEl);
        }

        messageList.insertBefore(card, messageList.firstChild);
    }

    function handleReadUpdated(data) {
        if (!trackSeenIndicator || Number(data.conversationId) !== Number(config.conversationId)) {
            return;
        }
        if (Number(data.accountId) === Number(config.accountId)) {
            return; // our own read position — nothing to show ourselves
        }
        otherReadMessageId = Math.max(otherReadMessageId, Number(data.messageId));
        refreshSeenIndicator();
    }

    function refreshSeenIndicator() {
        var existing = messageList.querySelectorAll(".read-status");
        for (var i = 0; i < existing.length; i++) {
            existing[i].textContent = "";
        }
        if (latestOwnMessageId === null || otherReadMessageId < latestOwnMessageId) {
            return;
        }
        var card = messageList.querySelector('.message-card.own[data-message-id="' + latestOwnMessageId + '"]');
        var label = card ? card.querySelector(".read-status") : null;
        if (label) {
            label.textContent = "Đã xem";
        }
    }

    sendForm.addEventListener("submit", function (event) {
        if (!socketOpen) {
            return; // HTTP fallback — let the normal form submission happen.
        }
        var content = contentInput.value;
        if (!content || !content.trim()) {
            return;
        }
        event.preventDefault();
        socket.send(JSON.stringify({
            type: "SEND_MESSAGE",
            conversationId: config.conversationId,
            content: content
        }));
        contentInput.value = "";
    });

    if (markReadForm && markReadInput) {
        markReadForm.addEventListener("submit", function (event) {
            if (!socketOpen) {
                return; // HTTP fallback — POST /account/chat/read still works exactly as before.
            }
            var conversationIdInput = markReadForm.querySelector("input[name=conversationId]");
            if (!conversationIdInput) {
                return; // unexpected form shape — fall back to HTTP rather than send a malformed event.
            }
            event.preventDefault();
            socket.send(JSON.stringify({
                type: "MARK_READ",
                conversationId: Number(conversationIdInput.value),
                messageId: Number(markReadInput.value)
            }));
        });
    }

    connect();
})();
