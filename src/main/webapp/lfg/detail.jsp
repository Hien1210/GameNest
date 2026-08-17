<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.gamenest.model.LFGMember" %>
<%@ page import="com.gamenest.model.LFGPost" %>
<%@ page import="com.gamenest.model.LFGStatus" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <%
        LFGPost post = (LFGPost) request.getAttribute("post");
        String pageTitle = post == null ? "Nhóm không tồn tại" : post.getTitle();
    %>
    <title><%= HtmlUtils.escape(pageTitle) %> - GameNest</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        body { display: block; padding: 30px 20px; }
        .page-wrap { max-width: 760px; margin: 0 auto; }
        .lfg-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; margin-bottom: 6px; flex-wrap: wrap; }
        .lfg-title { font-size: 1.5rem; font-weight: 700; }
        .lfg-game { color: var(--accent-cyan); font-size: 0.9rem; margin-bottom: 16px; }
        .status-badge { display: inline-block; padding: 3px 11px; border-radius: 999px; font-size: 0.78rem; font-weight: 600; }
        .status-open { background: rgba(16,185,129,0.15); color: var(--success-color); }
        .status-full { background: rgba(245,158,11,0.15); color: #f59e0b; }
        .status-closed, .status-expired { background: rgba(255,255,255,0.06); color: var(--text-secondary); }
        .info-card { background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 12px; padding: 20px; margin-bottom: 18px; }
        .info-row { display: flex; justify-content: space-between; padding: 6px 0; font-size: 0.9rem; border-bottom: 1px solid var(--border-color); }
        .info-row:last-child { border-bottom: none; }
        .info-row span:first-child { color: var(--text-secondary); }
        .lfg-desc { white-space: pre-wrap; line-height: 1.5; margin-top: 10px; }
        .members-list { list-style: none; padding: 0; margin: 0; }
        .members-list li { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px solid var(--border-color); }
        .members-list li:last-child { border-bottom: none; }
        .member-avatar { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; background: var(--bg-primary); }
        .actions { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 18px; }
        .btn { padding: 10px 18px; border-radius: 8px; border: none; cursor: pointer; font-size: 0.88rem; font-weight: 600; text-decoration: none; display: inline-block; }
        .btn-join { background: var(--accent-purple); color: #fff; }
        .btn-leave { background: rgba(239,68,68,0.15); color: var(--error-color); }
        .btn-edit { background: var(--bg-secondary); border: 1px solid var(--border-color); color: var(--text-primary); }
        .btn-close { background: rgba(245,158,11,0.15); color: #f59e0b; }
        .btn-delete { background: rgba(239,68,68,0.15); color: var(--error-color); }
        .error-msg { color: var(--error-color); margin-bottom: 16px; }
        .inline-form { display: inline; }
    </style>
</head>
<body>
<div class="page-wrap">
    <% if (request.getAttribute("error") != null) { %>
        <p class="error-msg"><%= HtmlUtils.escape((String) request.getAttribute("error")) %></p>
    <% } %>

    <% if (post == null) { %>
        <p>Nhóm không tồn tại hoặc đã bị xóa.</p>
        <p><a href="${pageContext.request.contextPath}/lfg" style="color: var(--text-secondary);">&larr; Quay lại danh sách</a></p>
    <% } else {
        boolean isLoggedIn = Boolean.TRUE.equals(request.getAttribute("isLoggedIn"));
        boolean isCreator = Boolean.TRUE.equals(request.getAttribute("isCreator"));
        boolean canJoin = Boolean.TRUE.equals(request.getAttribute("canJoin"));
        boolean canLeave = Boolean.TRUE.equals(request.getAttribute("canLeave"));
        boolean canManage = Boolean.TRUE.equals(request.getAttribute("canManage"));

        String badgeClass = "status-closed";
        if (LFGStatus.OPEN.equals(post.getStatus())) badgeClass = "status-open";
        else if (LFGStatus.FULL.equals(post.getStatus())) badgeClass = "status-full";
        else if (LFGStatus.EXPIRED.equals(post.getStatus())) badgeClass = "status-expired";

        @SuppressWarnings("unchecked")
        List<LFGMember> members = (List<LFGMember>) request.getAttribute("members");
    %>
        <div class="lfg-header">
            <span class="lfg-title"><%= HtmlUtils.escape(post.getTitle()) %></span>
            <span class="status-badge <%= badgeClass %>"><%= HtmlUtils.escape(post.getStatus()) %></span>
        </div>
        <div class="lfg-game"><%= HtmlUtils.escape(post.getGameName()) %></div>

        <div class="actions">
            <% if (canJoin) { %>
            <form class="inline-form" method="post" action="${pageContext.request.contextPath}/lfg/join">
                <input type="hidden" name="id" value="<%= post.getLfgId() %>">
                <button type="submit" class="btn btn-join">Tham gia</button>
            </form>
            <% } %>
            <% if (canLeave) { %>
            <form class="inline-form" method="post" action="${pageContext.request.contextPath}/lfg/leave">
                <input type="hidden" name="id" value="<%= post.getLfgId() %>">
                <button type="submit" class="btn btn-leave">Rời nhóm</button>
            </form>
            <% } %>
            <% if (canManage) { %>
            <a class="btn btn-edit" href="${pageContext.request.contextPath}/lfg/edit?id=<%= post.getLfgId() %>">Chỉnh sửa</a>
            <% if (LFGStatus.OPEN.equals(post.getStatus()) || LFGStatus.FULL.equals(post.getStatus())) { %>
            <form class="inline-form" method="post" action="${pageContext.request.contextPath}/lfg/close">
                <input type="hidden" name="id" value="<%= post.getLfgId() %>">
                <button type="submit" class="btn btn-close">Đóng nhóm</button>
            </form>
            <% } %>
            <form class="inline-form" method="post" action="${pageContext.request.contextPath}/lfg/delete"
                  onsubmit="return confirm('Bạn có chắc muốn xóa nhóm này?');">
                <input type="hidden" name="id" value="<%= post.getLfgId() %>">
                <button type="submit" class="btn btn-delete">Xóa nhóm</button>
            </form>
            <% } %>
            <% if (!isLoggedIn) { %>
            <span style="color: var(--text-secondary); font-size: 0.85rem; align-self: center;">Đăng nhập để tham gia nhóm.</span>
            <% } %>
        </div>

        <div class="info-card">
            <div class="info-row"><span>Người tạo</span><span><%= HtmlUtils.escape(post.getCreatorUsername()) %></span></div>
            <div class="info-row"><span>Thành viên</span><span><%= post.getCurrentPlayers() %>/<%= post.getMaxPlayers() %></span></div>
            <% if (post.getDescription() != null && !post.getDescription().isEmpty()) { %>
            <div class="lfg-desc"><%= HtmlUtils.escape(post.getDescription()) %></div>
            <% } %>
        </div>

        <div class="info-card">
            <h3 style="margin-top: 0; margin-bottom: 12px; font-size: 1rem;">Thành viên (<%= members == null ? 0 : members.size() %>)</h3>
            <ul class="members-list">
                <% if (members != null) { for (LFGMember m : members) {
                    String avatar = m.getAvatarUrl();
                    String displayName = m.getDisplayName() != null && !m.getDisplayName().isEmpty() ? m.getDisplayName() : m.getUsername();
                %>
                <li>
                    <% if (avatar != null && !avatar.isEmpty()) { %>
                    <img class="member-avatar" src="<%= HtmlUtils.escape(avatar) %>" alt="">
                    <% } else { %>
                    <span class="member-avatar"></span>
                    <% } %>
                    <span><%= HtmlUtils.escape(displayName) %></span>
                    <% if (m.getAccountId() == post.getAccountId()) { %>
                    <span style="color: var(--text-secondary); font-size: 0.78rem;">(người tạo)</span>
                    <% } %>
                </li>
                <% } } %>
            </ul>
        </div>

        <p><a href="${pageContext.request.contextPath}/lfg" style="color: var(--text-secondary);">&larr; Quay lại danh sách</a></p>
    <% } %>
</div>
</body>
</html>
