(function () {
    "use strict";

    // Realtime transport for Notification delivery only (task spec §3/§4/
    // §15). Opens its own connection to the SAME /ws/chat endpoint Chat and
    // Presence already use — no second WebSocket system — because
    // chat-realtime.js is scoped to a single open conversation (it bails
    // out immediately without a conversationId) and never runs on
    // home.jsp/notifications.jsp, so there is no existing connection on
    // these pages to piggyback on. This module only ever listens for
    // NOTIFICATION_CREATED and silently ignores every other event type
    // (MESSAGE_CREATED/READ_UPDATED/PRESENCE_CHANGED/TYPING_*) that may also
    // arrive on this same account's connection.

    var config = window.GameNestNotification;
    if (!config || !("WebSocket" in window)) {
        return;
    }

    var badge = document.getElementById("notifBadge");
    var list = document.getElementById("notifList");
    var emptyPlaceholder = document.getElementById("notifEmpty");

    if (!badge && !list && !emptyPlaceholder) {
        return; // nothing on this page needs realtime notification updates
    }

    function connect() {
        var scheme = window.location.protocol === "https:" ? "wss:" : "ws:";
        var url = scheme + "//" + window.location.host + config.contextPath + "/ws/chat";

        var socket;
        try {
            socket = new WebSocket(url);
        } catch (e) {
            return;
        }

        socket.onmessage = function (event) {
            handleServerEvent(event.data);
        };
        // No reconnect/backoff here — same documented limitation as
        // presence-realtime.js: while this connection is down, badge/list
        // simply stop updating in realtime until the page is reloaded.
    }

    function handleServerEvent(raw) {
        var data;
        try {
            data = JSON.parse(raw);
        } catch (e) {
            return;
        }
        if (!data || data.type !== "NOTIFICATION_CREATED" || !data.notification) {
            return;
        }
        var notification = data.notification;
        if (badge) {
            incrementBadge();
        }
        if (list || emptyPlaceholder) {
            prependNotificationCard(notification);
        }
    }

    function incrementBadge() {
        var current = parseInt(badge.getAttribute("data-count"), 10);
        if (isNaN(current)) {
            current = 0;
        }
        current += 1;
        badge.setAttribute("data-count", String(current));
        badge.textContent = current > 99 ? "99+" : String(current);
        badge.style.display = "";
    }

    // Rebuilds exactly the same markup/classes account/notifications.jsp
    // already renders server-side for one notification card (task spec
    // §11/§12) — textContent only, never innerHTML, since title/message are
    // user-influenced business text (task spec §12), matching the same
    // security guarantee HtmlUtils.escape gives the server-rendered version.
    function prependNotificationCard(notification) {
        if (emptyPlaceholder && emptyPlaceholder.parentNode) {
            if (!list) {
                list = document.createElement("div");
                list.className = "notif-list";
                list.id = "notifList";
                emptyPlaceholder.parentNode.insertBefore(list, emptyPlaceholder);
            }
            emptyPlaceholder.parentNode.removeChild(emptyPlaceholder);
            emptyPlaceholder = null;
        }
        if (!list) {
            return;
        }

        var navigable = notification.targetType === "QUESTION"
            || notification.targetType === "ANSWER"
            || notification.targetType === "ACCOUNT";

        var card = document.createElement("div");
        card.className = "notif-card unread";

        var body = document.createElement("div");
        body.className = "notif-body";

        var titleRow = document.createElement("div");
        titleRow.className = "notif-title-row";

        var dot = document.createElement("span");
        dot.className = "notif-unread-dot";
        titleRow.appendChild(dot);

        var titleEl = document.createElement("span");
        titleEl.className = "notif-title";
        titleEl.textContent = notification.title || "";
        titleRow.appendChild(titleEl);

        if (navigable) {
            var openForm = document.createElement("form");
            openForm.className = "inline-form";
            openForm.method = "post";
            openForm.action = config.contextPath + "/account/notifications/open";

            var openIdInput = document.createElement("input");
            openIdInput.type = "hidden";
            openIdInput.name = "id";
            openIdInput.value = notification.notificationId;
            openForm.appendChild(openIdInput);

            var openButton = document.createElement("button");
            openButton.type = "submit";
            openButton.className = "notif-link-btn";
            openButton.appendChild(titleRow);
            openForm.appendChild(openButton);

            body.appendChild(openForm);
        } else {
            body.appendChild(titleRow);
        }

        if (notification.message) {
            var messageEl = document.createElement("div");
            messageEl.className = "notif-message";
            messageEl.textContent = notification.message;
            body.appendChild(messageEl);
        }

        var timeEl = document.createElement("div");
        timeEl.className = "notif-time";
        timeEl.textContent = notification.createdAt || "";
        body.appendChild(timeEl);

        card.appendChild(body);

        var actions = document.createElement("div");
        actions.className = "notif-actions";

        var readForm = document.createElement("form");
        readForm.className = "inline-form";
        readForm.method = "post";
        readForm.action = config.contextPath + "/account/notifications/read";

        var readIdInput = document.createElement("input");
        readIdInput.type = "hidden";
        readIdInput.name = "id";
        readIdInput.value = notification.notificationId;
        readForm.appendChild(readIdInput);

        var readButton = document.createElement("button");
        readButton.type = "submit";
        readButton.className = "btn-mark-read";
        readButton.textContent = "Đánh dấu đã đọc";
        readForm.appendChild(readButton);

        actions.appendChild(readForm);
        card.appendChild(actions);

        list.insertBefore(card, list.firstChild);
    }

    connect();
})();
