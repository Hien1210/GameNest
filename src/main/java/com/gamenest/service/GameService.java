package com.gamenest.service;

import com.gamenest.dao.GameDAO;
import com.gamenest.exception.DuplicateGameException;
import com.gamenest.exception.GameNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.Game;
import com.gamenest.model.GameStatus;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class GameService {

    private static final int PAGE_SIZE = 12;
    private static final int ADMIN_PAGE_SIZE = 20;
    private static final int NAME_MAX_LENGTH = 150;
    private static final int COVER_URL_MAX_LENGTH = 500;
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1950, 1, 1);

    private final GameDAO gameDAO;

    public GameService() {
        this.gameDAO = new GameDAO();
    }

    public GameService(GameDAO gameDAO) {
        this.gameDAO = gameDAO;
    }

    // ---- User-facing (ACTIVE only) ----

    public List<Game> listActiveGames(int page) throws SQLException {
        int offset = (clampPage(page) - 1) * PAGE_SIZE;
        return gameDAO.findActive(offset, PAGE_SIZE);
    }

    public int countActiveGames() throws SQLException {
        return gameDAO.countActive();
    }

    public List<Game> searchActiveGames(String query, int page) throws SQLException {
        int offset = (clampPage(page) - 1) * PAGE_SIZE;
        return gameDAO.searchActiveByName(query == null ? "" : query.trim(), offset, PAGE_SIZE);
    }

    public int countSearchActiveGames(String query) throws SQLException {
        return gameDAO.countSearchActiveByName(query == null ? "" : query.trim());
    }

    public int getPageSize() {
        return PAGE_SIZE;
    }

    /**
     * Only ever returns an ACTIVE game — an INACTIVE game is treated as not
     * found for normal users, so it disappears from both listings and direct
     * links.
     */
    public Game getActiveGameDetail(int gameId) throws GameNotFoundException, SQLException {
        Game game = gameDAO.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game không tồn tại."));
        if (!GameStatus.ACTIVE.equals(game.getStatus())) {
            throw new GameNotFoundException("Game không tồn tại.");
        }
        return game;
    }

    // ---- Admin (any status) ----

    public List<Game> listAllGamesForAdmin(int page) throws SQLException {
        int offset = (clampPage(page) - 1) * ADMIN_PAGE_SIZE;
        return gameDAO.findAllForAdmin(offset, ADMIN_PAGE_SIZE);
    }

    public int countAllGamesForAdmin() throws SQLException {
        return gameDAO.countAllForAdmin();
    }

    public int getAdminPageSize() {
        return ADMIN_PAGE_SIZE;
    }

    public Game getGameForAdmin(int gameId) throws GameNotFoundException, SQLException {
        return gameDAO.findById(gameId).orElseThrow(() -> new GameNotFoundException("Game không tồn tại."));
    }

    /**
     * Creates a new game. Status is validated against the known enum values
     * rather than trusted verbatim from the form.
     */
    public Game createGame(String name, String description, String coverImageUrl, LocalDate releaseDate,
                            String status) throws ValidationException, DuplicateGameException, SQLException {

        name = name == null ? null : name.trim();
        description = normalizeOptional(description);
        coverImageUrl = normalizeOptional(coverImageUrl);

        validateName(name);
        validateCoverImageUrl(coverImageUrl);
        validateReleaseDate(releaseDate);
        String validStatus = validateStatus(status);

        Game game = new Game();
        game.setName(name);
        game.setDescription(description);
        game.setCoverImageUrl(coverImageUrl);
        game.setReleaseDate(releaseDate);
        game.setStatus(validStatus);

        return gameDAO.insert(game);
    }

    public Game updateGame(int gameId, String name, String description, String coverImageUrl,
                            LocalDate releaseDate)
            throws ValidationException, DuplicateGameException, GameNotFoundException, SQLException {

        Game existing = getGameForAdmin(gameId);

        name = name == null ? null : name.trim();
        description = normalizeOptional(description);
        coverImageUrl = normalizeOptional(coverImageUrl);

        validateName(name);
        validateCoverImageUrl(coverImageUrl);
        validateReleaseDate(releaseDate);

        existing.setName(name);
        existing.setDescription(description);
        existing.setCoverImageUrl(coverImageUrl);
        existing.setReleaseDate(releaseDate);

        gameDAO.update(existing);
        return existing;
    }

    public void activateGame(int gameId) throws GameNotFoundException, SQLException {
        setStatus(gameId, GameStatus.ACTIVE);
    }

    public void deactivateGame(int gameId) throws GameNotFoundException, SQLException {
        setStatus(gameId, GameStatus.INACTIVE);
    }

    private void setStatus(int gameId, String status) throws GameNotFoundException, SQLException {
        int updated = gameDAO.updateStatus(gameId, status);
        if (updated == 0) {
            throw new GameNotFoundException("Game không tồn tại.");
        }
    }

    private int clampPage(int page) {
        return Math.max(page, 1);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateName(String name) throws ValidationException {
        if (name == null || name.isEmpty()) {
            throw new ValidationException("Tên game không được để trống.");
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new ValidationException("Tên game không được vượt quá " + NAME_MAX_LENGTH + " ký tự.");
        }
    }

    private void validateCoverImageUrl(String coverImageUrl) throws ValidationException {
        if (coverImageUrl != null && coverImageUrl.length() > COVER_URL_MAX_LENGTH) {
            throw new ValidationException("URL ảnh bìa không được vượt quá " + COVER_URL_MAX_LENGTH + " ký tự.");
        }
    }

    private void validateReleaseDate(LocalDate releaseDate) throws ValidationException {
        if (releaseDate == null) {
            return;
        }
        if (releaseDate.isBefore(MIN_RELEASE_DATE) || releaseDate.isAfter(LocalDate.now().plusYears(5))) {
            throw new ValidationException("Ngày phát hành không hợp lệ.");
        }
    }

    private String validateStatus(String status) throws ValidationException {
        if (status == null || status.isBlank()) {
            return GameStatus.ACTIVE;
        }
        if (!GameStatus.ACTIVE.equals(status) && !GameStatus.INACTIVE.equals(status)) {
            throw new ValidationException("Trạng thái game không hợp lệ.");
        }
        return status;
    }
}
