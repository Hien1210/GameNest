package com.gamenest.controller;

import com.gamenest.exception.ConversationNotFoundException;
import com.gamenest.exception.ForbiddenException;
import com.gamenest.exception.TeamNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.Account;
import com.gamenest.model.Conversation;
import com.gamenest.model.ConversationType;
import com.gamenest.model.MessageSearchResult;
import com.gamenest.model.Team;
import com.gamenest.service.ChatService;
import com.gamenest.service.TeamService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Search Message (APPROVED design): read-only, GET only — reuses
 * {@link ChatService#getAccessibleConversation}, exactly like
 * {@link ChatDetailServlet}'s own access check, never the stricter
 * send-tier check, so a DIRECT conversation's history stays searchable
 * after Unfriend/Block just as it stays readable. No WebSocket event, no
 * JSON — forwards to a server-rendered JSP results view, mirroring this
 * project's only existing search precedent ({@code LFGListServlet}'s
 * {@code ?q=} pattern) since no JSON/AJAX API exists anywhere else in this
 * codebase's controller layer.
 */
@WebServlet(name = "ChatSearchServlet", urlPatterns = {"/account/chat/search"})
public class ChatSearchServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ChatSearchServlet.class.getName());
    private static final String VIEW = "/account/chat-search.jsp";
    private static final String LIST_VIEW = "/account/chats";

    private final ChatService chatService = new ChatService();
    private final TeamService teamService = new TeamService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");
            return;
        }
        int accountId = (int) session.getAttribute("accountId");
        int conversationId = parseId(request.getParameter("id"));
        String keyword = request.getParameter("keyword");

        try {
            Conversation conversation = chatService.getAccessibleConversation(conversationId, accountId);

            String displayName;
            if (ConversationType.TEAM.equals(conversation.getType())) {
                Team team = teamService.getTeam(conversation.getTeamId());
                displayName = team.getName();
            } else {
                Optional<Account> other = chatService.getOtherDirectParticipant(conversationId, accountId);
                displayName = other.map(a -> a.getDisplayName() != null && !a.getDisplayName().isEmpty()
                        ? a.getDisplayName() : a.getUsername()).orElse("Người dùng");
            }

            List<MessageSearchResult> results = chatService.searchMessages(conversationId, accountId, keyword);

            request.setAttribute("conversation", conversation);
            request.setAttribute("displayName", displayName);
            request.setAttribute("keyword", keyword);
            request.setAttribute("results", results);

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (ForbiddenException e) {
            request.getRequestDispatcher("/access-denied.jsp").forward(request, response);

        } catch (ConversationNotFoundException e) {
            session.setAttribute("flashError", e.getMessage());
            response.sendRedirect(request.getContextPath() + LIST_VIEW);

        } catch (TeamNotFoundException e) {
            session.setAttribute("flashError", "Nhóm không còn tồn tại.");
            response.sendRedirect(request.getContextPath() + LIST_VIEW);

        } catch (ValidationException e) {
            session.setAttribute("flashError", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/account/chat/detail?id=" + conversationId);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while searching messages", e);
            session.setAttribute("flashError", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            response.sendRedirect(request.getContextPath() + "/account/chat/detail?id=" + conversationId);
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
