package com.gamenest.controller;

import com.gamenest.exception.ConversationNotFoundException;
import com.gamenest.exception.ForbiddenException;
import com.gamenest.exception.MessageNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.ReactionResult;
import com.gamenest.service.ChatService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HTTP fallback for Message Reaction (Decision 1) — the WebSocket
 * TOGGLE_REACTION event is the primary transport; this servlet exists only
 * for a client without an open WebSocket connection. Gated by
 * {@link com.gamenest.filter.AuthenticationFilter}. accountId always comes
 * from the session, never from the client (mirrors {@link ChatSendServlet}
 * exactly). Full authorization chain (membership, Friend+Block for DIRECT,
 * Team ACTIVE+membership for TEAM, soft-deleted-target rejection) is
 * enforced in {@link ChatService#toggleReaction}, not here.
 * <p>
 * The redirect target's conversationId is read back from the
 * {@link ReactionResult} the Service returns, never from a request
 * parameter — a client cannot redirect this servlet to an arbitrary
 * conversation it does not itself belong to.
 */
@WebServlet(name = "ChatReactionServlet", urlPatterns = {"/account/chat/reaction"})
public class ChatReactionServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ChatReactionServlet.class.getName());
    private static final String LIST_VIEW = "/account/chats";

    private final ChatService chatService = new ChatService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");
            return;
        }
        int accountId = (int) session.getAttribute("accountId");
        int messageId = parseId(request.getParameter("messageId"));
        String emoji = request.getParameter("emoji");

        // messageId missing/invalid — nothing to resolve a conversation
        // redirect from; fail back to the chat list rather than guessing.
        if (messageId <= 0) {
            session.setAttribute("flashError", "Tin nhắn không tồn tại.");
            response.sendRedirect(request.getContextPath() + LIST_VIEW);
            return;
        }

        try {
            ReactionResult result = chatService.toggleReaction(messageId, accountId, emoji);
            response.sendRedirect(request.getContextPath() + "/account/chat/detail?id=" + result.getConversationId());
            return;

        } catch (MessageNotFoundException e) {
            session.setAttribute("flashError", e.getMessage());
            response.sendRedirect(request.getContextPath() + LIST_VIEW);
            return;

        } catch (ForbiddenException e) {
            request.getRequestDispatcher("/access-denied.jsp").forward(request, response);
            return;

        } catch (ValidationException e) {
            session.setAttribute("flashError", e.getMessage());
            response.sendRedirect(request.getContextPath() + LIST_VIEW);
            return;

        } catch (ConversationNotFoundException e) {
            session.setAttribute("flashError", e.getMessage());
            response.sendRedirect(request.getContextPath() + LIST_VIEW);
            return;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while toggling chat reaction", e);
            session.setAttribute("flashError", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            response.sendRedirect(request.getContextPath() + LIST_VIEW);
        }
    }

    private int parseId(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException | NullPointerException e) {
            return -1;
        }
    }
}
