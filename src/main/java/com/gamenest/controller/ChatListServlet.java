package com.gamenest.controller;

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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gated by {@link com.gamenest.filter.AuthenticationFilter} — "my Chats" is
 * a private view of the current session's own conversations (task spec
 * §18/§19), never every conversation in the system.
 */
@WebServlet(name = "ChatListServlet", urlPatterns = {"/account/chats"})
public class ChatListServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ChatListServlet.class.getName());
    private static final String VIEW = "/account/chats.jsp";

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
        int page = parsePage(request.getParameter("page"));

        if (session.getAttribute("flashError") != null) {
            request.setAttribute("error", session.getAttribute("flashError"));
            session.removeAttribute("flashError");
        }

        try {
            List<Conversation> conversations = chatService.listMyConversations(accountId, page);
            int totalCount = chatService.countMyConversations(accountId);
            int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / chatService.getPageSize()));

            request.setAttribute("conversations", conversations);
            request.setAttribute("page", page);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("currentAccountId", accountId);

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while listing chats", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher(VIEW).forward(request, response);
        }
    }

    private int parsePage(String raw) {
        try {
            return Math.max(Integer.parseInt(raw), 1);
        } catch (NumberFormatException | NullPointerException e) {
            return 1;
        }
    }
}
