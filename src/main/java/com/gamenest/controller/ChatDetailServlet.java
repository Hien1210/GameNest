package com.gamenest.controller;

import com.gamenest.exception.ForbiddenException;
import com.gamenest.exception.ConversationNotFoundException;
import com.gamenest.exception.TeamNotFoundException;
import com.gamenest.model.Account;
import com.gamenest.model.Conversation;
import com.gamenest.model.ConversationType;
import com.gamenest.model.Message;
import com.gamenest.model.Team;
import com.gamenest.service.ChatService;
import com.gamenest.service.PresenceService;
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
 * Gated by {@link com.gamenest.filter.AuthenticationFilter}. Access is
 * fully re-verified in {@link ChatService#getAccessibleConversation} on
 * every request (task spec §13/§22) — never inferred from a hidden field
 * or the fact the link exists; ForbiddenException forwards to
 * /access-denied.jsp, matching Team Detail's own convention for the exact
 * same kind of "not a member" case.
 */
@WebServlet(name = "ChatDetailServlet", urlPatterns = {"/account/chat/detail"})
public class ChatDetailServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ChatDetailServlet.class.getName());
    private static final String VIEW = "/account/chat-detail.jsp";
    private static final String LIST_VIEW = "/account/chats";

    private final ChatService chatService = new ChatService();
    private final TeamService teamService = new TeamService();
    private final PresenceService presenceService = new PresenceService();

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
        int page = parsePage(request.getParameter("page"));

        if (session.getAttribute("flashError") != null) {
            request.setAttribute("error", session.getAttribute("flashError"));
            session.removeAttribute("flashError");
        }

        try {
            Conversation conversation = chatService.getAccessibleConversation(conversationId, accountId);

            String displayName;
            Integer otherAccountId = null;
            Boolean otherOnline = null;
            if (ConversationType.TEAM.equals(conversation.getType())) {
                Team team = teamService.getTeam(conversation.getTeamId());
                displayName = team.getName();
            } else {
                Optional<Account> other = chatService.getOtherDirectParticipant(conversationId, accountId);
                displayName = other.map(a -> a.getDisplayName() != null && !a.getDisplayName().isEmpty()
                        ? a.getDisplayName() : a.getUsername()).orElse("Người dùng");
                // Presence is User↔User (Friend) only in this task — Team
                // Chat participants are not covered, matching Presence task
                // spec §13. Re-checked live (not cached) since a DIRECT
                // conversation's history stays visible even after Unfriend/
                // Block, so the relationship may have changed since this
                // conversation was created.
                if (other.isPresent()) {
                    otherAccountId = other.get().getAccountId();
                    otherOnline = presenceService.getVisiblePresence(accountId, otherAccountId);
                }
            }

            List<Message> messages = chatService.listMessages(conversationId, accountId, page);
            int totalCount = chatService.countMessages(conversationId, accountId);
            int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / chatService.getPageSize()));

            request.setAttribute("conversation", conversation);
            request.setAttribute("displayName", displayName);
            request.setAttribute("messages", messages);
            request.setAttribute("page", page);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("currentAccountId", accountId);
            request.setAttribute("otherAccountId", otherAccountId);
            request.setAttribute("otherOnline", otherOnline != null && otherOnline);

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (ForbiddenException e) {
            request.getRequestDispatcher("/access-denied.jsp").forward(request, response);

        } catch (ConversationNotFoundException e) {
            session.setAttribute("flashError", e.getMessage());
            response.sendRedirect(request.getContextPath() + LIST_VIEW);

        } catch (TeamNotFoundException e) {
            session.setAttribute("flashError", "Nhóm không còn tồn tại.");
            response.sendRedirect(request.getContextPath() + LIST_VIEW);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while loading chat detail", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher(VIEW).forward(request, response);
        }
    }

    private int parseId(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException | NullPointerException e) {
            return -1;
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
