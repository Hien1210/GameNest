<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.gamenest.model.Question" %>
<%@ page import="com.gamenest.model.QuestionStatus" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<% request.setAttribute("pageTitle", "Chi tiết câu hỏi"); %>
<%@ include file="/WEB-INF/moderator/layout/header.jsp" %>

<div class="mod-page-header">
    <h1>Chi tiết câu hỏi</h1>
</div>

<% if (request.getAttribute("error") != null) { %>
    <div class="alert alert-error" style="margin-bottom: 16px;">
        <span class="material-symbols-outlined alert-icon">error</span>
        <span><%= HtmlUtils.escape((String) request.getAttribute("error")) %></span>
    </div>
<% } %>

<%
    Question question = (Question) request.getAttribute("question");
%>
<% if (question == null) { %>
    <p>Câu hỏi không tồn tại.</p>
<% } else {
    String badgeClass = "mod-badge-muted";
    if (QuestionStatus.ACTIVE.equals(question.getStatus())) badgeClass = "mod-badge-success";
    else if (QuestionStatus.HIDDEN.equals(question.getStatus())) badgeClass = "mod-badge-warning";
    else if (QuestionStatus.DELETED.equals(question.getStatus())) badgeClass = "mod-badge-danger";
%>

<!-- Question Information -->
<div class="mod-detail-card">
    <h2>Thông tin câu hỏi</h2>
    <dl>
        <dt>Mã câu hỏi</dt><dd>#<%= question.getQuestionId() %></dd>
        <dt>Tiêu đề</dt><dd><%= HtmlUtils.escape(question.getTitle()) %></dd>
        <dt>Nội dung</dt><dd style="white-space: pre-line;"><%= HtmlUtils.escape(question.getContent()) %></dd>
        <dt>Trạng thái</dt><dd><span class="mod-badge <%= badgeClass %>"><%= HtmlUtils.escape(question.getStatus()) %></span></dd>
        <dt>Số câu trả lời</dt><dd><%= question.getAnswerCount() %></dd>
        <dt>Thời gian tạo</dt><dd><%= question.getCreatedAt() != null ? question.getCreatedAt().toString() : "-" %></dd>
        <dt>Cập nhật gần nhất</dt><dd><%= question.getUpdatedAt() != null ? question.getUpdatedAt().toString() : "-" %></dd>
    </dl>
</div>

<!-- Author & Game context -->
<div class="mod-detail-card">
    <h2>Tác giả &amp; Game</h2>
    <dl>
        <dt>Tác giả</dt><dd><%= HtmlUtils.escape(question.getAuthorUsername()) %></dd>
        <dt>Game</dt><dd><%= HtmlUtils.escape(question.getGameName()) %></dd>
    </dl>
</div>

<!-- Moderation Actions -->
<div class="mod-detail-card">
    <h2>Moderation</h2>
    <% if (question.isDeleted()) { %>
        <p style="color: var(--text-secondary);">Câu hỏi này đã bị xóa (soft-delete) và không thể thay đổi trạng thái qua Moderation.</p>
    <% } else { %>
        <p style="color: var(--text-secondary); margin-bottom: 12px;">Chuyển trạng thái câu hỏi khi cần kiểm duyệt nội dung.</p>
        <div class="mod-form-actions">
            <% if (!QuestionStatus.ACTIVE.equals(question.getStatus())) { %>
            <form action="${pageContext.request.contextPath}/moderator/questions/status" method="post">
                <input type="hidden" name="id" value="<%= question.getQuestionId() %>">
                <input type="hidden" name="action" value="activate">
                <button type="submit" class="mod-btn">Kích hoạt (ACTIVE)</button>
            </form>
            <% } %>
            <% if (!QuestionStatus.HIDDEN.equals(question.getStatus())) { %>
            <form action="${pageContext.request.contextPath}/moderator/questions/status" method="post">
                <input type="hidden" name="id" value="<%= question.getQuestionId() %>">
                <input type="hidden" name="action" value="hide">
                <button type="submit" class="mod-btn-secondary">Ẩn (HIDDEN)</button>
            </form>
            <% } %>
            <% if (!QuestionStatus.LOCKED.equals(question.getStatus())) { %>
            <form action="${pageContext.request.contextPath}/moderator/questions/status" method="post">
                <input type="hidden" name="id" value="<%= question.getQuestionId() %>">
                <input type="hidden" name="action" value="lock">
                <button type="submit" class="mod-btn-danger">Khóa (LOCKED)</button>
            </form>
            <% } %>
        </div>
    <% } %>
</div>
<% } %>

<div class="mod-back-link">
    <a href="${pageContext.request.contextPath}/moderator/questions">&larr; Quay lại danh sách</a>
</div>

<%@ include file="/WEB-INF/moderator/layout/footer.jsp" %>
