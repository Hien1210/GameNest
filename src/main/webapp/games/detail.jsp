<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.gamenest.model.Game" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${game.name} - GameNest</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        body { display: block; padding: 30px 20px; }
        .page-wrap { max-width: 900px; margin: 0 auto; }
        .cover { width: 100%; max-height: 320px; object-fit: cover; border-radius: 12px; margin-bottom: 20px; background: #222; }
        .status-badge { display: inline-block; padding: 3px 10px; border-radius: 6px; font-size: 0.8rem; background: var(--accent-purple); color: #fff; }
        .section-placeholders { display: flex; gap: 14px; margin-top: 28px; flex-wrap: wrap; }
        .section-placeholders div { flex: 1; min-width: 180px; padding: 16px; background: var(--bg-secondary); border: 1px dashed var(--border-color); border-radius: 10px; color: var(--text-secondary); text-align: center; }
        .section-placeholders a { text-decoration: none; color: var(--text-primary); border-style: solid; display: block; }
    </style>
</head>
<body>
<div class="page-wrap">
<%
    Game game = (Game) request.getAttribute("game");
%>
    <p><a href="${pageContext.request.contextPath}/games" style="color: var(--text-secondary);">&larr; Danh sách Games</a></p>

    <% if (game.getCoverImageUrl() != null) { %>
        <img class="cover" src="<%= game.getCoverImageUrl() %>" alt="<%= game.getName() %>">
    <% } %>

    <h1><%= game.getName() %></h1>
    <p class="status-badge"><%= game.getStatus() %></p>

    <% if (game.getDescription() != null) { %>
        <p style="margin-top: 16px; white-space: pre-line;"><%= game.getDescription() %></p>
    <% } %>

    <% if (game.getReleaseDate() != null) { %>
        <p style="margin-top: 10px; color: var(--text-secondary);">Ngày phát hành: <%= game.getReleaseDate() %></p>
    <% } %>

    <!-- Chỗ mở rộng cho các module sau: Questions (đã triển khai), Find Players (LFG), Teams -->
    <div class="section-placeholders">
        <a href="${pageContext.request.contextPath}/questions?gameId=<%= game.getGameId() %>">Questions</a>
        <div>Find Players<br><small>(sắp có)</small></div>
        <div>Teams<br><small>(sắp có)</small></div>
    </div>
</div>
</body>
</html>
