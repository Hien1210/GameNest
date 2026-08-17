package com.gamenest.controller;

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
 * Gated by {@link com.gamenest.filter.AuthenticationFilter}. Owner-only —
 * enforced in {@link TeamService#removeMember}, not by hiding the button. A
 * MEMBER calling this endpoint directly gets {@link ForbiddenException}
 * (task spec §14).
 */
@WebServlet(name = "TeamRemoveMemberServlet", urlPatterns = {"/account/team/remove-member"})
public class TeamRemoveMemberServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(TeamRemoveMemberServlet.class.getName());

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
        int targetAccountId = parseId(request.getParameter("targetAccountId"));

        try {
            teamService.removeMember(teamId, accountId, targetAccountId);

        } catch (ForbiddenException e) {
            request.getRequestDispatcher("/access-denied.jsp").forward(request, response);
            return;

        } catch (ValidationException e) {
            session.setAttribute("flashError", e.getMessage());

        } catch (TeamNotFoundException e) {
            session.setAttribute("flashError", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/account/teams");
            return;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while removing team member", e);
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
