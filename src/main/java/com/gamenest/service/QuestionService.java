package com.gamenest.service;

import com.gamenest.dao.QuestionDAO;
import com.gamenest.exception.ForbiddenException;
import com.gamenest.exception.GameNotFoundException;
import com.gamenest.exception.QuestionNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.Question;
import com.gamenest.model.QuestionStatus;

import java.sql.SQLException;
import java.util.List;

public class QuestionService {

    private static final int PAGE_SIZE = 15;
    private static final int TITLE_MAX_LENGTH = 250;
    private static final int CONTENT_MAX_LENGTH = 10000;

    private final QuestionDAO questionDAO;
    private final GameService gameService;

    public QuestionService() {
        this.questionDAO = new QuestionDAO();
        this.gameService = new GameService();
    }

    public QuestionService(QuestionDAO questionDAO, GameService gameService) {
        this.questionDAO = questionDAO;
        this.gameService = gameService;
    }

    public List<Question> listActiveByGame(int gameId, int page) throws SQLException {
        int offset = (clampPage(page) - 1) * PAGE_SIZE;
        return questionDAO.listActiveByGame(gameId, offset, PAGE_SIZE);
    }

    public int countActiveByGame(int gameId) throws SQLException {
        return questionDAO.countActiveByGame(gameId);
    }

    public List<Question> searchActiveByGame(int gameId, String query, int page) throws SQLException {
        int offset = (clampPage(page) - 1) * PAGE_SIZE;
        return questionDAO.searchActiveByGame(gameId, query == null ? "" : query.trim(), offset, PAGE_SIZE);
    }

    public int countSearchActiveByGame(int gameId, String query) throws SQLException {
        return questionDAO.countSearchActiveByGame(gameId, query == null ? "" : query.trim());
    }

    public int getPageSize() {
        return PAGE_SIZE;
    }

    public Question getQuestion(int questionId) throws QuestionNotFoundException, SQLException {
        return questionDAO.findById(questionId).orElseThrow(() -> new QuestionNotFoundException("Câu hỏi không tồn tại."));
    }

    /**
     * account_id always comes from the authenticated session — never trust a
     * client-supplied account_id. Only allowed against an existing, ACTIVE
     * game.
     */
    public Question createQuestion(int gameId, int accountId, String title, String content)
            throws ValidationException, GameNotFoundException, SQLException {

        title = title == null ? null : title.trim();
        content = content == null ? null : content.trim();

        validateTitle(title);
        validateContent(content);

        // Throws GameNotFoundException for a missing OR inactive game — same
        // rule the Games module already uses for hiding INACTIVE games.
        gameService.getActiveGameDetail(gameId);

        Question question = new Question();
        question.setGameId(gameId);
        question.setAccountId(accountId);
        question.setTitle(title);
        question.setContent(content);
        question.setStatus(QuestionStatus.ACTIVE);

        return questionDAO.insert(question);
    }

    /**
     * Editing is owner-only — ADMIN may moderate (soft-delete) content it
     * does not own, but must never silently rewrite another user's words.
     */
    public Question updateQuestion(int questionId, int requesterAccountId, String title, String content)
            throws QuestionNotFoundException, ForbiddenException, ValidationException, SQLException {

        Question question = getQuestion(questionId);
        requireOwner(question, requesterAccountId);

        title = title == null ? null : title.trim();
        content = content == null ? null : content.trim();
        validateTitle(title);
        validateContent(content);

        questionDAO.update(questionId, title, content);

        question.setTitle(title);
        question.setContent(content);
        return question;
    }

    /**
     * Owner may soft-delete their own question; ADMIN may soft-delete any
     * question as a moderation action. Never a hard delete.
     */
    public void softDeleteQuestion(int questionId, int requesterAccountId, boolean requesterIsAdmin)
            throws QuestionNotFoundException, ForbiddenException, SQLException {

        Question question = getQuestion(questionId);
        requireOwnerOrAdmin(question, requesterAccountId, requesterIsAdmin);

        int updated = questionDAO.softDelete(questionId, requesterAccountId);
        if (updated == 0) {
            throw new QuestionNotFoundException("Câu hỏi không tồn tại hoặc đã bị xóa.");
        }
    }

    /**
     * Strict ownership check for editing — ADMIN is never allowed to bypass
     * this, unlike {@link #requireOwnerOrAdmin} which governs soft-delete.
     */
    private void requireOwner(Question question, int requesterAccountId) throws ForbiddenException {
        if (question.getAccountId() != requesterAccountId) {
            throw new ForbiddenException("Bạn không có quyền thực hiện thao tác này.");
        }
    }

    private void requireOwnerOrAdmin(Question question, int requesterAccountId, boolean requesterIsAdmin)
            throws ForbiddenException {
        if (requesterIsAdmin) {
            return;
        }
        if (question.getAccountId() != requesterAccountId) {
            throw new ForbiddenException("Bạn không có quyền thực hiện thao tác này.");
        }
    }

    private int clampPage(int page) {
        return Math.max(page, 1);
    }

    private void validateTitle(String title) throws ValidationException {
        if (title == null || title.isEmpty()) {
            throw new ValidationException("Tiêu đề không được để trống.");
        }
        if (title.length() > TITLE_MAX_LENGTH) {
            throw new ValidationException("Tiêu đề không được vượt quá " + TITLE_MAX_LENGTH + " ký tự.");
        }
    }

    private void validateContent(String content) throws ValidationException {
        if (content == null || content.isEmpty()) {
            throw new ValidationException("Nội dung không được để trống.");
        }
        if (content.length() > CONTENT_MAX_LENGTH) {
            throw new ValidationException("Nội dung không được vượt quá " + CONTENT_MAX_LENGTH + " ký tự.");
        }
    }
}
