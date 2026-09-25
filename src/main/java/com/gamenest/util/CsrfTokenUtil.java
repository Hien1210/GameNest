package com.gamenest.util;

import jakarta.servlet.http.HttpSession;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Synchronizer-token helper for CSRF protection. One token per session,
 * created lazily and compared with a constant-time check to avoid timing attacks.
 */
public final class CsrfTokenUtil {

    private static final String SESSION_ATTR = "csrfToken";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CsrfTokenUtil() {
    }

    public static String getOrCreateToken(HttpSession session) {
        String token = (String) session.getAttribute(SESSION_ATTR);
        if (token == null) {
            byte[] bytes = new byte[32];
            SECURE_RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(SESSION_ATTR, token);
        }
        return token;
    }

    public static boolean isValid(HttpSession session, String submitted) {
        if (session == null || submitted == null) {
            return false;
        }
        String expected = (String) session.getAttribute(SESSION_ATTR);
        return expected != null
                && MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.UTF_8),
                        submitted.getBytes(StandardCharsets.UTF_8));
    }
}
