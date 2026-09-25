package com.gamenest.service;

import com.gamenest.dao.AccountDAO;
import com.gamenest.dao.AnswerDAO;
import com.gamenest.dao.QuestionDAO;
import com.gamenest.dao.ReportDAO;
import com.gamenest.exception.DuplicatePendingReportException;
import com.gamenest.exception.ReportNotFoundException;
import com.gamenest.exception.ReportTargetInvalidException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.Account;
import com.gamenest.model.Answer;
import com.gamenest.model.AnswerStatus;
import com.gamenest.model.Question;
import com.gamenest.model.QuestionStatus;
import com.gamenest.model.Report;
import com.gamenest.model.ReportReason;
import com.gamenest.model.ReportStatus;
import com.gamenest.model.ReportTargetType;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Business rules for the Reports module (task spec §6). Target existence
 * checks coordinate across AccountDAO/QuestionDAO/AnswerDAO directly —
 * CLAUDE.md §4 explicitly allows a Service to coordinate multiple DAOs, and
 * target_id is a polymorphic reference with no single SQL FK, so validation
 * has to happen here rather than at the database.
 */
public class ReportService {

    private static final Logger LOGGER = Logger.getLogger(ReportService.class.getName());

    private static final int PAGE_SIZE = 20;
    private static final int DESCRIPTION_MAX_LENGTH = 1000;
    private static final int RESOLUTION_NOTE_MAX_LENGTH = 1000;
    private static final Set<String> VALID_REASONS = Set.of(
            ReportReason.SPAM, ReportReason.HARASSMENT, ReportReason.INAPPROPRIATE_CONTENT,
            ReportReason.HATE_SPEECH, ReportReason.MISINFORMATION, ReportReason.CHEATING, ReportReason.OTHER);
    private static final Set<String> VALID_TARGET_TYPES = Set.of(
            ReportTargetType.ACCOUNT, ReportTargetType.QUESTION, ReportTargetType.ANSWER);

    private final ReportDAO reportDAO;
    private final AccountDAO accountDAO;
    private final QuestionDAO questionDAO;
    private final AnswerDAO answerDAO;
    private final NotificationService notificationService;

    public ReportService() {
        this.reportDAO = new ReportDAO();
        this.accountDAO = new AccountDAO();
        this.questionDAO = new QuestionDAO();
        this.answerDAO = new AnswerDAO();
        this.notificationService = new NotificationService();
    }

    /**
     * Creates a new Report. reporterAccountId must come from the caller's
     * session — this method trusts whatever int it is given, so callers
     * must never source it from a request parameter (task spec §4/§15).
     */
    public Report createReport(int reporterAccountId, String targetType, int targetId,
                                String reason, String description)
            throws ValidationException, ReportTargetInvalidException, DuplicatePendingReportException, SQLException {

        if (targetType == null || !VALID_TARGET_TYPES.contains(targetType)) {
            throw new ValidationException("Loại đối tượng báo cáo không hợp lệ.");
        }
        validateReason(reason);
        String normalizedDescription = normalize(description, DESCRIPTION_MAX_LENGTH);

        validateTarget(targetType, targetId, reporterAccountId);

        if (reportDAO.findPendingByReporterAndTarget(reporterAccountId, targetType, targetId).isPresent()) {
            throw new DuplicatePendingReportException("Bạn đã có một báo cáo đang chờ xử lý cho đối tượng này.");
        }

        Report report = new Report();
        report.setReporterAccountId(reporterAccountId);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReason(reason);
        report.setDescription(normalizedDescription);
        report.setStatus(ReportStatus.PENDING);

        return reportDAO.insert(report);
    }

    private void validateTarget(String targetType, int targetId, int reporterAccountId)
            throws ReportTargetInvalidException, SQLException {

        switch (targetType) {
            case ReportTargetType.ACCOUNT -> {
                if (targetId == reporterAccountId) {
                    throw new ReportTargetInvalidException("Không thể tự báo cáo chính mình.");
                }
                accountDAO.findById(targetId)
                        .orElseThrow(() -> new ReportTargetInvalidException("Tài khoản không tồn tại."));
            }
            case ReportTargetType.QUESTION -> {
                Question question = questionDAO.findById(targetId)
                        .orElseThrow(() -> new ReportTargetInvalidException("Câu hỏi không tồn tại."));
                if (!QuestionStatus.ACTIVE.equals(question.getStatus())) {
                    throw new ReportTargetInvalidException("Câu hỏi không tồn tại.");
                }
                if (question.getAccountId() == reporterAccountId) {
                    throw new ReportTargetInvalidException("Không thể tự báo cáo nội dung của chính mình.");
                }
            }
            case ReportTargetType.ANSWER -> {
                Answer answer = answerDAO.findById(targetId)
                        .orElseThrow(() -> new ReportTargetInvalidException("Câu trả lời không tồn tại."));
                if (!AnswerStatus.ACTIVE.equals(answer.getStatus())) {
                    throw new ReportTargetInvalidException("Câu trả lời không tồn tại.");
                }
                if (answer.getAccountId() == reporterAccountId) {
                    throw new ReportTargetInvalidException("Không thể tự báo cáo nội dung của chính mình.");
                }
            }
            default -> throw new ReportTargetInvalidException("Loại đối tượng báo cáo không hợp lệ.");
        }
    }

