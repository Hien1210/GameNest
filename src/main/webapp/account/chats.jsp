<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%@ page import="com.gamenest.model.Conversation" %>
<%@ page import="com.gamenest.model.ConversationType" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Trò chuyện - GameNest</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        body { display: block; padding: 30px 20px; }
        .page-wrap { max-width: 640px; margin: 0 auto; }
        .page-header { margin-bottom: 20px; }
        .chat-card { display: flex; align-items: center; gap: 12px; background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 10px; padding: 12px 16px; margin-bottom: 10px; text-decoration: none; color: var(--text-primary); }
        .chat-card:hover { border-color: var(--border-hover); }
        .chat-card.unread { border-color: var(--accent-purple); background: rgba(139, 92, 246, 0.08); }
        .chat-avatar-img, .chat-avatar-circle { width: 42px; height: 42px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
        .chat-avatar-circle { display: flex; align-items: center; justify-content: center; background: var(--accent-purple); color: #fff; font-weight: 600; }
        .chat-info { flex: 1; min-width: 0; }
        .chat-name-row { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
        .chat-name { font-weight: 600; font-size: 0.94rem; }
        .chat-time { color: var(--text-secondary); font-size: 0.76rem; flex-shrink: 0; }
        .chat-preview { color: var(--text-secondary); font-size: 0.84rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
        .unread-badge { background: var(--accent-purple); color: #fff; border-radius: 999px; padding: 1px 8px; font-size: 0.72rem; font-weight: 600; flex-shrink: 0; }
        .empty { color: var(--text-secondary); margin-top: 20px; }
        .error-msg { color: var(--error-color); margin-bottom: 16px; }
        .pagination { margin-top: 24px; display: flex; gap: 8px; }
        .pagination a { padding: 8px 12px; border-radius: 6px; border: 1px solid var(--border-color); color: var(--text-primary); text-decoration: none; }
    </style>
</head>
<body>
<div class="page-wrap">
    <div class="page-header">
        <h1>Trò chuyện</h1>
    </div>

    <% if (request.getAttribute("error") != null) { %>
        <p class="error-msg"><%= HtmlUtils.escape((String) request.getAttribute("error")) %></p>
    <% } %>

    <%
        @SuppressWarnings("unchecked")
        List<Conversation> conversations = (List<Conversation>) request.getAttribute("conversations");
        Integer currentAccountId = (Integer) request.getAttribute("currentAccountId");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    %>
    <% if (conversations == null || conversations.isEmpty()) { %>
        <p class="empty">Bạn chưa có cuộc trò chuyện nào. Hãy mở Chat từ trang Bạn bè hoặc Nhóm.</p>
    <% } else { %>
        <% for (Conversation c : conversations) {
            boolean isTeam = ConversationType.TEAM.equals(c.getType());
            String name = isTeam
                    ? c.getTeamName()
                    : (c.getOtherDisplayName() != null && !c.getOtherDisplayName().isEmpty() ? c.getOtherDisplayName() : c.getOtherUsername());
            if (name == null || name.isEmpty()) { name = isTeam ? "Nhóm" : "Người dùng"; }
            String initial = name.substring(0, 1).toUpperCase();
            String avatarUrl = isTeam ? null : c.getOtherAvatarUrl();

            String preview;
            if (c.getLatestMessageContent() == null) {
                preview = "Chưa có tin nhắn nào.";
            } else if (c.isLatestMessageDeleted()) {
                preview = "Tin nhắn đã được xóa.";
            } else {
                preview = c.getLatestMessageContent();
            }
        %>
        <a class="chat-card <%= c.getUnreadCount() > 0 ? "unread" : "" %>" href="${pageContext.request.contextPath}/account/chat/detail?id=<%= c.getConversationId() %>">
            <% if (avatarUrl != null && !avatarUrl.isEmpty()) { %>
            <img class="chat-avatar-img" src="<%= HtmlUtils.escape(avatarUrl) %>" alt="">
            <% } else { %>
            <span class="chat-avatar-circle"><%= HtmlUtils.escape(initial) %></span>
            <% } %>
            <div class="chat-info">
                <div class="chat-name-row">
                    <span class="chat-name"><%= HtmlUtils.escape(name) %><%= isTeam ? " (Nhóm)" : "" %></span>
                    <span class="chat-time"><%= c.getLatestMessageCreatedAt() != null ? c.getLatestMessageCreatedAt().format(fmt) : c.getCreatedAt().format(fmt) %></span>
                </div>
                <div style="display: flex; justify-content: space-between; align-items: center; gap: 8px;">
                    <span class="chat-preview"><%= HtmlUtils.escape(preview) %></span>
                    <% if (c.getUnreadCount() > 0) { %>
                    <span class="unread-badge"><%= c.getUnreadCount() > 99 ? "99+" : c.getUnreadCount() %></span>
                    <% } %>
                </div>
            </div>
        </a>
        <% } %>
    <% } %>

    <%
        int currentPage = request.getAttribute("page") == null ? 1 : (int) request.getAttribute("page");
        int totalPages = request.getAttribute("totalPages") == null ? 1 : (int) request.getAttribute("totalPages");
    %>
    <% if (totalPages > 1) { %>
        <div class="pagination">
            <% for (int p = 1; p <= totalPages; p++) { %>
                <a href="${pageContext.request.contextPath}/account/chats?page=<%= p %>"
                   style="<%= p == currentPage ? "border-color: var(--accent-purple);" : "" %>"><%= p %></a>
            <% } %>
        </div>
    <% } %>

    <p style="margin-top: 30px;"><a href="${pageContext.request.contextPath}/account/home.jsp" style="color: var(--text-secondary);">&larr; Về trang chủ</a></p>
</div>
</body>
</html>
