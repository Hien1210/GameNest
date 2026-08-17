package com.gamenest.controller;

import com.gamenest.exception.AccountNotFoundException;
import com.gamenest.exception.DuplicateTeamInvitationException;
import com.gamenest.exception.ForbiddenException;
import com.gamenest.exception.TeamNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.service.TeamService;

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
 * Gated by {@link com.gamenest.filter.AuthenticationFilter}. Owner-only,
 * enforced in {@link TeamService#inviteFriend} (task spec §9) — never from
 * hiding the Invite button. ownerAccountId always comes from the session.
 */
@WebServlet(name = "TeamInviteServlet", urlPatterns = {"/account/team/invite"})
public class TeamInviteServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(TeamInviteServlet.class.getName());

    private final TeamService teamService = new TeamService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");
            return;
        }
        int accountId = (int) session.getAttribute("accountId");
        String displayName = (String) session.getAttribute("displayName");
        String username = (String) session.getAttribute("username");
        String ownerLabel = displayName != null && !displayName.isEmpty() ? displayName : username;

        int teamId = parseId(request.getParameter("teamId"));
        String targetUsername = request.getParameter("username");

        try {
            teamService.inviteFriend(teamId, accountId, ownerLabel, targetUsername);

        } catch (ForbiddenException e) {
            request.getRequestDispatcher("/access-denied.jsp").forward(request, response);
            return;

        } catch (ValidationException | DuplicateTeamInvitationException e) {
            session.setAttribute("flashError", e.getMessage());

        } catch (AccountNotFoundException e) {
            session.setAttribute("flashError", "Người dùng không tồn tại.");

        } catch (TeamNotFoundException e) {
            session.setAttribute("flashError", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/account/teams");
            return;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while inviting to team", e);
            session.setAttribute("flashError", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
        }

        response.sendRedirect(request.getContextPath() + "/account/team/detail?id=" + teamId);
    }

    private int parseId(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException | NullPointerException e) {
            return -1;
        }
    }
}
