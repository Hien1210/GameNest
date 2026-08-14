package com.gamenest.controller.admin;

import com.gamenest.exception.DuplicateGameException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.AuditAction;
import com.gamenest.model.AuditModule;
import com.gamenest.model.AuditTargetType;
import com.gamenest.model.Game;
import com.gamenest.service.AuditLogService;
import com.gamenest.service.GameService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "AdminGameCreateServlet", urlPatterns = {"/admin/games/new"})
public class AdminGameCreateServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdminGameCreateServlet.class.getName());
    private static final String VIEW = "/admin/games/form.jsp";

    private final GameService gameService = new GameService();
    private final AuditLogService auditLogService = new AuditLogService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("mode", "create");
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String name = request.getParameter("name");
        String description = request.getParameter("description");
        String coverImageUrl = request.getParameter("coverImageUrl");
        String releaseDateRaw = request.getParameter("releaseDate");
        String status = request.getParameter("status");

        request.setAttribute("mode", "create");
        request.setAttribute("name", name);
        request.setAttribute("description", description);
        request.setAttribute("coverImageUrl", coverImageUrl);
        request.setAttribute("releaseDate", releaseDateRaw);
        request.setAttribute("status", status);

        try {
            LocalDate releaseDate = parseReleaseDate(releaseDateRaw);
            Game created = gameService.createGame(name, description, coverImageUrl, releaseDate, status);

            auditLogService.log(request, AuditModule.GAMES, AuditAction.CREATE,
                    created.getGameId(), AuditTargetType.GAME,
                    "đã tạo Game \"" + created.getName() + "\".");

            response.sendRedirect(request.getContextPath() + "/admin/games");

        } catch (ValidationException | DuplicateGameException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while creating game", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher(VIEW).forward(request, response);
        }
    }

    private LocalDate parseReleaseDate(String raw) throws ValidationException {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("Ngày phát hành không hợp lệ.");
        }
    }
}
