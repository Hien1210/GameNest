package com.gamenest.controller;

import com.gamenest.exception.ForbiddenException;
import com.gamenest.exception.TeamNotFoundException;
import com.gamenest.model.Team;
import com.gamenest.model.TeamInvitation;
import com.gamenest.model.TeamMember;
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
 * Gated by {@link com.gamenest.filter.AuthenticationFilter} — Team Detail is
 * private, members-only (task spec §17 — a private Friend room has no
 * anonymous/non-member browsing requested anywhere in this task, unlike
 * Public Profile). Non-members are forwarded to /access-denied.jsp, exactly
 * like other Owner/member-only pages in this project.
 */
@WebServlet(name = "TeamDetailServlet", urlPatterns = {"/account/team/detail"})
public class TeamDetailServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(TeamDetailServlet.class.getName());
    private static final String VIEW = "/account/team-detail.jsp";
    private static final String LIST_VIEW = "/account/teams";

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
        int teamId = parseId(request.getParameter("id"));

        if (session.getAttribute("flashError") != null) {
            request.setAttribute("error", session.getAttribute("flashError"));
            session.removeAttribute("flashError");
        }

        try {
            List<TeamMember> members = teamService.listMembers(teamId, accountId);
            Team team = teamService.getTeam(teamId);
            boolean isOwner = team.getOwnerAccountId() == accountId;

            request.setAttribute("team", team);
            request.setAttribute("members", members);
            request.setAttribute("isOwner", isOwner);

            if (isOwner) {
                List<TeamInvitation> pendingInvitations = teamService.listPendingInvitationsForTeam(teamId, accountId);
                request.setAttribute("pendingInvitations", pendingInvitations);
            }

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (ForbiddenException e) {
            request.getRequestDispatcher("/access-denied.jsp").forward(request, response);

        } catch (TeamNotFoundException e) {
            session.setAttribute("flashError", e.getMessage());
            response.sendRedirect(request.getContextPath() + LIST_VIEW);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while loading team detail", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher(VIEW).forward(request, response);
        }
    }

    private int parseId(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException | NullPointerException e) {
            return -1;
        }
    }
}
