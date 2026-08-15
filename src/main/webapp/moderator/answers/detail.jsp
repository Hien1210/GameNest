<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.gamenest.model.Answer" %>
<%@ page import="com.gamenest.model.AnswerStatus" %>
<%@ page import="com.gamenest.model.Question" %>
<%@ page import="com.gamenest.model.QuestionStatus" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<% request.setAttribute("pageTitle", "Chi tiết câu trả lời"); %>
<%@ include file="/WEB-INF/moderator/layout/header.jsp" %>

<div class="mod-page-header">
    <h1>Chi tiết câu trả lời</h1>
</div>

<% if (request.getAttribute("error") != null) { %>
    <div class="alert alert-error" style="margin-bottom: 16px;">
        <span class="material-symbols-outlined alert-icon">error</span>
        <span><%= HtmlUtils.escape((String) request.getAttribute("error")) %></span>
    </div>
<% } %>

<%
    Answer answer = (Answer) request.getAttribute("answer");
%>
<% if (answer == null) { %>
    <p>Câu trả lời không tồn tại.</p>
<% } else {
    String badgeClass = "mod-badge-muted";
    if (AnswerStatus.ACTIVE.equals(answer.getStatus())) badgeClass = "mod-badge-success";
    else if (AnswerStatus.HIDDEN.equals(answer.getStatus())) badgeClass = "mod-badge-warning";
    else if (AnswerStatus.DELETED.equals(answer.getStatus())) badgeClass = "mod-badge-danger";
%>

<!-- Answer Information -->
<div class="mod-detail-card">
    <h2>Thông tin câu trả lời</h2>
    <dl>
        <dt>Mã câu trả lời</dt><dd>#<%= answer.getAnswerId() %></dd>
        <dt>Nội dung</dt><dd style="white-space: pre-line;"><%= HtmlUtils.escape(answer.getContent()) %></dd>
        <dt>Tác giả</dt><dd><%= HtmlUtils.escape(answer.getAuthorUsername()) %></dd>
        <dt>Trạng thái</dt><dd><span class="mod-badge <%= badgeClass %>"><%= HtmlUtils.escape(answer.getStatus()) %></span></dd>
        <dt>Câu trả lời hay nhất</dt><dd><%= answer.isAccepted() ? "Có" : "Không" %></dd>
        <dt>Thời gian tạo</dt><dd><%= answer.getCreatedAt() != null ? answer.getCreatedAt().toString() : "-" %></dd>
        <dt>Cập nhật gần nhất</dt><dd><%= answer.getUpdatedAt() != null ? answer.getUpdatedAt().toString() : "-" %></dd>
    </dl>
</div>

<!-- Question Context (read-only) -->
<div class="mod-detail-card">
    <h2>Question liên quan</h2>
    <%
        Boolean questionMissing = (Boolean) request.getAttribute("questionMissing");
        Question question = (Question) request.getAttribute("question");
    %>
    <% if (Boolean.TRUE.equals(questionMissing) || question == null) { %>
        <p style="color: var(--text-secondary);">Question không còn tồn tại hoặc đã bị xóa.</p>
    <% } else {
        String qBadge = "mod-badge-muted";
        if (QuestionStatus.ACTIVE.equals(question.getStatus())) qBadge = "mod-badge-success";
        else if (QuestionStatus.HIDDEN.equals(question.getStatus())) qBadge = "mod-badge-warning";
        else if (QuestionStatus.LOCKED.equals(question.getStatus())) qBadge = "mod-badge-warning";
        else if (QuestionStatus.DELETED.equals(question.getStatus())) qBadge = "mod-badge-danger";
    %>
        <dl>
            <dt>Mã câu hỏi</dt><dd>#<%= question.getQuestionId() %></dd>
            <dt>Tiêu đề</dt><dd><%= HtmlUtils.escape(question.getTitle()) %></dd>
            <dt>Nội dung</dt><dd style="white-space: pre-line;"><%= HtmlUtils.escape(question.getContent()) %></dd>
            <dt>Tác giả câu hỏi</dt><dd><%= HtmlUtils.escape(question.getAuthorUsername()) %></dd>
            <dt>Game</dt><dd><%= HtmlUtils.escape(question.getGameName()) %></dd>
            <dt>Trạng thái câu hỏi</dt><dd><span class="mod-badge <%= qBadge %>"><%= HtmlUtils.escape(question.getStatus()) %></span></dd>
        </dl>
    <% } %>
</div>

<!-- Moderation Actions -->
<div class="mod-detail-card">
    <h2>Moderation</h2>
    <% if (answer.isDeleted()) { %>
        <p style="color: var(--text-secondary);">Câu trả lời này đã bị xóa (soft-delete) và không thể thay đổi trạng thái qua Moderation.</p>
    <% } else { %>
        <p style="color: var(--text-secondary); margin-bottom: 12px;">Chuyển trạng thái câu trả lời khi cần kiểm duyệt nội dung.</p>
        <div class="mod-form-actions">
            <% if (!AnswerStatus.ACTIVE.equals(answer.getStatus())) { %>
            <form action="${pageContext.request.contextPath}/moderator/answers/status" method="post">
                <input type="hidden" name="id" value="<%= answer.getAnswerId() %>">
                <input type="hidden" name="action" value="activate">
                <button type="submit" class="mod-btn">Kích hoạt (ACTIVE)</button>
            </form>
            <% } %>
            <% if (!AnswerStatus.HIDDEN.equals(answer.getStatus())) { %>
            <form action="${pageContext.request.contextPath}/moderator/answers/status" method="post">
                <input type="hidden" name="id" value="<%= answer.getAnswerId() %>">
                <input type="hidden" name="action" value="hide">
                <button type="submit" class="mod-btn-secondary">Ẩn (HIDDEN)</button>
            </form>
            <% } %>
        </div>
    <% } %>
</div>
<% } %>

<div class="mod-back-link">
    <a href="${pageContext.request.contextPath}/moderator/answers">&larr; Quay lại danh sách</a>
</div>

<%@ include file="/WEB-INF/moderator/layout/footer.jsp" %>
