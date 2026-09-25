package com.gamenest.controller;

import com.gamenest.exception.TeamInvitationNotFoundException;
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
 * Gated by {@link com.gamenest.filter.AuthenticationFilter}. Only the
 * invitation's invitee may Accept — enforced in
 * {@link TeamService#acceptInvitation} via a guarded
 * {@code WHERE invitation_id = ? AND invitee_account_id = ? AND status = 'PENDING'}
 * UPDATE, in the same transaction as the resulting TeamMembers row (task
 * spec §10/§14).
 */
@WebServlet(name = "TeamInviteAcceptServlet", urlPatterns = {"/account/team/invite/accept"})
public class TeamInviteAcceptServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(TeamInviteAcceptServlet.class.getName());
    private static final String LIST_VIEW = "/account/teams/invitations";

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
        int invitationId = parseId(request.getParameter("invitationId"));

        try {
            int teamId = teamService.acceptInvitation(invitationId, accountId);
            response.sendRedirect(request.getContextPath() + "/account/team/detail?id=" + teamId);
            return;

        } catch (TeamInvitationNotFoundException e) {
            session.setAttribute("flashError", e.getMessage());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while accepting team invitation", e);
            session.setAttribute("flashError", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
        }

        response.sendRedirect(request.getContextPath() + LIST_VIEW);
    }

    private int parseId(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException | NullPointerException e) {
            return -1;
        }
    }
}
