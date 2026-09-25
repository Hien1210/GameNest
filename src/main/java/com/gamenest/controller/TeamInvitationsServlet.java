package com.gamenest.controller;

import com.gamenest.model.TeamInvitation;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gated by {@link com.gamenest.filter.AuthenticationFilter} — only the
 * current session's own incoming PENDING Team invitations (task spec §18).
 * accountId never comes from the request.
 */
@WebServlet(name = "TeamInvitationsServlet", urlPatterns = {"/account/teams/invitations"})
public class TeamInvitationsServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(TeamInvitationsServlet.class.getName());
    private static final String VIEW = "/account/team-invitations.jsp";

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
        int page = parsePage(request.getParameter("page"));

        if (session.getAttribute("flashError") != null) {
            request.setAttribute("error", session.getAttribute("flashError"));
            session.removeAttribute("flashError");
        }

        try {
            List<TeamInvitation> invitations = teamService.listIncomingInvitations(accountId, page);
            int totalCount = teamService.countIncomingInvitations(accountId);
            int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / teamService.getPageSize()));

            request.setAttribute("invitations", invitations);
            request.setAttribute("page", page);
            request.setAttribute("totalPages", totalPages);

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while listing team invitations", e);
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