    /**
     * Short, best-effort display label for the report-creation form (e.g.
     * a question's title, an account's username). Purely cosmetic — on any
     * failure this returns null and the JSP falls back to showing just the
     * target type + id, so failures here must never block report creation.
     */
    public String getTargetLabel(String targetType, int targetId) {
        try {
            if (ReportTargetType.ACCOUNT.equals(targetType)) {
                return accountDAO.findById(targetId).map(Account::getUsername).orElse(null);
            }
            if (ReportTargetType.QUESTION.equals(targetType)) {
                return questionDAO.findById(targetId).map(Question::getTitle).orElse(null);
            }
            if (ReportTargetType.ANSWER.equals(targetType)) {
                return answerDAO.findById(targetId).map(this::truncateForLabel).orElse(null);
            }
            return null;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load report target label", e);
            return null;
        }
    }

    private String truncateForLabel(Answer answer) {
        String content = answer.getContent();
        if (content == null) {
            return null;
        }
        return content.length() > 80 ? content.substring(0, 80) + "..." : content;
    }

    // ---- Admin review ----

    public void resolveReport(int reportId, int adminAccountId, String resolutionNote)
            throws ReportNotFoundException, ValidationException, SQLException {
        reviewReport(reportId, ReportStatus.RESOLVED, adminAccountId, resolutionNote);
    }

    public void rejectReport(int reportId, int adminAccountId, String resolutionNote)
            throws ReportNotFoundException, ValidationException, SQLException {
        reviewReport(reportId, ReportStatus.REJECTED, adminAccountId, resolutionNote);
    }

    /**
     * Only a PENDING report can be reviewed (task spec §11) — the DAO's
     * {@code WHERE status = 'PENDING'} guard enforces this atomically, so a
     * report that was already resolved/rejected by someone else (or does
     * not exist) surfaces here as {@link ReportNotFoundException} rather
     * than silently overwriting a prior decision.
     */
    private void reviewReport(int reportId, String newStatus, int adminAccountId, String resolutionNote)
            throws ReportNotFoundException, ValidationException, SQLException {

        String normalizedNote = normalize(resolutionNote, RESOLUTION_NOTE_MAX_LENGTH);
        int updated = reportDAO.updateResolution(reportId, newStatus, adminAccountId, normalizedNote);
        if (updated == 0) {
            throw new ReportNotFoundException("Report không tồn tại hoặc đã được xử lý.");
        }

        notifyReporter(reportId, newStatus, normalizedNote);
    }

    /**
     * REPORT_RESOLVED/REPORT_REJECTED: recipient is the Report's reporter,
     * resolved fresh here (never from the request). This runs only after
     * {@code reviewReport}'s UPDATE already committed. Re-reading the report
     * to find the reporter is itself best-effort — a failure here must never
     * surface as a failure of the resolve/reject operation that already
     * succeeded (task spec §17), same guarantee {@link NotificationService}
     * already gives its own INSERT.
     */
    private void notifyReporter(int reportId, String newStatus, String resolutionNote) {
        try {
            reportDAO.findById(reportId).ifPresent(report -> {
                if (ReportStatus.RESOLVED.equals(newStatus)) {
                    notificationService.notifyReportResolved(report.getReporterAccountId(), reportId);
                } else {
                    notificationService.notifyReportRejected(report.getReporterAccountId(), reportId, resolutionNote);
                }
            });
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load report for notification (resolution already committed): "
                    + "reportId=" + reportId, e);
        }
    }

    public Report getReportForAdmin(int reportId) throws ReportNotFoundException, SQLException {
        return reportDAO.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException("Report không tồn tại."));
    }

    public List<Report> search(String status, String reason, String targetType, String reporterUsername,
                                LocalDate dateFrom, LocalDate dateTo, int page) throws SQLException {
        int offset = (clampPage(page) - 1) * PAGE_SIZE;
        LocalDateTime from = dateFrom == null ? null : dateFrom.atStartOfDay();
        LocalDateTime toExclusive = dateTo == null ? null : dateTo.plusDays(1).atStartOfDay();
        return reportDAO.search(blankToNull(status), blankToNull(reason), blankToNull(targetType),
                blankToNull(reporterUsername), from, toExclusive, offset, PAGE_SIZE);
    }

    public int count(String status, String reason, String targetType, String reporterUsername,
                      LocalDate dateFrom, LocalDate dateTo) throws SQLException {
        LocalDateTime from = dateFrom == null ? null : dateFrom.atStartOfDay();
        LocalDateTime toExclusive = dateTo == null ? null : dateTo.plusDays(1).atStartOfDay();
        return reportDAO.count(blankToNull(status), blankToNull(reason), blankToNull(targetType),
                blankToNull(reporterUsername), from, toExclusive);
    }

    public int getPageSize() {
        return PAGE_SIZE;
    }

    private void validateReason(String reason) throws ValidationException {
        if (reason == null || !VALID_REASONS.contains(reason)) {
            throw new ValidationException("Lý do báo cáo không hợp lệ.");
        }
    }

    private String normalize(String value, int maxLength) throws ValidationException {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            throw new ValidationException("Nội dung không được vượt quá " + maxLength + " ký tự.");
        }
        return trimmed;
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
}
