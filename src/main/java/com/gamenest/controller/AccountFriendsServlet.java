package com.gamenest.controller;

import com.gamenest.model.AccountFriendship;
import com.gamenest.service.AccountFriendService;
import com.gamenest.service.PresenceService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gated by {@link com.gamenest.filter.AuthenticationFilter} — "my Friends"
 * is inherently a private view of the current session's own account, unlike
 * Follow's public followers/following lists (task spec §11: only
 * status=ACCEPTED, either side, current user's own accountId from session).
 */
@WebServlet(name = "AccountFriendsServlet", urlPatterns = {"/account/friends"})
public class AccountFriendsServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AccountFriendsServlet.class.getName());
    private static final String VIEW = "/account/friends.jsp";

    private final AccountFriendService accountFriendService = new AccountFriendService();
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
        int page = parsePage(request.getParameter("page"));
        request.setAttribute("currentAccountId", accountId);

        try {
            List<AccountFriendship> friends = accountFriendService.listFriends(accountId, page);
            int totalCount = accountFriendService.countFriends(accountId);
            int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / accountFriendService.getPageSize()));

            // Initial Presence snapshot for this page's rows only (task
            // spec §16) — every entry here is already an ACCEPTED friend by
            // construction of listFriends, so PresenceService's visibility
            // check can only ever return null on a razor-thin concurrent
            // Block race; that edge case is treated as Offline (never a
            // 3rd/"hidden" visual state — task spec §3 allows only 2).
            Map<Integer, Boolean> presenceByAccountId = new HashMap<>();
            for (AccountFriendship f : friends) {
                int otherAccountId = f.getRequesterAccountId() == accountId
                        ? f.getReceiverAccountId() : f.getRequesterAccountId();
                Boolean online = presenceService.getVisiblePresence(accountId, otherAccountId);
                presenceByAccountId.put(otherAccountId, Boolean.TRUE.equals(online));
            }

            request.setAttribute("friends", friends);
            request.setAttribute("page", page);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("presenceByAccountId", presenceByAccountId);

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while listing friends", e);
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
