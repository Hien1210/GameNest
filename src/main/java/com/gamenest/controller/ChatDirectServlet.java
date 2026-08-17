package com.gamenest.controller;

import com.gamenest.exception.AccountNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.Conversation;
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
 * Gated by {@link com.gamenest.filter.AuthenticationFilter}. Opens (or
 * creates) the Direct Conversation with the given username and redirects to
 * it (task spec §21). accountId always comes from the session — the
 * request only ever supplies the target's username, matching Follow/
 * Friend/Block/Team's existing "identify by username" convention.
 */
@WebServlet(name = "ChatDirectServlet", urlPatterns = {"/account/chat/direct"})
public class ChatDirectServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ChatDirectServlet.class.getName());
    private static final String LIST_VIEW = "/account/chats";

    private final ChatService chatService = new ChatService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");
            return;
        }
        int accountId = (int) session.getAttribute("accountId");
        String targetUsername = request.getParameter("username");

        try {
            Conversation conversation = chatService.openDirectChat(accountId, targetUsername);
            response.sendRedirect(request.getContextPath() + "/account/chat/detail?id=" + conversation.getConversationId());

        } catch (ValidationException e) {
            session.setAttribute("flashError", e.getMessage());
            response.sendRedirect(request.getContextPath() + LIST_VIEW);

        } catch (AccountNotFoundException e) {
            session.setAttribute("flashError", "Người dùng không tồn tại.");
            response.sendRedirect(request.getContextPath() + LIST_VIEW);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while opening direct chat", e);
            session.setAttribute("flashError", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            response.sendRedirect(request.getContextPath() + LIST_VIEW);
        }
    }
}
