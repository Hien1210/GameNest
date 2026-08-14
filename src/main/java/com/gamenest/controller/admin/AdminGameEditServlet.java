package com.gamenest.controller.admin;

import com.gamenest.exception.DuplicateGameException;
import com.gamenest.exception.GameNotFoundException;
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

@WebServlet(name = "AdminGameEditServlet", urlPatterns = {"/admin/games/edit"})
public class AdminGameEditServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdminGameEditServlet.class.getName());
    private static final String VIEW = "/admin/games/form.jsp";

    private final GameService gameService = new GameService();
    private final AuditLogService auditLogService = new AuditLogService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int gameId = parseId(request.getParameter("id"));

        try {
            Game game = gameService.getGameForAdmin(gameId);
            request.setAttribute("mode", "edit");
            request.setAttribute("gameId", game.getGameId());
            request.setAttribute("name", game.getName());
            request.setAttribute("description", game.getDescription());
            request.setAttribute("coverImageUrl", game.getCoverImageUrl());
            request.setAttribute("releaseDate", game.getReleaseDate() == null ? "" : game.getReleaseDate().toString());
            request.setAttribute("status", game.getStatus());

            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (GameNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/admin/games/list.jsp").forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while loading game for edit", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher("/admin/games/list.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int gameId = parseId(request.getParameter("id"));
        String name = request.getParameter("name");
        String description = request.getParameter("description");
        String coverImageUrl = request.getParameter("coverImageUrl");
        String releaseDateRaw = request.getParameter("releaseDate");

        request.setAttribute("mode", "edit");
        request.setAttribute("gameId", gameId);
        request.setAttribute("name", name);
        request.setAttribute("description", description);
        request.setAttribute("coverImageUrl", coverImageUrl);
        request.setAttribute("releaseDate", releaseDateRaw);

        try {
            LocalDate releaseDate = parseReleaseDate(releaseDateRaw);
            Game updated = gameService.updateGame(gameId, name, description, coverImageUrl, releaseDate);

            auditLogService.log(request, AuditModule.GAMES, AuditAction.UPDATE,
                    updated.getGameId(), AuditTargetType.GAME,
                    "đã cập nhật Game \"" + updated.getName() + "\".");

            response.sendRedirect(request.getContextPath() + "/admin/games");

        } catch (ValidationException | DuplicateGameException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (GameNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/admin/games/list.jsp").forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while updating game", e);
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

    private int parseId(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException | NullPointerException e) {
            return -1;
        }
    }
}
