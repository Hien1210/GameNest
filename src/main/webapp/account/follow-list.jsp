<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.gamenest.model.Account" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <%
        Account target = (Account) request.getAttribute("target");
        String listTitle = (String) request.getAttribute("listTitle");
        String pageHeading = listTitle == null ? "Danh sách" : listTitle;
    %>
    <title><%= HtmlUtils.escape(pageHeading) %><%= target != null ? " của " + HtmlUtils.escape(target.getUsername()) : "" %> - GameNest</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        body { display: block; padding: 30px 20px; }
        .page-wrap { max-width: 600px; margin: 0 auto; }
        .page-header { margin-bottom: 6px; }
        .page-subtitle { color: var(--text-secondary); margin-bottom: 20px; font-size: 0.9rem; }
        .account-list { display: flex; flex-direction: column; gap: 10px; }
        .account-row { display: flex; align-items: center; gap: 12px; background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 10px; padding: 12px 16px; text-decoration: none; color: var(--text-primary); }
        .account-row:hover { border-color: var(--border-hover); }
        .account-avatar-img, .account-avatar-circle { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
        .account-avatar-circle { display: flex; align-items: center; justify-content: center; background: var(--accent-purple); color: #fff; font-weight: 600; }
        .account-name { font-weight: 600; font-size: 0.92rem; }
        .account-username { color: var(--text-secondary); font-size: 0.8rem; }
        .empty { color: var(--text-secondary); margin-top: 20px; }
        .error-msg { color: var(--error-color); margin-bottom: 16px; }
        .pagination { margin-top: 24px; display: flex; gap: 8px; }
        .pagination a { padding: 8px 12px; border-radius: 6px; border: 1px solid var(--border-color); color: var(--text-primary); text-decoration: none; }
    </style>
</head>
<body>
<div class="page-wrap">
    <% if (request.getAttribute("error") != null) { %>
        <p class="error-msg"><%= HtmlUtils.escape((String) request.getAttribute("error")) %></p>
        <p><a href="${pageContext.request.contextPath}/" style="color: var(--text-secondary);">&larr; Về trang chủ</a></p>
    <% } else { %>
        <div class="page-header">
            <h1><%= HtmlUtils.escape(pageHeading) %></h1>
        </div>
        <div class="page-subtitle">@<%= HtmlUtils.escape(target.getUsername()) %></div>

        <%
            @SuppressWarnings("unchecked")
            List<Account> accounts = (List<Account>) request.getAttribute("accounts");
            String emptyMessage = (String) request.getAttribute("emptyMessage");
        %>
        <% if (accounts == null || accounts.isEmpty()) { %>
            <p class="empty"><%= HtmlUtils.escape(emptyMessage == null ? "Không có dữ liệu." : emptyMessage) %></p>
        <% } else { %>
            <div class="account-list">
            <% for (Account a : accounts) {
                String rowLabel = a.getDisplayName() != null && !a.getDisplayName().isEmpty() ? a.getDisplayName() : a.getUsername();
                String rowInitial = rowLabel != null && !rowLabel.isEmpty() ? rowLabel.substring(0, 1).toUpperCase() : "U";
            %>
                <a class="account-row" href="${pageContext.request.contextPath}/account/view?username=<%= HtmlUtils.escape(a.getUsername()) %>">
                    <% if (a.getAvatarUrl() != null && !a.getAvatarUrl().isEmpty()) { %>
                    <img class="account-avatar-img" src="<%= HtmlUtils.escape(a.getAvatarUrl()) %>" alt="">
                    <% } else { %>
                    <span class="account-avatar-circle"><%= rowInitial %></span>
                    <% } %>
                    <div>
                        <div class="account-name"><%= HtmlUtils.escape(rowLabel) %></div>
                        <div class="account-username">@<%= HtmlUtils.escape(a.getUsername()) %></div>
                    </div>
                </a>
            <% } %>
            </div>
        <% } %>

        <%
            int currentPage = request.getAttribute("page") == null ? 1 : (int) request.getAttribute("page");
            int totalPages = request.getAttribute("totalPages") == null ? 1 : (int) request.getAttribute("totalPages");
            String usernameParam = HtmlUtils.escape(target.getUsername());
            String basePath = "followers".equalsIgnoreCase(pageHeading) ? "/account/followers" : "/account/following";
        %>
        <% if (totalPages > 1) { %>
            <div class="pagination">
                <% for (int p = 1; p <= totalPages; p++) { %>
                    <a href="${pageContext.request.contextPath}<%= basePath %>?username=<%= usernameParam %>&page=<%= p %>"
                       style="<%= p == currentPage ? "border-color: var(--accent-purple);" : "" %>"><%= p %></a>
                <% } %>
            </div>
        <% } %>

        <p style="margin-top: 30px;"><a href="${pageContext.request.contextPath}/account/view?username=<%= usernameParam %>" style="color: var(--text-secondary);">&larr; Quay lại Profile</a></p>
    <% } %>
</div>
</body>
</html>
