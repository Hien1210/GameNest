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

    // Reply (Chat Advanced Feature #1): compose-state elements live in the
    // send-card above the textarea (see chat-detail.jsp). Absent on any page
    // that doesn't render them — every reply-UI function below no-ops safely
    // when these are null, same defensive pattern as typingIndicatorEl above.
    var replyToInput = document.getElementById("replyToMessageId");
    var replyPreviewEl = document.getElementById("replyComposePreview");
    var replyPreviewLabelEl = replyPreviewEl ? replyPreviewEl.querySelector(".reply-compose-label") : null;
    var replyPreviewTextEl = replyPreviewEl ? replyPreviewEl.querySelector(".reply-compose-text") : null;
    var replyPreviewCancelBtn = replyPreviewEl ? replyPreviewEl.querySelector(".reply-compose-cancel") : null;
    var REPLY_PREVIEW_MAX_LENGTH = 80;

    // Typing Indicator is DIRECT-only (same precedent as trackSeenIndicator
    // below — a Team member's "typing" would be ambiguous to attribute) and
    // WS-only: no HTTP fallback exists or is needed, since it's a passive
    // signal, not a user action (task spec — "nếu WebSocket rớt thì không
    // hiển thị Typing Indicator, đó là hành vi chấp nhận được").
    var typingIndicatorEl = document.getElementById("typingIndicator");
    var typingIndicatorEnabled = config.conversationType === "DIRECT" && !!typingIndicatorEl;
    var isTypingLocally = false;
    var typingStopTimer = null;
    var TYPING_STOP_DELAY_MS = 1500;

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
        } else if (data.type === "TYPING_STARTED") {
            handleTypingStarted(data);
        } else if (data.type === "TYPING_STOPPED") {
            handleTypingStopped(data);
        } else if (data.type === "REACTION_UPDATED") {
            handleReactionUpdated(data);
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
        if (typingIndicatorEnabled && !isOwn) {
            // The other party's message just arrived — guard against a
            // stale "đang nhập..." if this MESSAGE_CREATED happens to reach
            // us before its accompanying TYPING_STOPPED frame does (the
            // server sends them in that order for the same send).
            hideTypingIndicator();
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

        if (message.replyToMessageId) {
            card.appendChild(buildReplyTargetPreview(message));
        }

        var contentEl = document.createElement("div");
        contentEl.className = "message-content";
        contentEl.textContent = message.content || "";
        card.appendChild(contentEl);

        card.appendChild(buildReactionBar(message.messageId, [], null));

        if (trackSeenIndicator && isOwn) {
            var readStatusEl = document.createElement("span");
            readStatusEl.className = "read-status";
            card.appendChild(readStatusEl);
        }

        var replyRow = document.createElement("div");
        replyRow.className = "message-reply-row";
        var replyBtn = document.createElement("button");
        replyBtn.type = "button";
        replyBtn.className = "reply-btn";
        replyBtn.textContent = "Trả lời";
        replyBtn.setAttribute("data-message-id", String(message.messageId));
        replyBtn.setAttribute("data-sender-label", senderLabel);
        replyBtn.setAttribute("data-preview", truncateForCompose(message.content || ""));
        replyRow.appendChild(replyBtn);
        card.appendChild(replyRow);

        messageList.insertBefore(card, messageList.firstChild);
    }

    // Mirrors the reply-target-preview block chat-detail.jsp renders
    // server-side, so a WS-delivered reply looks identical to a page-loaded
    // one. message.replyToSenderLabel is always the target's real name
    // (never "Bạn") since one broadcast payload reaches every recipient —
    // "Bạn" is substituted here, per-client, by comparing accountIds.
    function buildReplyTargetPreview(message) {
        var block = document.createElement("div");
        block.className = "reply-target-preview";
        block.setAttribute("data-reply-target-id", String(message.replyToMessageId));

        if (message.replyToDeleted) {
            var deletedEl = document.createElement("span");
            deletedEl.className = "reply-target-deleted";
            deletedEl.textContent = "Tin nhắn đã được xóa";
            block.appendChild(deletedEl);
            return block;
        }

        var isTargetOwn = message.replyToSenderAccountId !== null && message.replyToSenderAccountId !== undefined
                && Number(message.replyToSenderAccountId) === Number(config.accountId);
        var senderEl = document.createElement("span");
        senderEl.className = "reply-target-sender";
        senderEl.textContent = isTargetOwn ? "Bạn" : (message.replyToSenderLabel || "");
        block.appendChild(senderEl);

        var textEl = document.createElement("span");
        textEl.className = "reply-target-text";
        textEl.textContent = message.replyToPreview || "";
        block.appendChild(textEl);

        return block;
    }

    // Client-side mirror of ChatWsProtocol#truncate — used only for the
    // reply button's own data-preview (a preview of THIS message's full
    // content, shown in the compose box if the user replies to it), never
    // for rendering a reply target (that preview always comes pre-truncated
    // from the server via replyToPreview).
    function truncateForCompose(content) {
        if (content.length <= REPLY_PREVIEW_MAX_LENGTH) {
            return content;
        }
        return content.substring(0, REPLY_PREVIEW_MAX_LENGTH) + "…";
    }

    function startReply(messageId, senderLabel, preview) {
        if (!replyToInput || !replyPreviewEl) {
            return;
        }
        replyToInput.value = String(messageId);
        if (replyPreviewLabelEl) {
            replyPreviewLabelEl.textContent = senderLabel || "";
        }
        if (replyPreviewTextEl) {
            replyPreviewTextEl.textContent = preview || "";
        }
        replyPreviewEl.style.display = "";
        contentInput.focus();
    }

    function cancelReply() {
        if (!replyToInput || !replyPreviewEl) {
            return;
        }
        replyToInput.value = "";
        replyPreviewEl.style.display = "none";
    }

    function scrollToMessage(messageId) {
        var target = messageList.querySelector('.message-card[data-message-id="' + messageId + '"]');
        if (!target) {
            return; // target not currently loaded on this page — no Message Search, do nothing (task scope).
        }
        target.scrollIntoView({behavior: "smooth", block: "center"});
        target.classList.add("highlight");
        setTimeout(function () {
            target.classList.remove("highlight");
        }, 1500);
    }

    if (replyPreviewCancelBtn) {
        replyPreviewCancelBtn.addEventListener("click", function (event) {
            event.preventDefault();
            cancelReply();
        });
    }

    // Delegated so it also covers reply buttons/preview blocks added later
    // by prependMessageCard, not just the ones rendered at page load.
    messageList.addEventListener("click", function (event) {
        var replyBtn = event.target.closest ? event.target.closest(".reply-btn") : null;
        if (replyBtn) {
            startReply(
                replyBtn.getAttribute("data-message-id"),
                replyBtn.getAttribute("data-sender-label"),
                replyBtn.getAttribute("data-preview")
            );
            return;
        }
        var previewBlock = event.target.closest ? event.target.closest(".reply-target-preview") : null;
        if (previewBlock) {
            scrollToMessage(previewBlock.getAttribute("data-reply-target-id"));
        }
    });

    // Reaction (Chat Advanced Feature #2): every non-deleted message renders
    // its own .reaction-bar (existing chips + a "+" picker of the fixed
    // 6-emoji allowlist — must mirror ChatService's REACTION_EMOJI_ALLOWLIST
    // and chat-detail.jsp's own REACTION_EMOJI array exactly). Reaction
    // controls are real <form> POSTs to /account/chat/reaction (progressive
    // enhancement, same pattern as sendForm/markReadForm): intercepted here
    // to send TOGGLE_REACTION over the socket when one is open, otherwise
    // left alone to submit as plain HTTP. The server is the sole authority
    // on ADD/REMOVE/CHANGE — this client only ever sends TOGGLE_REACTION and
    // only ever renders whatever REACTION_UPDATED sends back, never a local
    // optimistic guess.
    var REACTION_EMOJI_OPTIONS = ["👍", "❤️", "😂", "😮", "😢", "😡"];

    function buildReactionChipForm(messageId, emoji, count, mine, isPickerOption) {
        var form = document.createElement("form");
        form.className = "inline-form reaction-form";
        form.method = "post";
        form.action = config.contextPath + "/account/chat/reaction";

        var messageIdInput = document.createElement("input");
        messageIdInput.type = "hidden";
        messageIdInput.name = "messageId";
        messageIdInput.value = String(messageId);
        form.appendChild(messageIdInput);

        var emojiInput = document.createElement("input");
        emojiInput.type = "hidden";
        emojiInput.name = "emoji";
        emojiInput.value = emoji;
        form.appendChild(emojiInput);

        var button = document.createElement("button");
        button.type = "submit";
        if (isPickerOption) {
            button.className = "reaction-picker-btn";
            button.textContent = emoji;
        } else {
            button.className = "reaction-chip" + (mine ? " mine" : "");
            button.textContent = emoji + " ";
            var countEl = document.createElement("span");
            countEl.className = "reaction-count";
            countEl.textContent = String(count);
            button.appendChild(countEl);
        }
        form.appendChild(button);

        return form;
    }

    // Shared by prependMessageCard (brand-new WS-delivered message: empty
    // reactions, myReaction null) and handleReactionUpdated (rebuild an
    // existing bar in place from the server's own aggregate) — mirrors the
    // SSR structure chat-detail.jsp renders exactly (chips, then a "+"
    // add-button, then the hidden picker), so a WS-built bar and a
    // page-loaded one are indistinguishable.
    function buildReactionBar(messageId, reactions, myReaction) {
        var bar = document.createElement("div");
        bar.className = "reaction-bar";
        bar.setAttribute("data-message-id", String(messageId));

        (reactions || []).forEach(function (r) {
            bar.appendChild(buildReactionChipForm(messageId, r.emoji, r.count, r.emoji === myReaction, false));
        });

        var addWrap = document.createElement("div");
        addWrap.className = "reaction-add-wrap";

        var addBtn = document.createElement("button");
        addBtn.type = "button";
        addBtn.className = "reaction-add-btn";
        addBtn.title = "Thả cảm xúc";
        addBtn.textContent = "+";
        addWrap.appendChild(addBtn);

        var picker = document.createElement("div");
        picker.className = "reaction-picker";
        picker.style.display = "none";
        REACTION_EMOJI_OPTIONS.forEach(function (emoji) {
            picker.appendChild(buildReactionChipForm(messageId, emoji, null, false, true));
        });
        addWrap.appendChild(picker);

        bar.appendChild(addWrap);
        return bar;
    }

    // REACTION_UPDATED always carries the full current aggregate (task
    // Decision 3) — the whole bar is rebuilt from scratch rather than
    // patched, so there's no risk of drifting from server state.
    function handleReactionUpdated(data) {
        var card = messageList.querySelector('.message-card[data-message-id="' + data.messageId + '"]');
        if (!card) {
            return; // message not loaded on this page — nothing to update
        }
        var oldBar = card.querySelector(".reaction-bar");
        if (!oldBar) {
            // Only a soft-deleted message ever omits .reaction-bar (both the
            // JSP and prependMessageCard always render one otherwise); the
            // server itself rejects toggling a reaction on a deleted
            // message, so this should never actually happen — ignore rather
            // than guess where to insert one.
            return;
        }
        var newBar = buildReactionBar(data.messageId, data.reactions || [], data.myReaction || null);
        card.replaceChild(newBar, oldBar);
    }

    function closeAllReactionPickers(except) {
        var pickers = messageList.querySelectorAll(".reaction-picker");
        for (var i = 0; i < pickers.length; i++) {
            if (pickers[i] !== except) {
                pickers[i].style.display = "none";
            }
        }
    }

    function toggleReactionPicker(addBtn) {
        var wrap = addBtn.closest ? addBtn.closest(".reaction-add-wrap") : null;
        var picker = wrap ? wrap.querySelector(".reaction-picker") : null;
        if (!picker) {
            return;
        }
        var isOpen = picker.style.display !== "none";
        closeAllReactionPickers();
        picker.style.display = isOpen ? "none" : "flex";
    }

    // Delegated (not bound at creation time) because .reaction-add-btn
    // elements are rebuilt wholesale on every REACTION_UPDATED — a listener
    // attached to one specific button would be discarded along with it.
    messageList.addEventListener("click", function (event) {
        var addBtn = event.target.closest ? event.target.closest(".reaction-add-btn") : null;
        if (addBtn) {
            event.preventDefault();
            toggleReactionPicker(addBtn);
        }
    });

    // Close any open picker on an outside click. A click that opened one
    // (the handler above) or that is about to submit a picker option (the
    // submit handler below) both land inside .reaction-add-wrap, so they're
    // excluded here.
    document.addEventListener("click", function (event) {
        if (event.target.closest && event.target.closest(".reaction-add-wrap")) {
            return;
        }
        closeAllReactionPickers();
    });

    // Delegated for the same reason as the click handler above — reaction
    // forms (both existing chips and picker options) are rebuilt on every
    // REACTION_UPDATED, so only a listener on the stable #messageList
    // ancestor keeps working across rebuilds. TOGGLE_REACTION is the only
    // client→server reaction event (task Decision 2) — this never sends
    // ADD_REACTION/REMOVE_REACTION/CHANGE_REACTION; the server alone decides
    // which of those actually happened.
    messageList.addEventListener("submit", function (event) {
        var form = event.target.closest ? event.target.closest(".reaction-form") : null;
        if (!form) {
            return;
        }
        if (!socketOpen) {
            return; // HTTP fallback — POST /account/chat/reaction still works exactly as before.
        }
        var messageIdInput = form.querySelector('input[name="messageId"]');
        var emojiInput = form.querySelector('input[name="emoji"]');
        if (!messageIdInput || !emojiInput) {
            return;
        }
        event.preventDefault();
        socket.send(JSON.stringify({
            type: "TOGGLE_REACTION",
            messageId: Number(messageIdInput.value),
            emoji: emojiInput.value
        }));
        closeAllReactionPickers();
    });

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

    function handleTypingStarted(data) {
        if (!typingIndicatorEnabled || Number(data.conversationId) !== Number(config.conversationId)) {
            return;
        }
        if (Number(data.accountId) === Number(config.accountId)) {
            return; // never our own event (server already excludes us; this is a defensive double-guard)
        }
        typingIndicatorEl.style.display = "";
    }

    function handleTypingStopped(data) {
        if (!typingIndicatorEnabled || Number(data.conversationId) !== Number(config.conversationId)) {
            return;
        }
        if (Number(data.accountId) === Number(config.accountId)) {
            return;
        }
        hideTypingIndicator();
    }

    function hideTypingIndicator() {
        typingIndicatorEl.style.display = "none";
    }

    // Debounced TYPING_START/STOP (task spec — not per-keystroke): the
    // first keystroke of a burst sends TYPING_START, every subsequent
    // keystroke just resets the inactivity timer, and TYPING_STOP fires
    // once typing has paused for TYPING_STOP_DELAY_MS.
    function sendTypingStart() {
        if (isTypingLocally) {
            return;
        }
        isTypingLocally = true;
        socket.send(JSON.stringify({type: "TYPING_START", conversationId: config.conversationId}));
    }

    function scheduleTypingStop() {
        if (typingStopTimer) {
            clearTimeout(typingStopTimer);
        }
        typingStopTimer = setTimeout(function () {
            typingStopTimer = null;
            if (isTypingLocally && socketOpen) {
                isTypingLocally = false;
                socket.send(JSON.stringify({type: "TYPING_STOP", conversationId: config.conversationId}));
            } else {
                isTypingLocally = false;
            }
        }, TYPING_STOP_DELAY_MS);
    }

    // Clicking Send clears the client's own local typing timer/state (task
    // spec). No explicit TYPING_STOP frame is sent here: the server
    // independently clears the sender's Typing state as part of handling
    // SEND_MESSAGE (see ChatWebSocketEndpoint#handleSendMessage), so this
    // never relies solely on JS to notify the recipient.
    function clearLocalTypingState() {
        if (typingStopTimer) {
            clearTimeout(typingStopTimer);
            typingStopTimer = null;
        }
        isTypingLocally = false;
    }

    if (typingIndicatorEnabled) {
        contentInput.addEventListener("input", function () {
            if (!socketOpen) {
                return; // Typing Indicator is WS-only — no HTTP fallback.
            }
            sendTypingStart();
            scheduleTypingStop();
        });
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
        if (typingIndicatorEnabled) {
            clearLocalTypingState();
        }
        var replyToMessageId = replyToInput && replyToInput.value ? Number(replyToInput.value) : null;
        socket.send(JSON.stringify({
            type: "SEND_MESSAGE",
            conversationId: config.conversationId,
            content: content,
            replyToMessageId: replyToMessageId
        }));
        contentInput.value = "";
        cancelReply();
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
