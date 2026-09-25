<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%@ page import="com.gamenest.model.Team" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Nhóm của tôi - GameNest</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        body { display: block; padding: 30px 20px; }
        .page-wrap { max-width: 720px; margin: 0 auto; }
        .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 10px; }
        .header-actions { display: flex; gap: 8px; }
        .btn-create { padding: 10px 18px; border-radius: 8px; border: none; background: var(--accent-purple); color: #fff; cursor: pointer; text-decoration: none; font-size: 0.9rem; }
        .tab-link { padding: 10px 18px; border-radius: 8px; border: 1px solid var(--border-color); color: var(--text-secondary); text-decoration: none; font-size: 0.9rem; }
        .team-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 16px; }
        .team-card { background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 12px; padding: 16px 18px; text-decoration: none; color: var(--text-primary); display: block; }
        .team-card:hover { border-color: var(--border-hover); }
        .team-name { font-size: 1.02rem; font-weight: 600; margin-bottom: 6px; }
        .team-owner { font-size: 0.82rem; color: var(--accent-cyan); margin-bottom: 10px; }
        .team-meta { display: flex; justify-content: space-between; font-size: 0.8rem; color: var(--text-secondary); }
        .empty { color: var(--text-secondary); margin-top: 20px; }
        .error-msg { color: var(--error-color); margin-bottom: 16px; }
        .success-msg { color: var(--success-color); margin-bottom: 16px; }
        .pagination { margin-top: 24px; display: flex; gap: 8px; }
        .pagination a { padding: 8px 12px; border-radius: 6px; border: 1px solid var(--border-color); color: var(--text-primary); text-decoration: none; }
    </style>
</head>
<body>
<div class="page-wrap">
    <div class="page-header">
        <h1>Nhóm của tôi</h1>
        <div class="header-actions">
            <a class="tab-link" href="${pageContext.request.contextPath}/account/teams/invitations">Lời mời</a>
            <a class="btn-create" href="${pageContext.request.contextPath}/account/team/create">+ Tạo nhóm</a>
        </div>
    </div>

    <% if (request.getAttribute("error") != null) { %>
        <p class="error-msg"><%= HtmlUtils.escape((String) request.getAttribute("error")) %></p>
    <% } %>
    <% if (request.getParameter("deleted") != null) { %>
        <p class="success-msg">Đã xóa nhóm.</p>
    <% } %>

    <%
        @SuppressWarnings("unchecked")
        List<Team> teams = (List<Team>) request.getAttribute("teams");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    %>
    <% if (teams == null || teams.isEmpty()) { %>
        <p class="empty">Bạn chưa tham gia nhóm nào.</p>
    <% } else { %>
        <div class="team-grid">
        <% for (Team t : teams) { %>
            <a class="team-card" href="${pageContext.request.contextPath}/account/team/detail?id=<%= t.getTeamId() %>">
                <div class="team-name"><%= HtmlUtils.escape(t.getName()) %></div>
                <div class="team-owner">Chủ nhóm: <%= HtmlUtils.escape(t.getOwnerDisplayName() != null && !t.getOwnerDisplayName().isEmpty() ? t.getOwnerDisplayName() : t.getOwnerUsername()) %></div>
                <div class="team-meta">
                    <span><%= t.getMemberCount() != null ? t.getMemberCount() : 0 %> thành viên</span>
                    <span><%= t.getCreatedAt() != null ? t.getCreatedAt().format(fmt) : "" %></span>
                </div>
            </a>
        <% } %>
        </div>
    <% } %>

    <%
        int currentPage = request.getAttribute("page") == null ? 1 : (int) request.getAttribute("page");
        int totalPages = request.getAttribute("totalPages") == null ? 1 : (int) request.getAttribute("totalPages");
    %>
    <% if (totalPages > 1) { %>
        <div class="pagination">
            <% for (int p = 1; p <= totalPages; p++) { %>
                <a href="${pageContext.request.contextPath}/account/teams?page=<%= p %>"
                   style="<%= p == currentPage ? "border-color: var(--accent-purple);" : "" %>"><%= p %></a>
            <% } %>
        </div>
    <% } %>

    <p style="margin-top: 30px;"><a href="${pageContext.request.contextPath}/account/home.jsp" style="color: var(--text-secondary);">&larr; Về trang chủ</a></p>
</div>
</body>
</html>
