<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="com.gamenest.model.Question" %>
<%@ page import="com.gamenest.model.QuestionStatus" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<% request.setAttribute("pageTitle", "Questions"); %>
<%@ include file="/WEB-INF/moderator/layout/header.jsp" %>

<div class="mod-page-header">
    <h1>Questions</h1>
</div>

<% if (request.getAttribute("error") != null) { %>
    <div class="alert alert-error" style="margin-bottom: 16px;">
        <span class="material-symbols-outlined alert-icon">error</span>
        <span><%= HtmlUtils.escape((String) request.getAttribute("error")) %></span>
    </div>
<% } %>

<%
    String fStatus = (String) request.getAttribute("status");
    String fGameId = (String) request.getAttribute("gameId");
    String fAuthorUsername = (String) request.getAttribute("authorUsername");
    String fQuery = (String) request.getAttribute("q");
    if (fStatus == null) fStatus = "";
    if (fGameId == null) fGameId = "";
    if (fAuthorUsername == null) fAuthorUsername = "";
    if (fQuery == null) fQuery = "";
%>
<form class="mod-filter-bar" action="${pageContext.request.contextPath}/moderator/questions" method="get">
    <div class="mod-filter-field">
        <label for="status">Trạng thái</label>
        <select id="status" name="status">
            <option value="">Tất cả</option>
            <option value="<%= QuestionStatus.ACTIVE %>" <%= QuestionStatus.ACTIVE.equals(fStatus) ? "selected" : "" %>>ACTIVE</option>
            <option value="<%= QuestionStatus.HIDDEN %>" <%= QuestionStatus.HIDDEN.equals(fStatus) ? "selected" : "" %>>HIDDEN</option>
            <option value="<%= QuestionStatus.LOCKED %>" <%= QuestionStatus.LOCKED.equals(fStatus) ? "selected" : "" %>>LOCKED</option>
            <option value="<%= QuestionStatus.DELETED %>" <%= QuestionStatus.DELETED.equals(fStatus) ? "selected" : "" %>>DELETED</option>
        </select>
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
        <input type="text" id="q" name="q" value="<%= HtmlUtils.escape(fQuery) %>" placeholder="Tiêu đề hoặc nội dung">
    </div>
    <div class="mod-filter-actions">
        <button type="submit" class="mod-btn">
            <span class="material-symbols-outlined" style="font-size: 18px;">filter_alt</span>
            <span>Lọc</span>
        </button>
        <a class="mod-btn-secondary" href="${pageContext.request.contextPath}/moderator/questions">
            <span class="material-symbols-outlined" style="font-size: 18px;">restart_alt</span>
            <span>Xóa lọc</span>
        </a>
    </div>
</form>

<%
    @SuppressWarnings("unchecked")
    List<Question> questions = (List<Question>) request.getAttribute("questions");
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
%>
<div style="overflow-x: auto;">
<table class="mod-table">
    <thead>
        <tr>
            <th>ID</th>
            <th>Tiêu đề</th>
            <th>Tác giả</th>
            <th>Game</th>
            <th>Trạng thái</th>
            <th>Thời gian tạo</th>
            <th>Thao tác</th>
        </tr>
    </thead>
    <tbody>
    <% if (questions == null || questions.isEmpty()) { %>
        <tr><td colspan="7">Không có câu hỏi nào phù hợp.</td></tr>
    <% } else {
        for (Question q : questions) {
            String badgeClass = "mod-badge-muted";
            if (QuestionStatus.ACTIVE.equals(q.getStatus())) badgeClass = "mod-badge-success";
            else if (QuestionStatus.HIDDEN.equals(q.getStatus())) badgeClass = "mod-badge-warning";
            else if (QuestionStatus.DELETED.equals(q.getStatus())) badgeClass = "mod-badge-danger";
    %>
        <tr>
            <td>#<%= q.getQuestionId() %></td>
            <td class="mod-cell-truncate"><%= HtmlUtils.escape(q.getTitle()) %></td>
            <td><%= HtmlUtils.escape(q.getAuthorUsername()) %></td>
            <td><%= HtmlUtils.escape(q.getGameName()) %></td>
            <td><span class="mod-badge <%= badgeClass %>"><%= HtmlUtils.escape(q.getStatus()) %></span></td>
            <td><%= q.getCreatedAt() != null ? q.getCreatedAt().format(fmt) : "-" %></td>
            <td class="mod-actions">
                <a href="${pageContext.request.contextPath}/moderator/questions/detail?id=<%= q.getQuestionId() %>">Xem</a>
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
    if (!fGameId.isEmpty()) qs.append("&gameId=").append(URLEncoder.encode(fGameId, StandardCharsets.UTF_8));
    if (!fAuthorUsername.isEmpty()) qs.append("&authorUsername=").append(URLEncoder.encode(fAuthorUsername, StandardCharsets.UTF_8));
    if (!fQuery.isEmpty()) qs.append("&q=").append(URLEncoder.encode(fQuery, StandardCharsets.UTF_8));
    String qParam = qs.toString();
%>
<% if (totalPages > 1) { %>
    <div class="mod-pagination">
        <% for (int p = 1; p <= totalPages; p++) { %>
            <a class="<%= p == currentPage ? "current" : "" %>"
               href="${pageContext.request.contextPath}/moderator/questions?page=<%= p %><%= qParam %>"><%= p %></a>
        <% } %>
    </div>
<% } %>

<%@ include file="/WEB-INF/moderator/layout/footer.jsp" %>
