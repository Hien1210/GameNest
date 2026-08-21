<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%@ page import="com.gamenest.model.Conversation" %>
<%@ page import="com.gamenest.model.ConversationType" %>
<%@ page import="com.gamenest.model.Message" %>
<%@ page import="com.gamenest.model.MessageReactionSummary" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <%
        Conversation conversation = (Conversation) request.getAttribute("conversation");
        String pageTitle = (String) request.getAttribute("displayName");
    %>
    <title><%= pageTitle != null ? HtmlUtils.escape(pageTitle) : "Trò chuyện" %> - GameNest</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        body { display: block; padding: 30px 20px; }
        .page-wrap { max-width: 700px; margin: 0 auto; }
        .chat-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 10px; }
        .chat-title-group { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
        .chat-title { font-size: 1.3rem; font-weight: 700; }
        .presence-indicator { display: inline-flex; align-items: center; gap: 5px; font-size: 0.78rem; color: var(--text-secondary); }
        .presence-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--text-secondary); display: inline-block; }
        .presence-indicator.online { color: var(--accent-cyan); }
        .presence-indicator.online .presence-dot { background: var(--accent-cyan); }
        .send-card { background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 12px; padding: 16px; margin-bottom: 18px; }
        .send-form { display: flex; gap: 8px; }
        .send-form textarea { flex: 1; min-height: 60px; padding: 10px 12px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-primary); color: var(--text-primary); font-family: inherit; resize: vertical; }
        .btn { padding: 10px 18px; border-radius: 8px; border: none; cursor: pointer; font-size: 0.88rem; font-weight: 600; }
        .btn-primary { background: var(--accent-purple); color: #fff; align-self: flex-start; }
        .btn-secondary { background: transparent; border: 1px solid var(--border-color); color: var(--text-primary); }
        .toolbar { display: flex; justify-content: flex-end; margin-bottom: 14px; }
        .btn-sm { padding: 5px 10px; border-radius: 6px; border: 1px solid var(--border-color); background: transparent; color: var(--text-secondary); cursor: pointer; font-size: 0.74rem; }
        .message-card { background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 10px; padding: 12px 16px; margin-bottom: 10px; }
        .message-card.own { border-color: var(--border-hover); }
        .message-header { display: flex; justify-content: space-between; align-items: center; gap: 8px; margin-bottom: 4px; }
        .message-sender { font-weight: 600; font-size: 0.86rem; }
        .message-time { color: var(--text-secondary); font-size: 0.74rem; }
        .message-content { white-space: pre-wrap; line-height: 1.4; font-size: 0.9rem; }
        .message-content.deleted { color: var(--text-secondary); font-style: italic; }
        .message-edited-tag { color: var(--text-secondary); font-size: 0.72rem; margin-left: 6px; }
        .read-status { display: block; margin-top: 4px; font-size: 0.7rem; color: var(--text-secondary); text-align: right; }
        .typing-indicator { font-size: 0.78rem; color: var(--text-secondary); font-style: italic; margin-bottom: 10px; }
        .message-actions { display: flex; gap: 8px; margin-top: 8px; }
        .message-reply-row { margin-top: 6px; }
        .reaction-bar { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; margin-top: 8px; position: relative; }
        .reaction-chip { display: inline-flex; align-items: center; gap: 4px; padding: 2px 8px; border-radius: 12px; border: 1px solid var(--border-color); background: var(--bg-primary); color: var(--text-primary); cursor: pointer; font-size: 0.82rem; }
        .reaction-chip.mine { border-color: var(--accent-purple); background: rgba(139, 92, 246, 0.12); }
        .reaction-count { font-size: 0.74rem; color: var(--text-secondary); }
        .reaction-add-wrap { position: relative; display: inline-block; }
        .reaction-add-btn { width: 22px; height: 22px; line-height: 20px; padding: 0; border-radius: 50%; border: 1px solid var(--border-color); background: transparent; color: var(--text-secondary); cursor: pointer; font-size: 0.86rem; }
        .reaction-picker { position: absolute; bottom: 26px; left: 0; z-index: 5; display: flex; gap: 4px; padding: 6px 8px; border-radius: 10px; border: 1px solid var(--border-color); background: var(--bg-secondary); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25); }
        .reaction-picker-btn { padding: 2px 5px; border: none; background: transparent; cursor: pointer; font-size: 1rem; border-radius: 6px; }
        .reaction-picker-btn:hover { background: var(--bg-primary); }
        .reply-btn { padding: 3px 8px; border-radius: 6px; border: 1px solid var(--border-color); background: transparent; color: var(--text-secondary); cursor: pointer; font-size: 0.72rem; }
        .reply-target-preview { border-left: 3px solid var(--border-hover); padding: 4px 8px; margin-bottom: 6px; border-radius: 4px; background: var(--bg-primary); cursor: pointer; font-size: 0.8rem; }
        .reply-target-sender { font-weight: 600; margin-right: 6px; }
        .reply-target-text { color: var(--text-secondary); }
        .reply-target-deleted { color: var(--text-secondary); font-style: italic; }
        .message-card.highlight { border-color: var(--accent-purple); }
        .reply-compose-preview { display: flex; align-items: center; justify-content: space-between; gap: 8px; border-left: 3px solid var(--accent-purple); padding: 6px 10px; margin-bottom: 8px; border-radius: 4px; background: var(--bg-primary); font-size: 0.8rem; }
        .reply-compose-label { font-weight: 600; margin-right: 6px; }
        .reply-compose-text { color: var(--text-secondary); }
        .reply-compose-cancel { background: transparent; border: none; color: var(--text-secondary); cursor: pointer; font-size: 0.9rem; }
        .inline-form { margin: 0; }
        .edit-form { display: flex; gap: 6px; margin-top: 8px; }
        .edit-form input[type=text] { flex: 1; padding: 6px 10px; border-radius: 6px; border: 1px solid var(--border-color); background: var(--bg-primary); color: var(--text-primary); font-family: inherit; font-size: 0.85rem; }
        .error-msg { color: var(--error-color); margin-bottom: 16px; }
        .empty { color: var(--text-secondary); margin-top: 20px; }
        .pagination { margin-top: 24px; display: flex; gap: 8px; }
        .pagination a { padding: 8px 12px; border-radius: 6px; border: 1px solid var(--border-color); color: var(--text-primary); text-decoration: none; }
    </style>
