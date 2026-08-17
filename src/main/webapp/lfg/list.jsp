<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page import="com.gamenest.model.Game" %>
<%@ page import="com.gamenest.model.LFGPost" %>
<%@ page import="com.gamenest.model.LFGStatus" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tìm người chơi cùng - GameNest</title>
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined" rel="stylesheet" />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        body { display: block; padding: 30px 20px; }
        .page-wrap { max-width: 1100px; margin: 0 auto; }
        .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 10px; }
        .btn-create { padding: 10px 18px; border-radius: 8px; border: none; background: var(--accent-purple); color: #fff; cursor: pointer; text-decoration: none; font-size: 0.9rem; display: inline-flex; align-items: center; gap: 6px; }
        .filter-bar { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 20px; padding: 14px; background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 10px; }
        .filter-bar input, .filter-bar select { padding: 9px 12px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-primary); color: var(--text-primary); font-family: inherit; }
        .filter-bar button { padding: 9px 16px; border-radius: 8px; border: none; background: var(--accent-cyan); color: #06222a; font-weight: 600; cursor: pointer; }
        .lfg-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
        .lfg-card { background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 12px; padding: 16px 18px; text-decoration: none; color: var(--text-primary); display: block; }
        .lfg-card:hover { border-color: var(--border-hover); }
        .lfg-card-top { display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; margin-bottom: 8px; }
        .lfg-title { font-size: 1.02rem; font-weight: 600; }
        .lfg-game { font-size: 0.82rem; color: var(--accent-cyan); margin-bottom: 8px; }
        .lfg-desc { font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 12px; line-height: 1.4; }
        .lfg-meta { display: flex; justify-content: space-between; align-items: center; font-size: 0.8rem; color: var(--text-secondary); }
        .lfg-players { font-weight: 600; color: var(--text-primary); }
        .status-badge { display: inline-block; padding: 2px 9px; border-radius: 999px; font-size: 0.72rem; font-weight: 600; }
        .status-open { background: rgba(16,185,129,0.15); color: var(--success-color); }
        .status-full { background: rgba(245,158,11,0.15); color: #f59e0b; }
        .status-closed, .status-expired { background: rgba(255,255,255,0.06); color: var(--text-secondary); }
        .pagination { margin-top: 24px; display: flex; gap: 8px; }
        .pagination a { padding: 8px 12px; border-radius: 6px; border: 1px solid var(--border-color); color: var(--text-primary); text-decoration: none; }
        .empty { color: var(--text-secondary); margin-top: 20px; }
    </style>
</head>
<body>
<div class="page-wrap">
    <div class="page-header">
        <h1>Tìm người chơi cùng</h1>
        <a class="btn-create" href="${pageContext.request.contextPath}/lfg/new">
            <span class="material-symbols-outlined" style="font-size: 18px;">add</span>
            <span>Tạo nhóm mới</span>
        </a>
    </div>

    <% if (request.getAttribute("error") != null) { %>
        <p style="color: var(--error-color);"><%= HtmlUtils.escape((String) request.getAttribute("error")) %></p>
    <% } %>
    <% if (request.getParameter("deleted") != null) { %>
        <p style="color: var(--success-color);">Đã xóa nhóm.</p>
    <% } %>

    <%
        String fGameId = (String) request.getAttribute("gameId");
        String fStatus = (String) request.getAttribute("status");
        String fQuery = (String) request.getAttribute("q");
        if (fGameId == null) fGameId = "";
        if (fStatus == null) fStatus = "";
        if (fQuery == null) fQuery = "";

        @SuppressWarnings("unchecked")
        List<Game> allActiveGames = (List<Game>) request.getAttribute("allActiveGames");
    %>
    <form class="filter-bar" action="${pageContext.request.contextPath}/lfg" method="get">
        <select name="gameId">
            <option value="">Tất cả game</option>
            <% if (allActiveGames != null) { for (Game g : allActiveGames) { %>
            <option value="<%= g.getGameId() %>" <%= String.valueOf(g.getGameId()).equals(fGameId) ? "selected" : "" %>><%= HtmlUtils.escape(g.getName()) %></option>
            <% } } %>
        </select>
        <select name="status">
            <option value="">OPEN / FULL</option>
            <option value="<%= LFGStatus.OPEN %>" <%= LFGStatus.OPEN.equals(fStatus) ? "selected" : "" %>>OPEN</option>
            <option value="<%= LFGStatus.FULL %>" <%= LFGStatus.FULL.equals(fStatus) ? "selected" : "" %>>FULL</option>
            <option value="<%= LFGStatus.CLOSED %>" <%= LFGStatus.CLOSED.equals(fStatus) ? "selected" : "" %>>CLOSED</option>
            <option value="<%= LFGStatus.EXPIRED %>" <%= LFGStatus.EXPIRED.equals(fStatus) ? "selected" : "" %>>EXPIRED</option>
        </select>
        <input type="text" name="q" value="<%= HtmlUtils.escape(fQuery) %>" placeholder="Tìm theo tiêu đề/mô tả...">
        <button type="submit">Lọc</button>
    </form>

    <%
        @SuppressWarnings("unchecked")
        List<LFGPost> posts = (List<LFGPost>) request.getAttribute("posts");
    %>
    <% if (posts == null || posts.isEmpty()) { %>
        <p class="empty">Không tìm thấy nhóm nào phù hợp.</p>
    <% } else { %>
        <div class="lfg-grid">
        <% for (LFGPost p : posts) {
            String badgeClass = "status-closed";
            if (LFGStatus.OPEN.equals(p.getStatus())) badgeClass = "status-open";
            else if (LFGStatus.FULL.equals(p.getStatus())) badgeClass = "status-full";
            else if (LFGStatus.EXPIRED.equals(p.getStatus())) badgeClass = "status-expired";
            String descPreview = p.getDescription() != null && p.getDescription().length() > 100
                    ? p.getDescription().substring(0, 100) + "..." : p.getDescription();
        %>
            <a class="lfg-card" href="${pageContext.request.contextPath}/lfg/detail?id=<%= p.getLfgId() %>">
                <div class="lfg-card-top">
                    <span class="lfg-title"><%= HtmlUtils.escape(p.getTitle()) %></span>
                    <span class="status-badge <%= badgeClass %>"><%= HtmlUtils.escape(p.getStatus()) %></span>
                </div>
                <div class="lfg-game"><%= HtmlUtils.escape(p.getGameName()) %></div>
                <% if (descPreview != null) { %>
                <div class="lfg-desc"><%= HtmlUtils.escape(descPreview) %></div>
                <% } %>
                <div class="lfg-meta">
                    <span>bởi <%= HtmlUtils.escape(p.getCreatorUsername()) %></span>
                    <span class="lfg-players"><%= p.getCurrentPlayers() %>/<%= p.getMaxPlayers() %></span>
                </div>
            </a>
        <% } %>
        </div>
    <% } %>

    <%
        int currentPage = request.getAttribute("page") == null ? 1 : (int) request.getAttribute("page");
        int totalPages = request.getAttribute("totalPages") == null ? 1 : (int) request.getAttribute("totalPages");

        StringBuilder qs = new StringBuilder();
        if (!fGameId.isEmpty()) qs.append("&gameId=").append(URLEncoder.encode(fGameId, StandardCharsets.UTF_8));
        if (!fStatus.isEmpty()) qs.append("&status=").append(URLEncoder.encode(fStatus, StandardCharsets.UTF_8));
        if (!fQuery.isEmpty()) qs.append("&q=").append(URLEncoder.encode(fQuery, StandardCharsets.UTF_8));
        String qParam = qs.toString();
    %>
    <% if (totalPages > 1) { %>
        <div class="pagination">
            <% for (int p = 1; p <= totalPages; p++) { %>
                <a href="${pageContext.request.contextPath}/lfg?page=<%= p %><%= qParam %>"
                   style="<%= p == currentPage ? "border-color: var(--accent-purple);" : "" %>"><%= p %></a>
            <% } %>
        </div>
    <% } %>

    <p style="margin-top: 30px;"><a href="${pageContext.request.contextPath}/" style="color: var(--text-secondary);">&larr; Về trang chủ</a></p>
</div>
</body>
</html>
