package com.gamenest.controller;

import com.gamenest.exception.ValidationException;
import com.gamenest.model.Team;
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
 * Gated by {@link com.gamenest.filter.AuthenticationFilter}. ownerAccountId
 * always comes from the session (task spec §5) — the form never submits an
 * owner/role/status field.
 */
@WebServlet(name = "TeamCreateServlet", urlPatterns = {"/account/team/create"})
public class TeamCreateServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(TeamCreateServlet.class.getName());
    private static final String VIEW = "/account/team-form.jsp";

    private final TeamService teamService = new TeamService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");
            return;
        }
        int accountId = (int) session.getAttribute("accountId");

        String name = request.getParameter("name");
        String description = request.getParameter("description");

        request.setAttribute("name", name);
        request.setAttribute("description", description);

        try {
            Team team = teamService.createTeam(accountId, name, description);
            response.sendRedirect(request.getContextPath() + "/account/team/detail?id=" + team.getTeamId());

        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while creating team", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher(VIEW).forward(request, response);
        }
    }
}
