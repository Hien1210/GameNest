<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= "edit".equals(request.getAttribute("mode")) ? "Sửa câu hỏi" : "Đặt câu hỏi" %> - GameNest</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        body { display: block; padding: 30px 20px; }
        .page-wrap { max-width: 700px; margin: 0 auto; }
        .form-group { margin-bottom: 16px; }
        label { display: block; margin-bottom: 6px; color: var(--text-secondary); font-size: 0.9rem; }
        input, textarea { width: 100%; padding: 10px 12px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-secondary); color: var(--text-primary); }
        textarea { min-height: 180px; resize: vertical; }
        .btn-submit { padding: 10px 18px; border-radius: 8px; border: none; background: var(--accent-purple); color: #fff; cursor: pointer; }
    </style>
</head>
<body>
<div class="page-wrap">
    <%
        boolean isEdit = "edit".equals(request.getAttribute("mode"));
        Object gameIdAttr = request.getAttribute("gameId");
        String actionUrl = isEdit
            ? request.getContextPath() + "/questions/edit?id=" + request.getAttribute("questionId")
            : request.getContextPath() + "/questions/new";
    %>
    <h1><%= isEdit ? "Sửa câu hỏi" : "Đặt câu hỏi" %></h1>

    <% if (request.getAttribute("error") != null) { %>
        <p style="color: var(--error-color);"><%= com.gamenest.util.HtmlUtils.escape((String) request.getAttribute("error")) %></p>
    <% } %>

    <form action="<%= actionUrl %>" method="post">
        <% if (!isEdit) { %>
            <input type="hidden" name="gameId" value="<%= gameIdAttr %>">
        <% } %>

        <div class="form-group">
            <label for="title">Tiêu đề</label>
            <input type="text" id="title" name="title"
                   value="<%= com.gamenest.util.HtmlUtils.escape((String) request.getAttribute("title")) %>"
                   required maxlength="250">
        </div>

        <div class="form-group">
            <label for="content">Nội dung</label>
            <textarea id="content" name="content" required><%= com.gamenest.util.HtmlUtils.escape((String) request.getAttribute("content")) %></textarea>
        </div>

        <button type="submit" class="btn-submit"><%= isEdit ? "Lưu thay đổi" : "Đăng câu hỏi" %></button>
    </form>
</div>
</body>
</html>
