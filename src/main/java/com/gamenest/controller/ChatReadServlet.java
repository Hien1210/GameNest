package com.gamenest.controller;

import com.gamenest.exception.ConversationNotFoundException;
import com.gamenest.exception.ForbiddenException;
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
 * Gated by {@link com.gamenest.filter.AuthenticationFilter}. accountId
 * always comes from the session (task spec §17/§27). The real access check
 * (membership, Team-still-ACTIVE) is a hard failure here; an invalid/stale
 * messageId is a silent no-op inside {@link ChatService#markAsRead} — never
 * an error (task spec §17 "fail safely").
 */
@WebServlet(name = "ChatReadServlet", urlPatterns = {"/account/chat/read"})
public class ChatReadServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ChatReadServlet.class.getName());
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
        int conversationId = parseId(request.getParameter("conversationId"));
        int messageId = parseId(request.getParameter("messageId"));

        try {
            chatService.markAsRead(conversationId, accountId, messageId);

        } catch (ForbiddenException e) {
            request.getRequestDispatcher("/access-denied.jsp").forward(request, response);
            return;

        } catch (ConversationNotFoundException e) {
            session.setAttribute("flashError", e.getMessage());
            response.sendRedirect(request.getContextPath() + LIST_VIEW);
            return;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while marking chat as read", e);
            session.setAttribute("flashError", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
        }

        response.sendRedirect(request.getContextPath() + "/account/chat/detail?id=" + conversationId);
    }

    private int parseId(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException | NullPointerException e) {
            return -1;
        }
    }
}
