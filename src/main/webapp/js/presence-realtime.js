(function () {
    "use strict";

    // Independent Presence transport (task spec §19: not folded into
    // chat-realtime.js). Opens its own connection to the SAME /ws/chat
    // endpoint (no second WebSocket system) so Presence works on pages that
    // never open a Chat conversation, e.g. the Friends list. Nothing here
    // ever sends a message to the server — Presence is entirely
    // server-generated from connect/disconnect (task spec §21/§24); this
    // script only ever listens.

    var config = window.GameNestPresence;
    if (!config || !("WebSocket" in window)) {
        return;
    }

    var dots = document.querySelectorAll("[data-presence-account-id]");
    if (dots.length === 0) {
        return; // nothing on this page needs realtime presence
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
        // Presence itself needs no reconnect/backoff logic here: the page's
        // initial server-rendered state (task spec §16) already reflects
        // presence as of page load; while this connection is down, that
        // rendered state simply stops updating in realtime until the user
        // reloads or a future reconnect strategy is added — out of scope
        // for this task.
    }

    function handleServerEvent(raw) {
        var data;
        try {
            data = JSON.parse(raw);
        } catch (e) {
            return;
        }
        if (!data || data.type !== "PRESENCE_CHANGED" || typeof data.accountId !== "number") {
            return;
        }
        updatePresenceDots(data.accountId, data.status === "ONLINE");
    }

    function updatePresenceDots(accountId, isOnline) {
        var matches = document.querySelectorAll('[data-presence-account-id="' + accountId + '"]');
        for (var i = 0; i < matches.length; i++) {
            applyState(matches[i], isOnline);
        }
    }

    function applyState(el, isOnline) {
        el.classList.toggle("online", isOnline);
        el.classList.toggle("offline", !isOnline);
        var label = el.querySelector(".presence-label");
        if (label) {
            label.textContent = isOnline ? "Online" : "Offline";
        }
    }

    connect();
})();
