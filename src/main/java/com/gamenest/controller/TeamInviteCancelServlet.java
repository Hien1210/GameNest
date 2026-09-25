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
 * invitation's inviter may Cancel — enforced in
 * {@link TeamService#cancelInvitation} via a guarded UPDATE (task spec
 * §12/§14). Never touches friendship or existing TeamMembers.
 */
@WebServlet(name = "TeamInviteCancelServlet", urlPatterns = {"/account/team/invite/cancel"})
public class TeamInviteCancelServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(TeamInviteCancelServlet.class.getName());

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
        int teamId = parseId(request.getParameter("teamId"));

        try {
            teamService.cancelInvitation(invitationId, accountId);

        } catch (TeamInvitationNotFoundException e) {
            session.setAttribute("flashError", e.getMessage());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while cancelling team invitation", e);
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
