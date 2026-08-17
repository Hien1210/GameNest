package com.gamenest.controller;

import com.gamenest.exception.ForbiddenException;
import com.gamenest.exception.LFGNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.LFGPost;
import com.gamenest.service.LFGService;

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
 * Gated by {@link com.gamenest.filter.AuthenticationFilter} for
 * authentication. Editing is creator-only — enforced server-side in
 * {@link LFGService#edit}, never inferred from hidden fields or the UI
 * (mirrors QuestionEditServlet's exact owner-check convention). game_id is
 * not editable (see LFGService#edit javadoc).
 */
@WebServlet(name = "LFGEditServlet", urlPatterns = {"/lfg/edit"})
public class LFGEditServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LFGEditServlet.class.getName());
    private static final String VIEW = "/lfg/form.jsp";
    private static final String LIST_VIEW = "/lfg";

    private final LFGService lfgService = new LFGService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int lfgId = parseId(request.getParameter("id"));
        HttpSession session = request.getSession(false);
        int accountId = session == null || session.getAttribute("accountId") == null
                ? -1 : (int) session.getAttribute("accountId");

        try {
            LFGPost post = lfgService.getLfg(lfgId);
            if (post.getAccountId() != accountId) {
                request.getRequestDispatcher("/access-denied.jsp").forward(request, response);
                return;
            }

            request.setAttribute("mode", "edit");
            request.setAttribute("lfgId", post.getLfgId());
            request.setAttribute("gameName", post.getGameName());
            request.setAttribute("title", post.getTitle());
            request.setAttribute("description", post.getDescription());
            request.setAttribute("maxPlayers", post.getMaxPlayers());
            request.setAttribute("currentPlayers", post.getCurrentPlayers());

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (LFGNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            response.sendRedirect(request.getContextPath() + LIST_VIEW);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while loading LFG post for edit", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            response.sendRedirect(request.getContextPath() + LIST_VIEW);
        }
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

        int lfgId = parseId(request.getParameter("id"));
        String title = request.getParameter("title");
        String description = request.getParameter("description");
        int maxPlayers = parseId(request.getParameter("maxPlayers"));

        request.setAttribute("mode", "edit");
        request.setAttribute("lfgId", lfgId);
        request.setAttribute("title", title);
        request.setAttribute("description", description);
        request.setAttribute("maxPlayers", maxPlayers);

        try {
            lfgService.edit(lfgId, accountId, title, description, maxPlayers);
            response.sendRedirect(request.getContextPath() + "/lfg/detail?id=" + lfgId);

        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (ForbiddenException e) {
            request.getRequestDispatcher("/access-denied.jsp").forward(request, response);

        } catch (LFGNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            response.sendRedirect(request.getContextPath() + LIST_VIEW);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while updating LFG post", e);
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
