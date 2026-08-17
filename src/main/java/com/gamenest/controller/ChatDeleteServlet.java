package com.gamenest.controller;

import com.gamenest.exception.MessageNotFoundException;
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
 * Gated by {@link com.gamenest.filter.AuthenticationFilter}. Only the
 * sender may delete — enforced in {@link ChatService#deleteMessage} via a
 * guarded UPDATE (task spec §16). Never a hard DELETE — soft delete only
 * (deleted_at). accountId always comes from the session.
 */
@WebServlet(name = "ChatDeleteServlet", urlPatterns = {"/account/chat/delete"})
public class ChatDeleteServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ChatDeleteServlet.class.getName());
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
        int conversationId = parseId(request.getParameter("conversationId"));

        try {
            chatService.deleteMessage(messageId, accountId);

        } catch (MessageNotFoundException e) {
            session.setAttribute("flashError", e.getMessage());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while deleting message", e);
            session.setAttribute("flashError", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
        }

        if (conversationId > 0) {
            response.sendRedirect(request.getContextPath() + "/account/chat/detail?id=" + conversationId);
        } else {
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
