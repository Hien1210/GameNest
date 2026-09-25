package com.gamenest.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Moderator Portal landing page. This is a shell/foundation only — no
 * moderation module exists yet, so this page must never show fabricated
 * statistics or fake data (task spec §9). Access is gated by
 * {@link com.gamenest.filter.ModeratorAuthorizationFilter} on /moderator/*.
 */
@WebServlet(name = "ModeratorDashboardServlet", urlPatterns = {"/moderator/dashboard"})
public class ModeratorDashboardServlet extends HttpServlet {

    private static final String VIEW = "/moderator/dashboard.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
