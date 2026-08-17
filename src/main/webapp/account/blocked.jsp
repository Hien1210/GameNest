<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%@ page import="com.gamenest.model.AccountBlock" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đã chặn - GameNest</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        body { display: block; padding: 30px 20px; }
        .page-wrap { max-width: 600px; margin: 0 auto; }
        .block-card { display: flex; align-items: center; gap: 12px; background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 10px; padding: 12px 16px; margin-bottom: 10px; }
        .block-avatar-img, .block-avatar-circle { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
        .block-avatar-circle { display: flex; align-items: center; justify-content: center; background: var(--accent-purple); color: #fff; font-weight: 600; }
        .block-info { flex: 1; min-width: 0; }
        .block-name { font-weight: 600; font-size: 0.92rem; }
        .block-time { color: var(--text-secondary); font-size: 0.78rem; }
        .inline-form { margin: 0; }
        .btn-unblock { padding: 8px 14px; border-radius: 8px; border: 1px solid var(--border-color); background: transparent; color: var(--text-primary); cursor: pointer; font-size: 0.82rem; }
        .empty { color: var(--text-secondary); margin-top: 20px; }
        .error-msg { color: var(--error-color); margin-bottom: 16px; }
        .pagination { margin-top: 24px; display: flex; gap: 8px; }
        .pagination a { padding: 8px 12px; border-radius: 6px; border: 1px solid var(--border-color); color: var(--text-primary); text-decoration: none; }
    </style>
</head>
<body>
<div class="page-wrap">
    <h1>Đã chặn</h1>

    <% if (request.getAttribute("error") != null) { %>
        <p class="error-msg"><%= HtmlUtils.escape((String) request.getAttribute("error")) %></p>
    <% } %>

    <%
        @SuppressWarnings("unchecked")
        List<AccountBlock> blocked = (List<AccountBlock>) request.getAttribute("blocked");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    %>
    <% if (blocked == null || blocked.isEmpty()) { %>
        <p class="empty">Bạn chưa chặn người dùng nào.</p>
    <% } else { %>
        <% for (AccountBlock b : blocked) {
            String rowLabel = b.getBlockedDisplayName() != null && !b.getBlockedDisplayName().isEmpty() ? b.getBlockedDisplayName() : b.getBlockedUsername();
            String rowInitial = rowLabel != null && !rowLabel.isEmpty() ? rowLabel.substring(0, 1).toUpperCase() : "U";
            String encodedUsername = HtmlUtils.escape(b.getBlockedUsername());
        %>
        <div class="block-card">
            <% if (b.getBlockedAvatarUrl() != null && !b.getBlockedAvatarUrl().isEmpty()) { %>
            <img class="block-avatar-img" src="<%= HtmlUtils.escape(b.getBlockedAvatarUrl()) %>" alt="">
            <% } else { %>
            <span class="block-avatar-circle"><%= rowInitial %></span>
            <% } %>
            <div class="block-info">
                <div class="block-name"><%= HtmlUtils.escape(rowLabel) %></div>
                <div class="block-time"><%= b.getCreatedAt() != null ? b.getCreatedAt().format(fmt) : "" %></div>
            </div>
            <form class="inline-form" method="post" action="${pageContext.request.contextPath}/account/unblock">
                <input type="hidden" name="username" value="<%= encodedUsername %>">
                <button type="submit" class="btn-unblock">Bỏ chặn</button>
            </form>
        </div>
        <% } %>
    <% } %>

    <%
        int currentPage = request.getAttribute("page") == null ? 1 : (int) request.getAttribute("page");
        int totalPages = request.getAttribute("totalPages") == null ? 1 : (int) request.getAttribute("totalPages");
    %>
    <% if (totalPages > 1) { %>
        <div class="pagination">
            <% for (int p = 1; p <= totalPages; p++) { %>
                <a href="${pageContext.request.contextPath}/account/blocked?page=<%= p %>"
                   style="<%= p == currentPage ? "border-color: var(--accent-purple);" : "" %>"><%= p %></a>
            <% } %>
        </div>
    <% } %>

    <p style="margin-top: 30px;"><a href="${pageContext.request.contextPath}/account/home.jsp" style="color: var(--text-secondary);">&larr; Về trang chủ</a></p>
</div>
</body>
</html>
