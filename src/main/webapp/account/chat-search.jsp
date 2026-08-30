<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%@ page import="com.gamenest.model.Conversation" %>
<%@ page import="com.gamenest.model.Message" %>
<%@ page import="com.gamenest.model.MessageSearchResult" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <%
        Conversation conversation = (Conversation) request.getAttribute("conversation");
        String displayName = (String) request.getAttribute("displayName");
        String keyword = (String) request.getAttribute("keyword");
    %>
    <title>Tìm kiếm tin nhắn - GameNest</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        body { display: block; padding: 30px 20px; }
        .page-wrap { max-width: 700px; margin: 0 auto; }
        .search-title { font-size: 1.2rem; font-weight: 700; margin-bottom: 10px; }
        .chat-search-form { display: flex; gap: 6px; margin-bottom: 20px; }
        .chat-search-form input[type=text] { flex: 1; padding: 8px 12px; border-radius: 6px; border: 1px solid var(--border-color); background: var(--bg-primary); color: var(--text-primary); font-family: inherit; font-size: 0.88rem; }
        .btn-sm { padding: 8px 14px; border-radius: 6px; border: 1px solid var(--border-color); background: transparent; color: var(--text-primary); cursor: pointer; font-size: 0.82rem; }
        .search-result { display: block; background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 10px; padding: 12px 16px; margin-bottom: 10px; text-decoration: none; color: inherit; }
        .search-result:hover { border-color: var(--border-hover); }
        .search-result-header { display: flex; justify-content: space-between; gap: 8px; margin-bottom: 4px; }
        .search-result-sender { font-weight: 600; font-size: 0.86rem; }
        .search-result-time { color: var(--text-secondary); font-size: 0.74rem; }
        .search-result-content { white-space: pre-wrap; line-height: 1.4; font-size: 0.88rem; }
        .empty { color: var(--text-secondary); margin-top: 20px; }
    </style>
</head>
<body>
<div class="page-wrap">
    <% if (conversation == null) { %>
        <p>Cuộc trò chuyện không tồn tại.</p>
        <p><a href="${pageContext.request.contextPath}/account/chats" style="color: var(--text-secondary);">&larr; Quay lại</a></p>
    <% } else {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
        @SuppressWarnings("unchecked")
        List<MessageSearchResult> results = (List<MessageSearchResult>) request.getAttribute("results");
    %>
        <div class="search-title">Tìm kiếm trong: <%= HtmlUtils.escape(displayName != null ? displayName : "Trò chuyện") %></div>
        <form class="chat-search-form" method="get" action="${pageContext.request.contextPath}/account/chat/search">
            <input type="hidden" name="id" value="<%= conversation.getConversationId() %>">
            <input type="text" name="keyword" maxlength="200" placeholder="Tìm tin nhắn..." value="<%= HtmlUtils.escape(keyword) %>">
            <button type="submit" class="btn-sm">Tìm</button>
        </form>

        <% if (results == null || results.isEmpty()) { %>
            <p class="empty">Không tìm thấy tin nhắn nào phù hợp.</p>
        <% } else {
            for (MessageSearchResult result : results) {
                Message m = result.getMessage();
                String senderLabel = m.getSenderDisplayName() != null && !m.getSenderDisplayName().isEmpty() ? m.getSenderDisplayName() : m.getSenderUsername();
        %>
            <a class="search-result" href="${pageContext.request.contextPath}/account/chat/detail?id=<%= conversation.getConversationId() %>&page=<%= result.getPage() %>&highlight=<%= m.getMessageId() %>">
                <div class="search-result-header">
                    <span class="search-result-sender"><%= HtmlUtils.escape(senderLabel) %></span>
                    <span class="search-result-time"><%= m.getCreatedAt() != null ? m.getCreatedAt().format(fmt) : "" %></span>
                </div>
                <div class="search-result-content"><%= HtmlUtils.escape(m.getContent()) %></div>
            </a>
        <% }
        } %>

        <p style="margin-top: 30px;"><a href="${pageContext.request.contextPath}/account/chat/detail?id=<%= conversation.getConversationId() %>" style="color: var(--text-secondary);">&larr; Quay lại cuộc trò chuyện</a></p>
    <% } %>
</div>
</body>
</html>
