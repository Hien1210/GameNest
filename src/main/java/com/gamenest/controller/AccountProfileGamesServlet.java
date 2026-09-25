package com.gamenest.controller;

import com.gamenest.exception.DuplicateAccountGameException;
import com.gamenest.exception.GameNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.AccountGameRelationshipType;
import com.gamenest.service.AccountGameService;

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
 * Add/remove a game under the currently logged-in account's Playing or
 * Favorite list (AccountGames, task spec §9/§10). accountId always comes
 * from the session, never from a request parameter — the DELETE path is
 * scoped to (accountId, gameId, relationshipType) at the DAO level, so a
 * user can only ever affect their own relationship rows (task spec §7/§9).
 * Uses the existing session-flash pattern (see QuestionDetailServlet) to
 * carry a message across the redirect back to /account/profile.
 */
@WebServlet(name = "AccountProfileGamesServlet", urlPatterns = {"/account/profile/games"})
public class AccountProfileGamesServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AccountProfileGamesServlet.class.getName());

    private final AccountGameService accountGameService = new AccountGameService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");
            return;
        }
        int accountId = (int) session.getAttribute("accountId");

        String action = request.getParameter("action");
        int gameId = parseId(request.getParameter("gameId"));

        String relationshipType = relationshipTypeOf(action);
        boolean isAdd = action != null && action.startsWith("add-");

        if (relationshipType == null || gameId <= 0) {
            session.setAttribute("flashError", "Hành động không hợp lệ.");
            response.sendRedirect(request.getContextPath() + "/account/profile");
            return;
        }

        try {
            if (isAdd) {
                accountGameService.addRelationship(accountId, gameId, relationshipType);
            } else {
                accountGameService.removeRelationship(accountId, gameId, relationshipType);
            }
            response.sendRedirect(request.getContextPath() + "/account/profile?updated=1");

        } catch (GameNotFoundException | ValidationException | DuplicateAccountGameException e) {
            session.setAttribute("flashError", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/account/profile");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while updating account games", e);
            session.setAttribute("flashError", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            response.sendRedirect(request.getContextPath() + "/account/profile");
        }
    }

    private String relationshipTypeOf(String action) {
        if (action == null) {
            return null;
        }
        return switch (action) {
            case "add-playing", "remove-playing" -> AccountGameRelationshipType.PLAYING;
            case "add-favorite", "remove-favorite" -> AccountGameRelationshipType.FAVORITE;
            default -> null;
        };
    }

    private int parseId(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException | NullPointerException e) {
            return -1;
        }
    }
}