</head>
<body>
<div class="page-wrap">
    <% if (request.getAttribute("error") != null) { %>
        <p class="error-msg"><%= HtmlUtils.escape((String) request.getAttribute("error")) %></p>
    <% } %>

    <% if (conversation == null) { %>
        <p>Cuộc trò chuyện không tồn tại.</p>
        <p><a href="${pageContext.request.contextPath}/account/chats" style="color: var(--text-secondary);">&larr; Quay lại</a></p>
    <% } else {
        Integer currentAccountId = (Integer) request.getAttribute("currentAccountId");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
        @SuppressWarnings("unchecked")
        List<Message> messages = (List<Message>) request.getAttribute("messages");
    %>
        <%
            Integer otherAccountId = (Integer) request.getAttribute("otherAccountId");
            boolean otherOnline = Boolean.TRUE.equals(request.getAttribute("otherOnline"));
        %>
        <div class="chat-header">
            <span class="chat-title-group">
                <span class="chat-title"><%= HtmlUtils.escape(pageTitle != null ? pageTitle : "Trò chuyện") %></span>
                <% if (otherAccountId != null) { %>
                <span class="presence-indicator <%= otherOnline ? "online" : "offline" %>" data-presence-account-id="<%= otherAccountId %>">
                    <span class="presence-dot"></span><span class="presence-label"><%= otherOnline ? "Online" : "Offline" %></span>
                </span>
                <% } %>
            </span>
        </div>

        <% if (ConversationType.DIRECT.equals(conversation.getType())) { %>
        <div id="typingIndicator" class="typing-indicator" style="display: none;">Đang nhập...</div>
        <% } %>

        <div class="send-card">
            <div id="replyComposePreview" class="reply-compose-preview" style="display: none;">
                <span><span class="reply-compose-label"></span><span class="reply-compose-text"></span></span>
                <button type="button" class="reply-compose-cancel" title="Hủy trả lời">&times;</button>
            </div>
            <form class="send-form" method="post" action="${pageContext.request.contextPath}/account/chat/send">
                <input type="hidden" name="conversationId" value="<%= conversation.getConversationId() %>">
                <input type="hidden" id="replyToMessageId" name="replyToMessageId" value="">
                <textarea name="content" maxlength="2000" placeholder="Nhập tin nhắn..." required></textarea>
                <button type="submit" class="btn btn-primary">Gửi</button>
            </form>
        </div>

        <% if (messages != null && !messages.isEmpty()) { %>
        <div class="toolbar">
            <form class="inline-form" method="post" action="${pageContext.request.contextPath}/account/chat/read">
                <input type="hidden" name="conversationId" value="<%= conversation.getConversationId() %>">
                <input type="hidden" name="messageId" value="<%= messages.get(0).getMessageId() %>">
                <button type="submit" class="btn-sm">Đánh dấu đã đọc</button>
            </form>
        </div>
        <% } %>

        <div id="messageList">
        <% if (messages == null || messages.isEmpty()) { %>
            <p class="empty" id="emptyMessagesPlaceholder">Chưa có tin nhắn nào. Hãy bắt đầu cuộc trò chuyện!</p>
        <% } else {
            String[] REACTION_EMOJI = {"👍", "❤️", "😂", "😮", "😢", "😡"};
            for (Message m : messages) {
                boolean isOwn = currentAccountId != null && m.getSenderAccountId() == currentAccountId;
                boolean isDeleted = m.getDeletedAt() != null;
                String senderLabel = m.getSenderDisplayName() != null && !m.getSenderDisplayName().isEmpty() ? m.getSenderDisplayName() : m.getSenderUsername();

                Integer replyToMessageId = m.getReplyToMessageId();
                boolean replyToDeleted = m.getReplyToDeletedAt() != null;
                boolean replyTargetIsOwn = replyToMessageId != null && currentAccountId != null
                        && m.getReplyToSenderAccountId() != null && m.getReplyToSenderAccountId() == currentAccountId;
                String replyTargetLabel = null;
                String replyTargetPreview = null;
                if (replyToMessageId != null && !replyToDeleted) {
                    replyTargetLabel = replyTargetIsOwn ? "Bạn"
                            : (m.getReplyToSenderDisplayName() != null && !m.getReplyToSenderDisplayName().isEmpty()
                                    ? m.getReplyToSenderDisplayName() : m.getReplyToSenderUsername());
                    replyTargetPreview = m.getReplyToContent();
                    if (replyTargetPreview != null && replyTargetPreview.length() > 80) {
                        replyTargetPreview = replyTargetPreview.substring(0, 80) + "…";
                    }
                }
                String composePreviewText = m.getContent() != null && m.getContent().length() > 80
                        ? m.getContent().substring(0, 80) + "…" : m.getContent();
            %>
            <div class="message-card <%= isOwn ? "own" : "" %>" data-message-id="<%= m.getMessageId() %>">
                <div class="message-header">
                    <span class="message-sender"><%= HtmlUtils.escape(isOwn ? "Bạn" : senderLabel) %></span>
                    <span class="message-time"><%= m.getCreatedAt() != null ? m.getCreatedAt().format(fmt) : "" %></span>
                </div>
                <% if (replyToMessageId != null) { %>
                <div class="reply-target-preview" data-reply-target-id="<%= replyToMessageId %>">
                    <% if (replyToDeleted) { %>
                    <span class="reply-target-deleted">Tin nhắn đã được xóa</span>
                    <% } else { %>
                    <span class="reply-target-sender"><%= HtmlUtils.escape(replyTargetLabel) %></span>
                    <span class="reply-target-text"><%= HtmlUtils.escape(replyTargetPreview) %></span>
                    <% } %>
                </div>
                <% } %>
                <% if (isDeleted) { %>
                <div class="message-content deleted">Tin nhắn đã được xóa.</div>
                <% } else { %>
                <div class="message-content"><%= HtmlUtils.escape(m.getContent()) %><% if (m.getEditedAt() != null) { %><span class="message-edited-tag">(đã chỉnh sửa)</span><% } %></div>

                <% if (!isDeleted) { %>
                <div class="reaction-bar" data-message-id="<%= m.getMessageId() %>">
                    <% for (MessageReactionSummary r : m.getReactions()) {
                        boolean mine = r.getEmoji().equals(m.getMyReaction());
                    %>
                    <form class="inline-form reaction-form" method="post" action="${pageContext.request.contextPath}/account/chat/reaction">
                        <input type="hidden" name="messageId" value="<%= m.getMessageId() %>">
                        <input type="hidden" name="emoji" value="<%= r.getEmoji() %>">
                        <button type="submit" class="reaction-chip <%= mine ? "mine" : "" %>"><%= r.getEmoji() %> <span class="reaction-count"><%= r.getCount() %></span></button>
                    </form>
                    <% } %>
                    <div class="reaction-add-wrap">
                        <button type="button" class="reaction-add-btn" title="Thả cảm xúc">+</button>
                        <div class="reaction-picker" style="display: none;">
                            <% for (String emoji : REACTION_EMOJI) { %>
                            <form class="inline-form reaction-form" method="post" action="${pageContext.request.contextPath}/account/chat/reaction">
                                <input type="hidden" name="messageId" value="<%= m.getMessageId() %>">
                                <input type="hidden" name="emoji" value="<%= emoji %>">
                                <button type="submit" class="reaction-picker-btn"><%= emoji %></button>
                            </form>
                            <% } %>
                        </div>
                    </div>
                </div>
                <% } %>

                <% if (isOwn && !isDeleted && ConversationType.DIRECT.equals(conversation.getType())) { %>
                <span class="read-status"></span>
                <% } %>

                <% if (!isDeleted) { %>
                <div class="message-reply-row">
                    <button type="button" class="reply-btn"
                            data-message-id="<%= m.getMessageId() %>"
                            data-sender-label="<%= HtmlUtils.escape(isOwn ? "Bạn" : senderLabel) %>"
                            data-preview="<%= HtmlUtils.escape(composePreviewText) %>">Trả lời</button>
                </div>
                <% } %>

                <% if (isOwn && !isDeleted) { %>
                <div class="message-actions">
                    <form class="edit-form" method="post" action="${pageContext.request.contextPath}/account/chat/edit">
                        <input type="hidden" name="messageId" value="<%= m.getMessageId() %>">
                        <input type="hidden" name="conversationId" value="<%= conversation.getConversationId() %>">
                        <input type="text" name="content" maxlength="2000" value="<%= HtmlUtils.escape(m.getContent()) %>" required>
                        <button type="submit" class="btn-sm">Lưu</button>
                    </form>
                    <form class="inline-form" method="post" action="${pageContext.request.contextPath}/account/chat/delete"
                          onsubmit="return confirm('Xóa tin nhắn này?');">
                        <input type="hidden" name="messageId" value="<%= m.getMessageId() %>">
                        <input type="hidden" name="conversationId" value="<%= conversation.getConversationId() %>">
                        <button type="submit" class="btn-sm">Xóa</button>
                    </form>
                </div>
                <% } %>
            </div>
            <% } %>
        <% } %>
        </div>

        <%
            int currentPage = request.getAttribute("page") == null ? 1 : (int) request.getAttribute("page");
            int totalPages = request.getAttribute("totalPages") == null ? 1 : (int) request.getAttribute("totalPages");
        %>
        <% if (totalPages > 1) { %>
            <div class="pagination">
                <% for (int p = 1; p <= totalPages; p++) { %>
                    <a href="${pageContext.request.contextPath}/account/chat/detail?id=<%= conversation.getConversationId() %>&page=<%= p %>"
                       style="<%= p == currentPage ? "border-color: var(--accent-purple);" : "" %>"><%= p %></a>
                <% } %>
            </div>
        <% } %>

        <p style="margin-top: 30px;"><a href="${pageContext.request.contextPath}/account/chats" style="color: var(--text-secondary);">&larr; Quay lại danh sách</a></p>

        <script>
            window.GameNestChat = {
                contextPath: "${pageContext.request.contextPath}",
                conversationId: <%= conversation.getConversationId() %>,
                accountId: <%= currentAccountId %>,
                conversationType: "<%= conversation.getType() %>"
            };
        </script>
        <script src="${pageContext.request.contextPath}/js/chat-realtime.js"></script>
        <% if (otherAccountId != null) { %>
        <script>
            window.GameNestPresence = {
                contextPath: "${pageContext.request.contextPath}"
            };
        </script>
        <script src="${pageContext.request.contextPath}/js/presence-realtime.js"></script>
        <% } %>
    <% } %>
</div>
</body>
</html>
