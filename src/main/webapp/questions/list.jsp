<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.gamenest.model.Question" %>
<%@ page import="com.gamenest.model.Game" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Questions - <%= HtmlUtils.escape(((Game) request.getAttribute("game")).getName()) %> - GameNest</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        body { display: block; padding: 30px 20px; }
        .page-wrap { max-width: 900px; margin: 0 auto; }
        .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 10px; }
        .search-form input { padding: 10px 14px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-secondary); color: var(--text-primary); }
        .search-form button, .btn { padding: 10px 16px; border-radius: 8px; border: none; background: var(--accent-purple); color: #fff; cursor: pointer; text-decoration: none; display: inline-block; }
        .question-item { background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 10px; padding: 14px 16px; margin-bottom: 12px; text-decoration: none; display: block; color: var(--text-primary); }
        .question-item h3 { font-size: 1rem; margin-bottom: 6px; }
        .question-meta { font-size: 0.82rem; color: var(--text-secondary); }
        .pagination { margin-top: 20px; display: flex; gap: 8px; }
        .pagination a { padding: 8px 12px; border-radius: 6px; border: 1px solid var(--border-color); color: var(--text-primary); text-decoration: none; }
        .empty { color: var(--text-secondary); margin-top: 20px; }
    </style>
</head>
<body>
<%
    Game game = (Game) request.getAttribute("game");
%>
<div class="page-wrap">
    <p><a href="${pageContext.request.contextPath}/games/detail?id=<%= game.getGameId() %>" style="color: var(--text-secondary);">&larr; <%= HtmlUtils.escape(game.getName()) %></a></p>

    <div class="page-header">
        <h1>Questions</h1>
        <a class="btn" href="${pageContext.request.contextPath}/questions/new?gameId=<%= game.getGameId() %>">+ Đặt câu hỏi</a>
    </div>

    <form class="search-form" action="${pageContext.request.contextPath}/questions" method="get">
        <input type="hidden" name="gameId" value="<%= game.getGameId() %>">
        <input type="text" name="q" value="${query}" placeholder="Tìm câu hỏi theo tiêu đề...">
        <button type="submit">Tìm kiếm</button>
    </form>

    <% if (request.getAttribute("error") != null) { %>
        <p style="color: var(--error-color); margin-top: 14px;"><%= HtmlUtils.escape((String) request.getAttribute("error")) %></p>
    <% } %>

    <%
        @SuppressWarnings("unchecked")
        List<Question> questions = (List<Question>) request.getAttribute("questions");
    %>
    <div style="margin-top: 18px;">
    <% if (questions == null || questions.isEmpty()) { %>
        <p class="empty">Chưa có câu hỏi nào cho game này.</p>
    <% } else {
        for (Question q : questions) { %>
        <a class="question-item" href="${pageContext.request.contextPath}/questions/detail?id=<%= q.getQuestionId() %>">
            <h3><%= HtmlUtils.escape(q.getTitle()) %></h3>
            <div class="question-meta">
                bởi <%= HtmlUtils.escape(q.getAuthorUsername()) %> · <%= q.getCreatedAt() %> · <%= q.getAnswerCount() %> câu trả lời
            </div>
        </a>
    <% } } %>
    </div>

    <%
        int currentPage = request.getAttribute("page") == null ? 1 : (int) request.getAttribute("page");
        int totalPages = request.getAttribute("totalPages") == null ? 1 : (int) request.getAttribute("totalPages");
        String qParam = request.getAttribute("query") == null ? "" : "&q=" + java.net.URLEncoder.encode((String) request.getAttribute("query"), java.nio.charset.StandardCharsets.UTF_8);
    %>
    <% if (totalPages > 1) { %>
        <div class="pagination">
            <% for (int p = 1; p <= totalPages; p++) { %>
                <a href="${pageContext.request.contextPath}/questions?gameId=<%= game.getGameId() %>&page=<%= p %><%= qParam %>"
                   style="<%= p == currentPage ? "border-color: var(--accent-purple);" : "" %>"><%= p %></a>
            <% } %>
        </div>
    <% } %>
</div>
</body>
</html>
