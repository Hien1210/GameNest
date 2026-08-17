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
 * invitation's invitee may Reject — enforced in
 * {@link TeamService#rejectInvitation} via a guarded UPDATE (task spec
 * §11/§14). No TeamMember is created; no Notification is sent.
 */
@WebServlet(name = "TeamInviteRejectServlet", urlPatterns = {"/account/team/invite/reject"})
public class TeamInviteRejectServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(TeamInviteRejectServlet.class.getName());

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
            teamService.rejectInvitation(invitationId, accountId);

        } catch (TeamInvitationNotFoundException e) {
            session.setAttribute("flashError", e.getMessage());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while rejecting team invitation", e);
            session.setAttribute("flashError", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
        }

        response.sendRedirect(request.getContextPath() + "/account/teams/invitations");
    }

    private int parseId(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException | NullPointerException e) {
            return -1;
        }
    }
}
