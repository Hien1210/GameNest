<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sửa câu trả lời - GameNest</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        body { display: block; padding: 30px 20px; }
        .page-wrap { max-width: 700px; margin: 0 auto; }
        textarea { width: 100%; min-height: 160px; padding: 10px 12px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-secondary); color: var(--text-primary); resize: vertical; }
        .btn-submit { margin-top: 12px; padding: 10px 18px; border-radius: 8px; border: none; background: var(--accent-purple); color: #fff; cursor: pointer; }
    </style>
</head>
<body>
<div class="page-wrap">
    <h1>Sửa câu trả lời</h1>

    <% if (request.getAttribute("error") != null) { %>
        <p style="color: var(--error-color);"><%= com.gamenest.util.HtmlUtils.escape((String) request.getAttribute("error")) %></p>
    <% } %>

    <form action="${pageContext.request.contextPath}/answers/edit?id=${answerId}" method="post">
        <input type="hidden" name="id" value="${answerId}">
        <textarea name="content" required><%= com.gamenest.util.HtmlUtils.escape((String) request.getAttribute("content")) %></textarea>
        <br>
        <button type="submit" class="btn-submit">Lưu thay đổi</button>
    </form>

    <p style="margin-top: 16px;"><a href="${pageContext.request.contextPath}/questions/detail?id=${questionId}" style="color: var(--text-secondary);">&larr; Quay lại câu hỏi</a></p>
</div>
</body>
</html>
