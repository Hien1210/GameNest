package com.gamenest.controller;

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
 * Gated by {@link com.gamenest.filter.AuthenticationFilter}. accountId
 * always comes from the session — {@link TeamService#leaveTeam} deletes
 * only the caller's own MEMBER row (task spec §13/§23). The OWNER is
 * rejected with a friendly message rather than silently doing nothing —
 * see TeamService's Javadoc for the Leave-vs-Owner decision.
 */
@WebServlet(name = "TeamLeaveServlet", urlPatterns = {"/account/team/leave"})
public class TeamLeaveServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(TeamLeaveServlet.class.getName());

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
        int teamId = parseId(request.getParameter("teamId"));

        try {
            teamService.leaveTeam(teamId, accountId);
            response.sendRedirect(request.getContextPath() + "/account/teams");
            return;

        } catch (ValidationException e) {
            session.setAttribute("flashError", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/account/team/detail?id=" + teamId);
            return;

        } catch (TeamNotFoundException e) {
            session.setAttribute("flashError", e.getMessage());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while leaving team", e);
            session.setAttribute("flashError", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
        }

        response.sendRedirect(request.getContextPath() + "/account/teams");
    }

    private int parseId(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException | NullPointerException e) {
            return -1;
        }
    }
}
