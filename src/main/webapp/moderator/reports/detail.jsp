<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.gamenest.model.Report" %>
<%@ page import="com.gamenest.model.ReportStatus" %>
<%@ page import="com.gamenest.model.ReportReason" %>
<%@ page import="com.gamenest.model.ReportTargetType" %>
<%@ page import="com.gamenest.model.Account" %>
<%@ page import="com.gamenest.model.AccountStatus" %>
<%@ page import="com.gamenest.model.Question" %>
<%@ page import="com.gamenest.model.QuestionStatus" %>
<%@ page import="com.gamenest.model.Answer" %>
<%@ page import="com.gamenest.model.AnswerStatus" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<% request.setAttribute("pageTitle", "Chi tiết báo cáo"); %>
<%@ include file="/WEB-INF/moderator/layout/header.jsp" %>

<div class="mod-page-header">
    <h1>Chi tiết báo cáo</h1>
</div>

<% if (request.getAttribute("error") != null) { %>
    <div class="alert alert-error" style="margin-bottom: 16px;">
        <span class="material-symbols-outlined alert-icon">error</span>
        <span><%= HtmlUtils.escape((String) request.getAttribute("error")) %></span>
    </div>
<% } %>

<%
    Report report = (Report) request.getAttribute("report");
%>
<% if (report == null) { %>
    <p>Báo cáo không tồn tại.</p>
<% } else {
    String badgeClass = "mod-badge-muted";
    String statusText = report.getStatus();
    if (ReportStatus.PENDING.equals(report.getStatus())) {
        badgeClass = "mod-badge-warning";
        statusText = "Chờ xử lý";
    } else if (ReportStatus.RESOLVED.equals(report.getStatus())) {
        badgeClass = "mod-badge-success";
        statusText = "Đã giải quyết";
    } else if (ReportStatus.REJECTED.equals(report.getStatus())) {
        badgeClass = "mod-badge-danger";
        statusText = "Đã từ chối";
    }

    String targetTypeText = report.getTargetType();
    if (ReportTargetType.ACCOUNT.equals(targetTypeText)) targetTypeText = "Tài khoản";
    else if (ReportTargetType.QUESTION.equals(targetTypeText)) targetTypeText = "Câu hỏi";
    else if (ReportTargetType.ANSWER.equals(targetTypeText)) targetTypeText = "Câu trả lời";

    String reasonText = report.getReason();
    if (ReportReason.SPAM.equals(reasonText)) reasonText = "Spam / Rác";
    else if (ReportReason.HARASSMENT.equals(reasonText)) reasonText = "Quấy rối / Xúc phạm";
    else if (ReportReason.INAPPROPRIATE_CONTENT.equals(reasonText)) reasonText = "Nội dung không phù hợp";
    else if (ReportReason.HATE_SPEECH.equals(reasonText)) reasonText = "Ngôn từ thù ghét";
    else if (ReportReason.MISINFORMATION.equals(reasonText)) reasonText = "Thông tin sai lệch";
    else if (ReportReason.CHEATING.equals(reasonText)) reasonText = "Gian lận";
    else if (ReportReason.OTHER.equals(reasonText)) reasonText = "Khác";
%>

<!-- Report Information -->
<div class="mod-detail-card">
    <h2>Thông tin báo cáo</h2>
    <dl>
        <dt>Mã báo cáo</dt><dd>#<%= report.getReportId() %></dd>
        <dt>Người báo cáo</dt><dd><%= HtmlUtils.escape(report.getReporterUsername()) %></dd>
        <dt>Loại mục tiêu</dt><dd><%= HtmlUtils.escape(targetTypeText) %></dd>
        <dt>Mã mục tiêu</dt><dd>#<%= report.getTargetId() %></dd>
        <dt>Lý do</dt><dd><%= HtmlUtils.escape(reasonText) %></dd>
        <dt>Mô tả</dt>
        <dd><%= report.getDescription() != null ? HtmlUtils.escape(report.getDescription()) : "-" %></dd>
        <dt>Trạng thái</dt><dd><span class="mod-badge <%= badgeClass %>"><%= statusText %></span></dd>
        <dt>Thời gian tạo</dt><dd><%= report.getCreatedAt() != null ? report.getCreatedAt().toString() : "-" %></dd>
    </dl>
</div>

<!-- Reported Target -->
<div class="mod-detail-card">
    <h2>Đối tượng bị báo cáo</h2>
    <%
        Boolean targetMissing = (Boolean) request.getAttribute("targetMissing");
        Account targetAccount = (Account) request.getAttribute("targetAccount");
        Question targetQuestion = (Question) request.getAttribute("targetQuestion");
        Answer targetAnswer = (Answer) request.getAttribute("targetAnswer");
    %>
    <% if (Boolean.TRUE.equals(targetMissing)) { %>
        <p style="color: var(--text-secondary);">Target không còn tồn tại hoặc đã bị xóa.</p>
    <% } else if (targetAccount != null) {
        String accBadge = "mod-badge-muted";
        if (AccountStatus.ACTIVE.equals(targetAccount.getStatus())) accBadge = "mod-badge-success";
        else if (AccountStatus.BANNED.equals(targetAccount.getStatus())) accBadge = "mod-badge-danger";
        else if (AccountStatus.SUSPENDED.equals(targetAccount.getStatus())) accBadge = "mod-badge-warning";
    %>
        <dl>
            <dt>Username</dt><dd><%= HtmlUtils.escape(targetAccount.getUsername()) %></dd>
            <dt>Tên hiển thị</dt><dd><%= HtmlUtils.escape(targetAccount.getDisplayName()) %></dd>
            <dt>Email</dt><dd><%= HtmlUtils.escape(targetAccount.getEmail()) %></dd>
            <dt>Vai trò</dt><dd><%= HtmlUtils.escape(targetAccount.getRole()) %></dd>
            <dt>Trạng thái</dt><dd><span class="mod-badge <%= accBadge %>"><%= HtmlUtils.escape(targetAccount.getStatus()) %></span></dd>
            <dt>Ngày tạo</dt><dd><%= targetAccount.getCreatedAt() != null ? targetAccount.getCreatedAt().toString() : "-" %></dd>
        </dl>
    <% } else if (targetQuestion != null) {
        String qBadge = "mod-badge-muted";
        if (QuestionStatus.ACTIVE.equals(targetQuestion.getStatus())) qBadge = "mod-badge-success";
        else if (QuestionStatus.HIDDEN.equals(targetQuestion.getStatus())) qBadge = "mod-badge-warning";
        else if (QuestionStatus.LOCKED.equals(targetQuestion.getStatus())) qBadge = "mod-badge-warning";
        else if (QuestionStatus.DELETED.equals(targetQuestion.getStatus())) qBadge = "mod-badge-danger";
    %>
        <dl>
            <dt>Tiêu đề</dt><dd><%= HtmlUtils.escape(targetQuestion.getTitle()) %></dd>
            <dt>Nội dung</dt><dd style="white-space: pre-line;"><%= HtmlUtils.escape(targetQuestion.getContent()) %></dd>
            <dt>Tác giả</dt><dd><%= HtmlUtils.escape(targetQuestion.getAuthorUsername()) %></dd>
            <dt>Game</dt><dd><%= HtmlUtils.escape(targetQuestion.getGameName()) %></dd>
            <dt>Trạng thái</dt><dd><span class="mod-badge <%= qBadge %>"><%= HtmlUtils.escape(targetQuestion.getStatus()) %></span></dd>
            <dt>Ngày tạo</dt><dd><%= targetQuestion.getCreatedAt() != null ? targetQuestion.getCreatedAt().toString() : "-" %></dd>
        </dl>
    <% } else if (targetAnswer != null) {
        String aBadge = "mod-badge-muted";
        if (AnswerStatus.ACTIVE.equals(targetAnswer.getStatus())) aBadge = "mod-badge-success";
        else if (AnswerStatus.HIDDEN.equals(targetAnswer.getStatus())) aBadge = "mod-badge-warning";
        else if (AnswerStatus.DELETED.equals(targetAnswer.getStatus())) aBadge = "mod-badge-danger";
    %>
        <dl>
            <dt>Nội dung</dt><dd style="white-space: pre-line;"><%= HtmlUtils.escape(targetAnswer.getContent()) %></dd>
            <dt>Tác giả</dt><dd><%= HtmlUtils.escape(targetAnswer.getAuthorUsername()) %></dd>
            <dt>Thuộc câu hỏi</dt><dd>#<%= targetAnswer.getQuestionId() %></dd>
            <dt>Trạng thái</dt><dd><span class="mod-badge <%= aBadge %>"><%= HtmlUtils.escape(targetAnswer.getStatus()) %></span></dd>
            <dt>Ngày tạo</dt><dd><%= targetAnswer.getCreatedAt() != null ? targetAnswer.getCreatedAt().toString() : "-" %></dd>
        </dl>
    <% } else { %>
        <p style="color: var(--text-secondary);">Target không còn tồn tại hoặc đã bị xóa.</p>
    <% } %>
</div>

<!-- Moderator Decision / Actions -->
<div class="mod-detail-card">
    <h2>Quyết định của Moderator</h2>
    <% if (ReportStatus.PENDING.equals(report.getStatus())) { %>
        <form action="${pageContext.request.contextPath}/moderator/reports/resolve" method="post">
            <input type="hidden" name="id" value="<%= report.getReportId() %>">
            <div class="mod-form-group">
                <label for="resolutionNote">Ghi chú xử lý (tùy chọn)</label>
                <textarea id="resolutionNote" name="resolutionNote" maxlength="1000"></textarea>
            </div>
            <div class="mod-form-actions">
                <button type="submit" name="action" value="resolve" class="mod-btn">Resolve</button>
                <button type="submit" name="action" value="reject" class="mod-btn-danger">Reject</button>
            </div>
        </form>
    <% } else { %>
        <dl>
            <dt>Kết quả</dt><dd><span class="mod-badge <%= badgeClass %>"><%= statusText %></span></dd>
            <dt>Người xét duyệt</dt>
            <dd><%= report.getReviewedByUsername() != null ? HtmlUtils.escape(report.getReviewedByUsername()) : "-" %></dd>
            <dt>Thời gian xét duyệt</dt>
            <dd><%= report.getReviewedAt() != null ? report.getReviewedAt().toString() : "-" %></dd>
            <dt>Ghi chú xử lý</dt>
            <dd><%= report.getResolutionNote() != null ? HtmlUtils.escape(report.getResolutionNote()) : "-" %></dd>
        </dl>
    <% } %>
</div>
<% } %>

<div class="mod-back-link">
    <a href="${pageContext.request.contextPath}/moderator/reports">&larr; Quay lại danh sách</a>
</div>

<%@ include file="/WEB-INF/moderator/layout/footer.jsp" %>
