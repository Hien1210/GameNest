<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Games - GameNest</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        body { display: block; padding: 30px 20px; }
        .page-wrap { max-width: 1100px; margin: 0 auto; }
        .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 10px; }
        .search-form input { padding: 10px 14px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-secondary); color: var(--text-primary); }
        .search-form button { padding: 10px 16px; border-radius: 8px; border: none; background: var(--accent-purple); color: #fff; cursor: pointer; }
        .game-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 18px; }
        .game-card { background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 12px; overflow: hidden; text-decoration: none; color: var(--text-primary); display: block; }
        .game-card img { width: 100%; height: 130px; object-fit: cover; background: #222; }
        .game-card .info { padding: 12px; }
        .game-card h3 { font-size: 1rem; margin-bottom: 6px; }
        .game-card p { font-size: 0.85rem; color: var(--text-secondary); }
        .pagination { margin-top: 24px; display: flex; gap: 8px; }
        .pagination a { padding: 8px 12px; border-radius: 6px; border: 1px solid var(--border-color); color: var(--text-primary); text-decoration: none; }
        .empty { color: var(--text-secondary); margin-top: 20px; }
    </style>
</head>
<body>
<div class="page-wrap">
    <div class="page-header">
        <h1>Games</h1>
        <form class="search-form" action="${pageContext.request.contextPath}/games" method="get">
            <input type="text" name="q" value="${query}" placeholder="Tìm game theo tên...">
            <button type="submit">Tìm kiếm</button>
        </form>
    </div>

    <% if (request.getAttribute("error") != null) { %>
        <p style="color: var(--error-color);"><%= request.getAttribute("error") %></p>
    <% } %>

    <% @SuppressWarnings("unchecked")
       java.util.List<com.gamenest.model.Game> games =
           (java.util.List<com.gamenest.model.Game>) request.getAttribute("games");
       if (games == null || games.isEmpty()) { %>
        <p class="empty">Không tìm thấy game nào.</p>
    <% } else { %>
        <div class="game-grid">
        <% for (com.gamenest.model.Game g : games) { %>
            <a class="game-card" href="${pageContext.request.contextPath}/games/detail?id=<%= g.getGameId() %>">
                <% if (g.getCoverImageUrl() != null) { %>
                    <img src="<%= g.getCoverImageUrl() %>" alt="<%= g.getName() %>">
                <% } else { %>
                    <img src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='1' height='1'/%3E" alt="">
                <% } %>
                <div class="info">
                    <h3><%= g.getName() %></h3>
                    <% if (g.getDescription() != null) { %>
                        <p><%= g.getDescription().length() > 80 ? g.getDescription().substring(0, 80) + "..." : g.getDescription() %></p>
                    <% } %>
                    <% if (g.getReleaseDate() != null) { %>
                        <p>Phát hành: <%= g.getReleaseDate() %></p>
                    <% } %>
                </div>
            </a>
        <% } %>
        </div>
    <% } %>

    <%
        int currentPage = request.getAttribute("page") == null ? 1 : (int) request.getAttribute("page");
        int totalPages = request.getAttribute("totalPages") == null ? 1 : (int) request.getAttribute("totalPages");
        String qParam = request.getAttribute("query") == null ? "" : "&q=" + java.net.URLEncoder.encode((String) request.getAttribute("query"), java.nio.charset.StandardCharsets.UTF_8);
    %>
    <% if (totalPages > 1) { %>
        <div class="pagination">
            <% for (int p = 1; p <= totalPages; p++) { %>
                <a href="${pageContext.request.contextPath}/games?page=<%= p %><%= qParam %>"
                   style="<%= p == currentPage ? "border-color: var(--accent-purple);" : "" %>"><%= p %></a>
            <% } %>
        </div>
    <% } %>

    <p style="margin-top: 30px;"><a href="${pageContext.request.contextPath}/" style="color: var(--text-secondary);">&larr; Về trang chủ</a></p>
</div>
</body>
</html>
