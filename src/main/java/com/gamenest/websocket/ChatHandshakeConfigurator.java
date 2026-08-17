package com.gamenest.websocket;

import jakarta.servlet.http.HttpSession;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * Transfers the already-authenticated HTTP session into the new
 * {@link ChatWebSocketEndpoint} instance for this one connection, so
 * {@code @OnOpen} can read {@code accountId} from it (task spec §5) — the
 * WebSocket layer never trusts anything the client itself could supply.
 * <p>
 * Deliberately does NOT stash the HttpSession in
 * {@link ServerEndpointConfig#getUserProperties()} the way the commonly
 * copy-pasted tutorial pattern does: that Map belongs to the single
 * {@code ServerEndpointConfig} instance shared by every connection to this
 * endpoint, so writing a per-connection value into it races under
 * concurrent handshakes — connection A's {@code @OnOpen} could read
 * connection B's HttpSession if B's handshake overwrites the shared map
 * between A's write and A's read, silently authenticating one user's
 * WebSocket connection as a different user's account. A {@link ThreadLocal}
 * is used instead: {@link #modifyHandshake} and {@link #getEndpointInstance}
 * both run synchronously on the same thread as part of processing the same
 * HTTP upgrade request (this is a Jakarta WebSocket lifecycle guarantee),
 * so plain thread-confinement — not a shared mutable field — is what keeps
 * concurrent connections isolated from each other here. Endpoint POJOs are
 * also a new instance per connection by default, so once the accountId is
 * copied onto that instance, no further sharing risk exists at all.
 */
public class ChatHandshakeConfigurator extends ServerEndpointConfig.Configurator {

    private static final ThreadLocal<HttpSession> PENDING_HTTP_SESSION = new ThreadLocal<>();

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        Object httpSession = request.getHttpSession();
        if (httpSession instanceof HttpSession session) {
            PENDING_HTTP_SESSION.set(session);
        }
    }

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        try {
            T instance = super.getEndpointInstance(endpointClass);
            if (instance instanceof ChatWebSocketEndpoint endpoint) {
                endpoint.assignHttpSession(PENDING_HTTP_SESSION.get());
            }
            return instance;
        } finally {
            PENDING_HTTP_SESSION.remove();
        }
    }
}
