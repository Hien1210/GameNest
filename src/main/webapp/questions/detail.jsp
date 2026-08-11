<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.gamenest.model.Question" %>
<%@ page import="com.gamenest.model.Answer" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <%
        Question question = (Question) request.getAttribute("question");
    %>
    <title><%= HtmlUtils.escape(question.getTitle()) %> - GameNest</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        body { display: block; padding: 30px 20px; }
        .page-wrap { max-width: 850px; margin: 0 auto; }
        .q-title { margin-bottom: 6px; }
        .q-meta { font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 16px; }
        .q-content, .a-content { white-space: pre-line; line-height: 1.5; }
        .status-badge { display: inline-block; padding: 2px 8px; border-radius: 6px; font-size: 0.75rem; background: var(--accent-purple); color: #fff; margin-left: 8px; }
        .actions-row { margin-top: 14px; display: flex; gap: 8px; }
        .actions-row a, .actions-row button { padding: 8px 14px; border-radius: 8px; border: 1px solid var(--border-color); background: transparent; color: var(--text-primary); cursor: pointer; text-decoration: none; font-size: 0.85rem; }
        .btn-primary { background: var(--accent-purple); color: #fff; border: none; }
        .answer-card { background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 10px; padding: 14px 16px; margin-top: 14px; }
        .answer-card.accepted { border-color: var(--success-color); }
        .answer-meta { font-size: 0.8rem; color: var(--text-secondary); margin-top: 8px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
        .accepted-tag { color: var(--success-color); font-weight: 600; font-size: 0.8rem; }
        textarea { width: 100%; min-height: 100px; padding: 10px 12px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-secondary); color: var(--text-primary); resize: vertical; }
        .btn-submit { margin-top: 10px; padding: 10px 18px; border-radius: 8px; border: none; background: var(--accent-purple); color: #fff; cursor: pointer; }
    </style>
</head>
<body>
<div class="page-wrap">
    <p><a href="${pageContext.request.contextPath}/questions?gameId=<%= question.getGameId() %>" style="color: var(--text-secondary);">&larr; <%= HtmlUtils.escape(question.getGameName()) %> - Questions</a></p>

    <% if (request.getAttribute("error") != null) { %>
        <p style="color: var(--error-color);"><%= HtmlUtils.escape((String) request.getAttribute("error")) %></p>
    <% } %>

    <h1 class="q-title"><%= HtmlUtils.escape(question.getTitle()) %>
        <% if (!"ACTIVE".equals(question.getStatus())) { %>
            <span class="status-badge"><%= question.getStatus() %></span>
        <% } %>
    </h1>
    <div class="q-meta">bởi <%= HtmlUtils.escape(question.getAuthorUsername()) %> · <%= question.getCreatedAt() %>
        <% if (question.getUpdatedAt() != null) { %> · đã sửa <%= question.getUpdatedAt() %><% } %>
    </div>

    <div class="q-content"><%= HtmlUtils.escape(question.getContent()) %></div>

    <%
        boolean isOwner = Boolean.TRUE.equals(request.getAttribute("isOwner"));
        boolean isAdmin = Boolean.TRUE.equals(request.getAttribute("isAdmin"));
        boolean isLoggedIn = Boolean.TRUE.equals(request.getAttribute("isLoggedIn"));
        boolean questionActive = "ACTIVE".equals(question.getStatus());
    %>
    <% if (isOwner || isAdmin) { %>
        <div class="actions-row">
            <% if (isOwner) { %>
                <a href="${pageContext.request.contextPath}/questions/edit?id=<%= question.getQuestionId() %>">Sửa</a>
            <% } %>
            <form action="${pageContext.request.contextPath}/questions/delete" method="post" onsubmit="return confirm('Xóa câu hỏi này?');">
                <input type="hidden" name="id" value="<%= question.getQuestionId() %>">
                <input type="hidden" name="gameId" value="<%= question.getGameId() %>">
                <button type="submit">Xóa</button>
            </form>
        </div>
    <% } %>

    <h2 style="margin-top: 30px;">Câu trả lời</h2>

    <%
        @SuppressWarnings("unchecked")
        List<Answer> answers = (List<Answer>) request.getAttribute("answers");
    %>
    <% if (answers == null || answers.isEmpty()) { %>
        <p style="color: var(--text-secondary); margin-top: 10px;">Chưa có câu trả lời nào.</p>
    <% } else {
        for (Answer ans : answers) {
            boolean isAnswerOwner = isLoggedIn && request.getSession().getAttribute("accountId") != null
                    && ((int) request.getSession().getAttribute("accountId")) == ans.getAccountId();
    %>
        <div class="answer-card<%= ans.isAccepted() ? " accepted" : "" %>">
            <div class="a-content"><%= HtmlUtils.escape(ans.getContent()) %></div>
            <div class="answer-meta">
                <span>
                    bởi <%= HtmlUtils.escape(ans.getAuthorUsername()) %> · <%= ans.getCreatedAt() %>
                    <% if (ans.isAccepted()) { %> · <span class="accepted-tag">✓ Câu trả lời hay nhất</span><% } %>
                </span>
                <span>
                <% if (isAnswerOwner || isAdmin) { %>
                    <a href="${pageContext.request.contextPath}/answers/edit?id=<%= ans.getAnswerId() %>">Sửa</a>
                    <form style="display:inline;" action="${pageContext.request.contextPath}/answers/delete" method="post" onsubmit="return confirm('Xóa câu trả lời này?');">
                        <input type="hidden" name="id" value="<%= ans.getAnswerId() %>">
                        <input type="hidden" name="questionId" value="<%= question.getQuestionId() %>">
                        <button type="submit">Xóa</button>
                    </form>
                <% }
                if (isOwner) { %>
                    <form style="display:inline;" action="${pageContext.request.contextPath}/answers/accept" method="post">
                        <input type="hidden" name="questionId" value="<%= question.getQuestionId() %>">
                        <input type="hidden" name="answerId" value="<%= ans.getAnswerId() %>">
                        <input type="hidden" name="action" value="<%= ans.isAccepted() ? "unaccept" : "accept" %>">
                        <button type="submit"><%= ans.isAccepted() ? "Bỏ chọn" : "Chọn là hay nhất" %></button>
                    </form>
                <% } %>
                </span>
            </div>
        </div>
    <% } } %>

    <% if (isLoggedIn && questionActive) { %>
        <form action="${pageContext.request.contextPath}/answers/new" method="post" style="margin-top: 20px;">
            <input type="hidden" name="questionId" value="<%= question.getQuestionId() %>">
            <textarea name="content" placeholder="Viết câu trả lời của bạn..." required></textarea>
            <button type="submit" class="btn-submit">Gửi câu trả lời</button>
        </form>
    <% } else if (!isLoggedIn) { %>
        <p style="margin-top: 16px;"><a href="${pageContext.request.contextPath}/account/login.jsp">Đăng nhập</a> để trả lời câu hỏi này.</p>
    <% } else { %>
        <p style="margin-top: 16px; color: var(--text-secondary);">Câu hỏi này không còn nhận câu trả lời mới.</p>
    <% } %>
</div>
</body>
</html>
