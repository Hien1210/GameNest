package com.gamenest.service;

import com.gamenest.dao.AnswerDAO;
import com.gamenest.dao.QuestionDAO;
import com.gamenest.exception.AnswerNotFoundException;
import com.gamenest.exception.ForbiddenException;
import com.gamenest.exception.QuestionNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.Answer;
import com.gamenest.model.AnswerStatus;
import com.gamenest.model.Question;
import com.gamenest.model.QuestionStatus;
import com.gamenest.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class AnswerService {

    private static final int CONTENT_MAX_LENGTH = 10000;
    private static final int MODERATION_PAGE_SIZE = 20;

    private final AnswerDAO answerDAO;
    private final QuestionDAO questionDAO;

    public AnswerService() {
        this.answerDAO = new AnswerDAO();
        this.questionDAO = new QuestionDAO();
    }

    public AnswerService(AnswerDAO answerDAO, QuestionDAO questionDAO) {
        this.answerDAO = answerDAO;
        this.questionDAO = questionDAO;
    }

    public List<Answer> listActiveByQuestion(int questionId) throws SQLException {
        return answerDAO.listActiveByQuestion(questionId);
    }

    public Answer getAnswer(int answerId) throws AnswerNotFoundException, SQLException {
        return answerDAO.findById(answerId)
                .orElseThrow(() -> new AnswerNotFoundException("Câu trả lời không tồn tại."));
    }

    /**
     * account_id always comes from the authenticated session. Rejects
     * answers on a question that does not exist or is not ACTIVE (deleted,
     * hidden, or locked questions no longer accept new answers).
     */
    public Answer createAnswer(int questionId, int accountId, String content)
            throws ValidationException, QuestionNotFoundException, SQLException {

        content = content == null ? null : content.trim();
        validateContent(content);

        Question question = questionDAO.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException("Câu hỏi không tồn tại."));
        if (!QuestionStatus.ACTIVE.equals(question.getStatus())) {
            throw new QuestionNotFoundException("Câu hỏi này không còn nhận câu trả lời mới.");
        }

        Answer answer = new Answer();
        answer.setQuestionId(questionId);
        answer.setAccountId(accountId);
        answer.setContent(content);
        answer.setStatus(AnswerStatus.ACTIVE);

        return answerDAO.insert(answer);
    }

    /**
     * Editing is owner-only — ADMIN may moderate (soft-delete) content it
     * does not own, but must never silently rewrite another user's words.
     */
    public Answer updateAnswer(int answerId, int requesterAccountId, String content)
            throws AnswerNotFoundException, ForbiddenException, ValidationException, SQLException {

        Answer answer = getAnswer(answerId);
        requireOwner(answer, requesterAccountId);

        content = content == null ? null : content.trim();
        validateContent(content);

        answerDAO.update(answerId, content);
        answer.setContent(content);
        return answer;
    }

    /**
     * Owner may soft-delete their own answer; ADMIN may soft-delete any
     * answer as a moderation action. Never a hard delete.
     */
    public void softDeleteAnswer(int answerId, int requesterAccountId, boolean requesterIsAdmin)
            throws AnswerNotFoundException, ForbiddenException, SQLException {

        Answer answer = getAnswer(answerId);
        requireOwnerOrAdmin(answer, requesterAccountId, requesterIsAdmin);

        int updated = answerDAO.softDelete(answerId, requesterAccountId);
        if (updated == 0) {
            throw new AnswerNotFoundException("Câu trả lời không tồn tại hoặc đã bị xóa.");
        }
    }

    /**
     * Only the question owner may accept an answer, and only one answer per
     * question may be accepted at a time — both statements run in a single
     * transaction so that invariant is never briefly violated.
     */
    public void acceptAnswer(int questionId, int answerId, int requesterAccountId)
            throws QuestionNotFoundException, AnswerNotFoundException, ForbiddenException, SQLException {

        Question question = questionDAO.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException("Câu hỏi không tồn tại."));
        if (question.getAccountId() != requesterAccountId) {
            throw new ForbiddenException("Chỉ người tạo câu hỏi mới có thể chọn câu trả lời hay nhất.");
        }

        Answer answer = getAnswer(answerId);
        if (answer.getQuestionId() != questionId) {
            throw new AnswerNotFoundException("Câu trả lời không thuộc câu hỏi này.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                answerDAO.unacceptAllForQuestion(conn, questionId);
                answerDAO.setAccepted(conn, answerId, true);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void unacceptAnswer(int questionId, int answerId, int requesterAccountId)
            throws QuestionNotFoundException, AnswerNotFoundException, ForbiddenException, SQLException {

        Question question = questionDAO.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException("Câu hỏi không tồn tại."));
        if (question.getAccountId() != requesterAccountId) {
            throw new ForbiddenException("Chỉ người tạo câu hỏi mới có thể bỏ chọn câu trả lời hay nhất.");
        }

        Answer answer = getAnswer(answerId);
        if (answer.getQuestionId() != questionId) {
            throw new AnswerNotFoundException("Câu trả lời không thuộc câu hỏi này.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            answerDAO.setAccepted(conn, answerId, false);
        }
    }

    // ---- Moderation (any status, any question/game) ----

    public List<Answer> searchForModeration(String status, Integer questionId, Integer gameId,
                                             String authorUsername, String searchText, int page) throws SQLException {
        int offset = (clampPage(page) - 1) * MODERATION_PAGE_SIZE;
        return answerDAO.searchForModeration(blankToNull(status), questionId, gameId, blankToNull(authorUsername),
                blankToNull(searchText), offset, MODERATION_PAGE_SIZE);
    }

    public int countForModeration(String status, Integer questionId, Integer gameId, String authorUsername,
                                   String searchText) throws SQLException {
        return answerDAO.countForModeration(blankToNull(status), questionId, gameId, blankToNull(authorUsername),
                blankToNull(searchText));
    }

    public int getModerationPageSize() {
        return MODERATION_PAGE_SIZE;
    }

    /**
     * Moderator status transition — ACTIVE/HIDDEN only, matching the real
     * CK_Answers_status values (no LOCKED for Answers, unlike Questions).
     * Deliberately does not accept DELETED: that status carries its own
     * is_deleted/deleted_at/deleted_by bookkeeping, owned exclusively by
     * {@link #softDeleteAnswer} (owner-or-admin today) — out of scope for
     * this task. A currently soft-deleted answer is rejected too: setting
     * status back to ACTIVE/HIDDEN via this path would leave is_deleted=1
     * alongside a non-DELETED status, an inconsistent state.
     */
    public void changeStatusForModeration(int answerId, String newStatus)
            throws AnswerNotFoundException, ValidationException, SQLException {

        if (!AnswerStatus.ACTIVE.equals(newStatus) && !AnswerStatus.HIDDEN.equals(newStatus)) {
            throw new ValidationException("Trạng thái không hợp lệ.");
        }

        Answer current = getAnswer(answerId);
        if (current.isDeleted()) {
            throw new ValidationException("Không thể thay đổi trạng thái của câu trả lời đã bị xóa.");
        }

        int updated = answerDAO.updateStatus(answerId, newStatus);
        if (updated == 0) {
            throw new AnswerNotFoundException("Câu trả lời không tồn tại.");
        }
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int clampPage(int page) {
        return Math.max(page, 1);
    }

    /**
     * Strict ownership check for editing — ADMIN is never allowed to bypass
     * this, unlike {@link #requireOwnerOrAdmin} which governs soft-delete.
     */
    private void requireOwner(Answer answer, int requesterAccountId) throws ForbiddenException {
        if (answer.getAccountId() != requesterAccountId) {
            throw new ForbiddenException("Bạn không có quyền thực hiện thao tác này.");
        }
    }

    private void requireOwnerOrAdmin(Answer answer, int requesterAccountId, boolean requesterIsAdmin)
            throws ForbiddenException {
        if (requesterIsAdmin) {
            return;
        }
        if (answer.getAccountId() != requesterAccountId) {
            throw new ForbiddenException("Bạn không có quyền thực hiện thao tác này.");
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
