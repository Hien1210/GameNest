package com.gamenest.controller;

import com.gamenest.exception.AccountNotFoundException;
import com.gamenest.model.Account;
import com.gamenest.model.AccountFriendship;
import com.gamenest.model.AccountGame;
import com.gamenest.model.AccountGameRelationshipType;
import com.gamenest.model.FriendshipStatus;
import com.gamenest.service.AccountBlockService;
import com.gamenest.service.AccountFollowService;
import com.gamenest.service.AccountFriendService;
import com.gamenest.service.AccountGameService;
import com.gamenest.service.AccountService;
import com.gamenest.service.PresenceService;

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
 * Read-only public profile of another account, identified by username (the
 * project has no existing "view another user" route; username is already
 * globally unique and already publicly visible everywhere — Question/Answer
 * author names, the login form — so it is not a new exposure, unlike the
 * internal numeric account_id used by /admin/accounts/detail).
 * <p>
 * Anonymous visitors are allowed (this route is intentionally NOT in
 * AuthenticationFilter) — same "public browsing" posture as /games and
 * /questions. Only an ACTIVE account has a visible profile; see
 * {@link AccountService#getPublicProfile}.
 */
@WebServlet(name = "PublicProfileServlet", urlPatterns = {"/account/view"})
public class PublicProfileServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(PublicProfileServlet.class.getName());
    private static final String VIEW = "/account/public-profile.jsp";

    private final AccountService accountService = new AccountService();
    private final AccountGameService accountGameService = new AccountGameService();
    private final AccountFollowService accountFollowService = new AccountFollowService();
    private final AccountFriendService accountFriendService = new AccountFriendService();
    private final AccountBlockService accountBlockService = new AccountBlockService();
    private final PresenceService presenceService = new PresenceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("flashError") != null) {
            request.setAttribute("error", session.getAttribute("flashError"));
            session.removeAttribute("flashError");
        }

        try {
            Account account = accountService.getPublicProfile(username);
            request.setAttribute("account", account);

            List<AccountGame> playing = accountGameService.listByAccountAndType(
                    account.getAccountId(), AccountGameRelationshipType.PLAYING);
            List<AccountGame> favorites = accountGameService.listByAccountAndType(
                    account.getAccountId(), AccountGameRelationshipType.FAVORITE);
            request.setAttribute("playingGames", playing);
            request.setAttribute("favoriteGames", favorites);

            // Follow — session accountId only, never the request (task spec §6).
            Object accountIdAttr = session == null ? null : session.getAttribute("accountId");
            boolean isSelf = accountIdAttr instanceof Integer && (Integer) accountIdAttr == account.getAccountId();
            request.setAttribute("isSelf", isSelf);

            // Block — computed before Follow/Friend so those can be
            // suppressed while a block exists in either direction (task spec
            // §14). isBlockedByViewer (exact direction) drives the "Đã chặn"/
            // "Bỏ chặn" button; blockedEitherDirection (both directions) only
            // hides Follow/Friend actions — it is never exposed to the JSP as
            // "target blocked you", per the no-sensitive-detail rule.
            boolean isBlockedByViewer = false;
            boolean blockedEitherDirection = false;
            if (accountIdAttr instanceof Integer && !isSelf) {
                int viewerAccountId = (Integer) accountIdAttr;
                isBlockedByViewer = accountBlockService.isBlocked(viewerAccountId, account.getAccountId());
                blockedEitherDirection = isBlockedByViewer
                        || accountBlockService.isBlockedBetween(viewerAccountId, account.getAccountId());
            }
            request.setAttribute("isBlockedByViewer", isBlockedByViewer);
            request.setAttribute("blockedEitherDirection", blockedEitherDirection);

            boolean isFollowing = accountIdAttr instanceof Integer && !isSelf && !blockedEitherDirection
                    && accountFollowService.isFollowing((Integer) accountIdAttr, account.getAccountId());
            request.setAttribute("isFollowing", isFollowing);
            request.setAttribute("followersCount", accountFollowService.countFollowers(account.getAccountId()));
            request.setAttribute("followingCount", accountFollowService.countFollowing(account.getAccountId()));

            // Friend — NONE/OUTGOING_PENDING/INCOMING_PENDING/FRIEND state,
            // computed here so the JSP never queries the database directly
            // (task spec §17). A REJECTED/CANCELLED/UNFRIENDED row is treated
            // the same as no row at all — re-friendable, matching
            // AccountFriendService#sendRequest's own reuse logic.
            boolean isFriend = false;
            boolean friendOutgoingPending = false;
            boolean friendIncomingPending = false;
            Integer friendshipId = null;
            if (accountIdAttr instanceof Integer && !isSelf && !blockedEitherDirection) {
                int viewerAccountId = (Integer) accountIdAttr;
                Optional<AccountFriendship> active =
                        accountFriendService.findActiveBetween(viewerAccountId, account.getAccountId());
                if (active.isPresent()) {
                    AccountFriendship friendship = active.get();
                    friendshipId = friendship.getFriendshipId();
                    if (FriendshipStatus.ACCEPTED.equals(friendship.getStatus())) {
                        isFriend = true;
                    } else if (friendship.getRequesterAccountId() == viewerAccountId) {
                        friendOutgoingPending = true;
                    } else {
                        friendIncomingPending = true;
                    }
                }
            }
            request.setAttribute("isFriend", isFriend);
            request.setAttribute("friendOutgoingPending", friendOutgoingPending);
            request.setAttribute("friendIncomingPending", friendIncomingPending);
            request.setAttribute("friendshipId", friendshipId);

            // Presence — static snapshot only, not realtime (task spec §20:
            // "chỉ thêm nếu tích hợp rất nhỏ"). Reuses isFriend/viewerAccountId
            // already computed above; only ever visible when isFriend is true,
            // same Friend+Block visibility rule as Friends List/Chat Detail.
            if (isFriend) {
                request.setAttribute("presenceOnline", presenceService.getVisiblePresence(
                        (Integer) accountIdAttr, account.getAccountId()));
            }

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (AccountNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while loading public profile", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher(VIEW).forward(request, response);
        }
    }
}
