<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="com.gamenest.model.Answer" %>
<%@ page import="com.gamenest.model.AnswerStatus" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<% request.setAttribute("pageTitle", "Answers"); %>
<%@ include file="/WEB-INF/moderator/layout/header.jsp" %>

<div class="mod-page-header">
    <h1>Answers</h1>
</div>

<% if (request.getAttribute("error") != null) { %>
    <div class="alert alert-error" style="margin-bottom: 16px;">
        <span class="material-symbols-outlined alert-icon">error</span>
        <span><%= HtmlUtils.escape((String) request.getAttribute("error")) %></span>
    </div>
<% } %>

<%
    String fStatus = (String) request.getAttribute("status");
    String fQuestionId = (String) request.getAttribute("questionId");
    String fGameId = (String) request.getAttribute("gameId");
    String fAuthorUsername = (String) request.getAttribute("authorUsername");
    String fQuery = (String) request.getAttribute("q");
    if (fStatus == null) fStatus = "";
    if (fQuestionId == null) fQuestionId = "";
    if (fGameId == null) fGameId = "";
    if (fAuthorUsername == null) fAuthorUsername = "";
    if (fQuery == null) fQuery = "";
%>
<form class="mod-filter-bar" action="${pageContext.request.contextPath}/moderator/answers" method="get">
    <div class="mod-filter-field">
        <label for="status">Trạng thái</label>
        <select id="status" name="status">
            <option value="">Tất cả</option>
            <option value="<%= AnswerStatus.ACTIVE %>" <%= AnswerStatus.ACTIVE.equals(fStatus) ? "selected" : "" %>>ACTIVE</option>
            <option value="<%= AnswerStatus.HIDDEN %>" <%= AnswerStatus.HIDDEN.equals(fStatus) ? "selected" : "" %>>HIDDEN</option>
            <option value="<%= AnswerStatus.DELETED %>" <%= AnswerStatus.DELETED.equals(fStatus) ? "selected" : "" %>>DELETED</option>
        </select>
    </div>
    <div class="mod-filter-field">
        <label for="questionId">Question ID</label>
        <input type="number" id="questionId" name="questionId" value="<%= HtmlUtils.escape(fQuestionId) %>" placeholder="Mã câu hỏi" min="1">
    </div>
    <div class="mod-filter-field">
        <label for="gameId">Game ID</label>
        <input type="number" id="gameId" name="gameId" value="<%= HtmlUtils.escape(fGameId) %>" placeholder="Mã game" min="1">
    </div>
    <div class="mod-filter-field">
        <label for="authorUsername">Tác giả</label>
        <input type="text" id="authorUsername" name="authorUsername" value="<%= HtmlUtils.escape(fAuthorUsername) %>" placeholder="Tên tài khoản">
    </div>
    <div class="mod-filter-field">
        <label for="q">Tìm kiếm</label>
        <input type="text" id="q" name="q" value="<%= HtmlUtils.escape(fQuery) %>" placeholder="Nội dung câu trả lời">
    </div>
    <div class="mod-filter-actions">
        <button type="submit" class="mod-btn">
            <span class="material-symbols-outlined" style="font-size: 18px;">filter_alt</span>
            <span>Lọc</span>
        </button>
        <a class="mod-btn-secondary" href="${pageContext.request.contextPath}/moderator/answers">
            <span class="material-symbols-outlined" style="font-size: 18px;">restart_alt</span>
            <span>Xóa lọc</span>
        </a>
    </div>
</form>

<%
    @SuppressWarnings("unchecked")
    List<Answer> answers = (List<Answer>) request.getAttribute("answers");
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
%>
<div style="overflow-x: auto;">
<table class="mod-table">
    <thead>
        <tr>
            <th>ID</th>
            <th>Nội dung</th>
            <th>Tác giả</th>
            <th>Câu hỏi</th>
            <th>Game</th>
            <th>Trạng thái</th>
            <th>Thời gian tạo</th>
            <th>Thao tác</th>
        </tr>
    </thead>
    <tbody>
    <% if (answers == null || answers.isEmpty()) { %>
        <tr><td colspan="8">Không có câu trả lời nào phù hợp.</td></tr>
    <% } else {
        for (Answer ans : answers) {
            String badgeClass = "mod-badge-muted";
            if (AnswerStatus.ACTIVE.equals(ans.getStatus())) badgeClass = "mod-badge-success";
            else if (AnswerStatus.HIDDEN.equals(ans.getStatus())) badgeClass = "mod-badge-warning";
            else if (AnswerStatus.DELETED.equals(ans.getStatus())) badgeClass = "mod-badge-danger";

            String contentPreview = ans.getContent() != null && ans.getContent().length() > 80
                    ? ans.getContent().substring(0, 80) + "..." : ans.getContent();
    %>
        <tr>
            <td>#<%= ans.getAnswerId() %></td>
            <td class="mod-cell-truncate"><%= HtmlUtils.escape(contentPreview) %></td>
            <td><%= HtmlUtils.escape(ans.getAuthorUsername()) %></td>
            <td class="mod-cell-truncate">#<%= ans.getQuestionId() %> <%= HtmlUtils.escape(ans.getQuestionTitle()) %></td>
            <td><%= HtmlUtils.escape(ans.getGameName()) %></td>
            <td><span class="mod-badge <%= badgeClass %>"><%= HtmlUtils.escape(ans.getStatus()) %></span></td>
            <td><%= ans.getCreatedAt() != null ? ans.getCreatedAt().format(fmt) : "-" %></td>
            <td class="mod-actions">
                <a href="${pageContext.request.contextPath}/moderator/answers/detail?id=<%= ans.getAnswerId() %>">Xem</a>
            </td>
        </tr>
    <% } } %>
    </tbody>
</table>
</div>

<%
    int currentPage = request.getAttribute("page") == null ? 1 : (int) request.getAttribute("page");
    int totalPages = request.getAttribute("totalPages") == null ? 1 : (int) request.getAttribute("totalPages");

    StringBuilder qs = new StringBuilder();
    if (!fStatus.isEmpty()) qs.append("&status=").append(URLEncoder.encode(fStatus, StandardCharsets.UTF_8));
    if (!fQuestionId.isEmpty()) qs.append("&questionId=").append(URLEncoder.encode(fQuestionId, StandardCharsets.UTF_8));
    if (!fGameId.isEmpty()) qs.append("&gameId=").append(URLEncoder.encode(fGameId, StandardCharsets.UTF_8));
    if (!fAuthorUsername.isEmpty()) qs.append("&authorUsername=").append(URLEncoder.encode(fAuthorUsername, StandardCharsets.UTF_8));
    if (!fQuery.isEmpty()) qs.append("&q=").append(URLEncoder.encode(fQuery, StandardCharsets.UTF_8));
    String qParam = qs.toString();
%>
<% if (totalPages > 1) { %>
    <div class="mod-pagination">
        <% for (int p = 1; p <= totalPages; p++) { %>
            <a class="<%= p == currentPage ? "current" : "" %>"
               href="${pageContext.request.contextPath}/moderator/answers?page=<%= p %><%= qParam %>"><%= p %></a>
        <% } %>
    </div>
<% } %>

<%@ include file="/WEB-INF/moderator/layout/footer.jsp" %>
