package com.gamenest.controller;

import com.gamenest.exception.ForbiddenException;
import com.gamenest.exception.TeamNotFoundException;
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
 * Gated by {@link com.gamenest.filter.AuthenticationFilter}. Not one of the
 * task's literally-listed routes — added because Team Detail needs a way
 * to link to Team Chat without already knowing the internal
 * conversation_id, mirroring {@link ChatDirectServlet}'s "resolve by
 * business id, then redirect" shape (documented in the final report).
 * Resolves (self-healing if needed — see {@link ChatService#openTeamChat})
 * the given Team's Conversation and redirects to it. Only a current
 * TeamMember of an ACTIVE Team may pass — never lets a request change
 * which Team's chat it lands on by any means other than this check (task
 * spec §22).
 */
@WebServlet(name = "ChatTeamServlet", urlPatterns = {"/account/chat/team"})
public class ChatTeamServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ChatTeamServlet.class.getName());
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
        int teamId = parseId(request.getParameter("id"));

        try {
            Conversation conversation = chatService.openTeamChat(teamId, accountId);
            response.sendRedirect(request.getContextPath() + "/account/chat/detail?id=" + conversation.getConversationId());

        } catch (ForbiddenException e) {
            request.getRequestDispatcher("/access-denied.jsp").forward(request, response);

        } catch (TeamNotFoundException e) {
            session.setAttribute("flashError", e.getMessage());
            response.sendRedirect(request.getContextPath() + LIST_VIEW);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while opening team chat", e);
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
