package com.gamenest.controller;

import com.gamenest.exception.LFGNotFoundException;
import com.gamenest.model.LFGMember;
import com.gamenest.model.LFGPost;
import com.gamenest.model.LFGStatus;
import com.gamenest.service.LFGService;

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
 * Public LFG detail (viewing does not require login; task spec §14).
 * isCreator/isMember/canJoin are computed here purely to drive which
 * buttons the JSP shows — the actual authorization check happens again,
 * independently, server-side in each action servlet (Join/Leave/Edit/
 * Close/Delete). The UI hint here is never the security boundary.
 */
@WebServlet(name = "LFGDetailServlet", urlPatterns = {"/lfg/detail"})
public class LFGDetailServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LFGDetailServlet.class.getName());
    private static final String VIEW = "/lfg/detail.jsp";
    private static final String LIST_VIEW = "/lfg";

    private final LFGService lfgService = new LFGService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int lfgId = parseId(request.getParameter("id"));

        HttpSession session = request.getSession(false);
        Object accountIdAttr = session == null ? null : session.getAttribute("accountId");
        boolean isLoggedIn = accountIdAttr instanceof Integer;

        if (session != null && session.getAttribute("flashError") != null) {
            request.setAttribute("error", session.getAttribute("flashError"));
            session.removeAttribute("flashError");
        }

        try {
            LFGPost post = lfgService.getLfg(lfgId);
            List<LFGMember> members = lfgService.listMembers(lfgId);

            boolean isCreator = isLoggedIn && post.getAccountId() == (Integer) accountIdAttr;
            boolean isMember = isLoggedIn && members.stream()
                    .anyMatch(m -> m.getAccountId() == (Integer) accountIdAttr);
            boolean canJoin = isLoggedIn && !isCreator && !isMember && LFGStatus.OPEN.equals(post.getStatus());
            boolean canLeave = isLoggedIn && !isCreator && isMember && !LFGStatus.DELETED.equals(post.getStatus());
            boolean canManage = isCreator && !LFGStatus.DELETED.equals(post.getStatus());

            request.setAttribute("post", post);
            request.setAttribute("members", members);
            request.setAttribute("isLoggedIn", isLoggedIn);
            request.setAttribute("isCreator", isCreator);
            request.setAttribute("canJoin", canJoin);
            request.setAttribute("canLeave", canLeave);
            request.setAttribute("canManage", canManage);

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (LFGNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            response.sendRedirect(request.getContextPath() + LIST_VIEW);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while loading LFG detail", e);
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
