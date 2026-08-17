<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tạo nhóm - GameNest</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        body { display: block; padding: 30px 20px; }
        .page-wrap { max-width: 560px; margin: 0 auto; }
        .form-card { background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 12px; padding: 26px; }
        .form-group { margin-bottom: 18px; }
        .form-group label { display: block; margin-bottom: 6px; font-size: 0.88rem; color: var(--text-secondary); }
        .form-group input, .form-group textarea {
            width: 100%; padding: 10px 12px; border-radius: 8px; border: 1px solid var(--border-color);
            background: var(--bg-primary); color: var(--text-primary); font-family: inherit; font-size: 0.92rem;
        }
        .form-group textarea { min-height: 100px; resize: vertical; }
        .btn-submit { padding: 11px 22px; border-radius: 8px; border: none; background: var(--accent-purple); color: #fff; font-weight: 600; cursor: pointer; }
        .error-msg { color: var(--error-color); margin-bottom: 16px; }
    </style>
</head>
<body>
<div class="page-wrap">
    <h1>Tạo nhóm mới</h1>
    <p style="color: var(--text-secondary); margin-bottom: 20px;">Nhóm là không gian riêng để chơi cùng bạn bè — bạn sẽ trở thành chủ nhóm và có thể mời bạn bè tham gia.</p>

    <% if (request.getAttribute("error") != null) { %>
        <p class="error-msg"><%= HtmlUtils.escape((String) request.getAttribute("error")) %></p>
    <% } %>

    <%
        String fName = (String) request.getAttribute("name");
        String fDescription = (String) request.getAttribute("description");
    %>
    <div class="form-card">
        <form method="post" action="${pageContext.request.contextPath}/account/team/create">
            <div class="form-group">
                <label for="name">Tên nhóm</label>
                <input type="text" name="name" id="name" maxlength="150" required value="<%= HtmlUtils.escape(fName == null ? "" : fName) %>">
            </div>
            <div class="form-group">
                <label for="description">Mô tả</label>
                <textarea name="description" id="description" maxlength="5000"><%= HtmlUtils.escape(fDescription == null ? "" : fDescription) %></textarea>
            </div>
            <button type="submit" class="btn-submit">Tạo nhóm</button>
        </form>
    </div>

    <p style="margin-top: 20px;"><a href="${pageContext.request.contextPath}/account/teams" style="color: var(--text-secondary);">&larr; Quay lại</a></p>
</div>
</body>
</html>
