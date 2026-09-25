<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="com.gamenest.model.AccountFriendship" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Bạn bè - GameNest</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        body { display: block; padding: 30px 20px; }
        .page-wrap { max-width: 600px; margin: 0 auto; }
        .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 10px; }
        .tab-link { padding: 8px 16px; border-radius: 8px; border: 1px solid var(--border-color); color: var(--text-secondary); text-decoration: none; font-size: 0.88rem; }
        .account-list { display: flex; flex-direction: column; gap: 10px; }
        .account-row { display: flex; align-items: center; gap: 12px; background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 10px; padding: 12px 16px; }
        .account-row:hover { border-color: var(--border-hover); }
        .account-row-link { flex: 1; min-width: 0; display: flex; align-items: center; gap: 12px; text-decoration: none; color: var(--text-primary); }
        .account-avatar-img, .account-avatar-circle { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
        .account-avatar-circle { display: flex; align-items: center; justify-content: center; background: var(--accent-purple); color: #fff; font-weight: 600; }
        .account-name { font-weight: 600; font-size: 0.92rem; }
        .account-username { color: var(--text-secondary); font-size: 0.8rem; }
        .btn-message { padding: 6px 12px; border-radius: 6px; border: 1px solid var(--border-color); color: var(--text-secondary); text-decoration: none; font-size: 0.78rem; white-space: nowrap; flex-shrink: 0; }
        .btn-message:hover { color: var(--text-primary); border-color: var(--border-hover); }
        .presence-indicator { display: inline-flex; align-items: center; gap: 5px; font-size: 0.76rem; color: var(--text-secondary); flex-shrink: 0; white-space: nowrap; }
        .presence-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--text-secondary); display: inline-block; }
        .presence-indicator.online { color: var(--accent-cyan); }
        .presence-indicator.online .presence-dot { background: var(--accent-cyan); }
        .empty { color: var(--text-secondary); margin-top: 20px; }
        .error-msg { color: var(--error-color); margin-bottom: 16px; }
        .pagination { margin-top: 24px; display: flex; gap: 8px; }
        .pagination a { padding: 8px 12px; border-radius: 6px; border: 1px solid var(--border-color); color: var(--text-primary); text-decoration: none; }
    </style>
</head>
<body>
<div class="page-wrap">
    <div class="page-header">
        <h1>Bạn bè</h1>
        <a class="tab-link" href="${pageContext.request.contextPath}/account/friends/requests">Lời mời kết bạn</a>
    </div>

    <% if (request.getAttribute("error") != null) { %>
        <p class="error-msg"><%= HtmlUtils.escape((String) request.getAttribute("error")) %></p>
    <% } %>

    <%
        @SuppressWarnings("unchecked")
        List<AccountFriendship> friends = (List<AccountFriendship>) request.getAttribute("friends");
        Integer currentAccountId = (Integer) request.getAttribute("currentAccountId");
        @SuppressWarnings("unchecked")
        Map<Integer, Boolean> presenceByAccountId = (Map<Integer, Boolean>) request.getAttribute("presenceByAccountId");
    %>
    <% if (friends == null || friends.isEmpty()) { %>
        <p class="empty">Bạn chưa có người bạn nào.</p>
    <% } else { %>
        <div class="account-list">
        <% for (AccountFriendship f : friends) {
            String rowLabel = f.getOtherDisplayName() != null && !f.getOtherDisplayName().isEmpty() ? f.getOtherDisplayName() : f.getOtherUsername();
            String rowInitial = rowLabel != null && !rowLabel.isEmpty() ? rowLabel.substring(0, 1).toUpperCase() : "U";
            int otherAccountId = currentAccountId != null && f.getRequesterAccountId() == currentAccountId
                    ? f.getReceiverAccountId() : f.getRequesterAccountId();
            boolean isOnline = presenceByAccountId != null && Boolean.TRUE.equals(presenceByAccountId.get(otherAccountId));
        %>
            <div class="account-row">
                <a class="account-row-link" href="${pageContext.request.contextPath}/account/view?username=<%= HtmlUtils.escape(f.getOtherUsername()) %>">
                    <% if (f.getOtherAvatarUrl() != null && !f.getOtherAvatarUrl().isEmpty()) { %>
                    <img class="account-avatar-img" src="<%= HtmlUtils.escape(f.getOtherAvatarUrl()) %>" alt="">
                    <% } else { %>
                    <span class="account-avatar-circle"><%= HtmlUtils.escape(rowInitial) %></span>
                    <% } %>
                    <div>
                        <div class="account-name"><%= HtmlUtils.escape(rowLabel) %></div>
                        <div class="account-username">@<%= HtmlUtils.escape(f.getOtherUsername()) %></div>
                    </div>
                </a>
                <span class="presence-indicator <%= isOnline ? "online" : "offline" %>" data-presence-account-id="<%= otherAccountId %>">
                    <span class="presence-dot"></span><span class="presence-label"><%= isOnline ? "Online" : "Offline" %></span>
                </span>
                <a class="btn-message" href="${pageContext.request.contextPath}/account/chat/direct?username=<%= HtmlUtils.escape(f.getOtherUsername()) %>">Nhắn tin</a>
            </div>
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
                <a href="${pageContext.request.contextPath}/account/friends?page=<%= p %>"
                   style="<%= p == currentPage ? "border-color: var(--accent-purple);" : "" %>"><%= p %></a>
            <% } %>
        </div>
    <% } %>

    <p style="margin-top: 30px;"><a href="${pageContext.request.contextPath}/account/home.jsp" style="color: var(--text-secondary);">&larr; Về trang chủ</a></p>
</div>

<script>
    window.GameNestPresence = {
        contextPath: "${pageContext.request.contextPath}"
    };
</script>
<script src="${pageContext.request.contextPath}/js/presence-realtime.js"></script>
</body>
</html>
