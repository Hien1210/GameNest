<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%@ page import="com.gamenest.model.Notification" %>
<%@ page import="com.gamenest.model.NotificationTargetType" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thông báo - GameNest</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        body { display: block; padding: 30px 20px; }
        .page-wrap { max-width: 720px; margin: 0 auto; }
        .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 10px; }
        .tabs { display: flex; gap: 8px; }
        .tabs a { padding: 8px 16px; border-radius: 8px; border: 1px solid var(--border-color); color: var(--text-secondary); text-decoration: none; font-size: 0.88rem; }
        .tabs a.active { background: var(--accent-purple); color: #fff; border-color: var(--accent-purple); }
        .btn-mark-all { padding: 9px 16px; border-radius: 8px; border: none; background: var(--bg-secondary); border: 1px solid var(--border-color); color: var(--text-primary); cursor: pointer; font-size: 0.85rem; }
        .notif-list { display: flex; flex-direction: column; gap: 10px; }
        .notif-card { background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 10px; padding: 14px 16px; display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
        .notif-card.unread { border-color: var(--accent-purple); background: rgba(139, 92, 246, 0.08); }
        .notif-body { flex: 1; min-width: 0; }
        .notif-title-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
        .notif-title { font-weight: 600; font-size: 0.95rem; }
        .notif-unread-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--accent-cyan); flex-shrink: 0; }
        .notif-message { font-size: 0.85rem; color: var(--text-secondary); margin-bottom: 6px; line-height: 1.4; }
        .notif-time { font-size: 0.76rem; color: var(--text-secondary); }
        .notif-link-btn { background: none; border: none; padding: 0; text-align: left; cursor: pointer; color: var(--text-primary); font: inherit; }
        .notif-link-btn:hover .notif-title { text-decoration: underline; }
        .notif-actions { display: flex; flex-direction: column; gap: 6px; align-items: flex-end; }
        .btn-mark-read { padding: 5px 10px; border-radius: 6px; border: 1px solid var(--border-color); background: transparent; color: var(--text-secondary); cursor: pointer; font-size: 0.76rem; white-space: nowrap; }
        .pagination { margin-top: 24px; display: flex; gap: 8px; }
        .pagination a { padding: 8px 12px; border-radius: 6px; border: 1px solid var(--border-color); color: var(--text-primary); text-decoration: none; }
        .empty { color: var(--text-secondary); margin-top: 20px; }
        .error-msg { color: var(--error-color); margin-bottom: 16px; }
        .inline-form { display: inline; margin: 0; }
    </style>
</head>
<body>
<div class="page-wrap">
    <div class="page-header">
        <h1>Thông báo</h1>
        <form class="inline-form" method="post" action="${pageContext.request.contextPath}/account/notifications/read-all">
            <button type="submit" class="btn-mark-all">Đánh dấu tất cả đã đọc</button>
        </form>
    </div>

    <%
        boolean unreadOnly = Boolean.TRUE.equals(request.getAttribute("unreadOnly"));
        Integer unreadCount = (Integer) request.getAttribute("unreadCount");
    %>
    <div class="tabs">
        <a href="${pageContext.request.contextPath}/account/notifications" class="<%= !unreadOnly ? "active" : "" %>">Tất cả</a>
        <a href="${pageContext.request.contextPath}/account/notifications?filter=unread" class="<%= unreadOnly ? "active" : "" %>">Chưa đọc<%= unreadCount != null && unreadCount > 0 ? " (" + unreadCount + ")" : "" %></a>
    </div>
    <br>

    <% if (request.getAttribute("error") != null) { %>
        <p class="error-msg"><%= HtmlUtils.escape((String) request.getAttribute("error")) %></p>
    <% } %>

    <%
        @SuppressWarnings("unchecked")
        List<Notification> notifications = (List<Notification>) request.getAttribute("notifications");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    %>
    <% if (notifications == null || notifications.isEmpty()) { %>
        <p class="empty"><%= unreadOnly ? "Không có thông báo chưa đọc." : "Bạn chưa có thông báo nào." %></p>
    <% } else { %>
        <div class="notif-list">
        <% for (Notification n : notifications) {
            boolean navigable = NotificationTargetType.QUESTION.equals(n.getTargetType())
                    || NotificationTargetType.ANSWER.equals(n.getTargetType())
                    || NotificationTargetType.ACCOUNT.equals(n.getTargetType());
        %>
            <div class="notif-card <%= n.isRead() ? "" : "unread" %>">
                <div class="notif-body">
                    <% if (navigable) { %>
                    <form class="inline-form" method="post" action="${pageContext.request.contextPath}/account/notifications/open">
                        <input type="hidden" name="id" value="<%= n.getNotificationId() %>">
                        <button type="submit" class="notif-link-btn">
                            <div class="notif-title-row">
                                <% if (!n.isRead()) { %><span class="notif-unread-dot"></span><% } %>
                                <span class="notif-title"><%= HtmlUtils.escape(n.getTitle()) %></span>
                            </div>
                        </button>
                    </form>
                    <% } else { %>
                    <div class="notif-title-row">
                        <% if (!n.isRead()) { %><span class="notif-unread-dot"></span><% } %>
                        <span class="notif-title"><%= HtmlUtils.escape(n.getTitle()) %></span>
                    </div>
                    <% } %>
                    <% if (n.getMessage() != null && !n.getMessage().isEmpty()) { %>
                    <div class="notif-message"><%= HtmlUtils.escape(n.getMessage()) %></div>
                    <% } %>
                    <div class="notif-time"><%= n.getCreatedAt() != null ? n.getCreatedAt().format(fmt) : "" %></div>
                </div>
                <% if (!n.isRead()) { %>
                <div class="notif-actions">
                    <form class="inline-form" method="post" action="${pageContext.request.contextPath}/account/notifications/read">
                        <input type="hidden" name="id" value="<%= n.getNotificationId() %>">
                        <button type="submit" class="btn-mark-read">Đánh dấu đã đọc</button>
                    </form>
                </div>
                <% } %>
            </div>
        <% } %>
        </div>
    <% } %>

    <%
        int currentPage = request.getAttribute("page") == null ? 1 : (int) request.getAttribute("page");
        int totalPages = request.getAttribute("totalPages") == null ? 1 : (int) request.getAttribute("totalPages");
        String filterQs = unreadOnly ? "&filter=unread" : "";
    %>
    <% if (totalPages > 1) { %>
        <div class="pagination">
            <% for (int p = 1; p <= totalPages; p++) { %>
                <a href="${pageContext.request.contextPath}/account/notifications?page=<%= p %><%= filterQs %>"
                   style="<%= p == currentPage ? "border-color: var(--accent-purple);" : "" %>"><%= p %></a>
            <% } %>
        </div>
    <% } %>

    <p style="margin-top: 30px;"><a href="${pageContext.request.contextPath}/account/home.jsp" style="color: var(--text-secondary);">&larr; Về trang chủ</a></p>
</div>
</body>
</html>
