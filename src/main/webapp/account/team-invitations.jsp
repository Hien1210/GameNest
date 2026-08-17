<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%@ page import="com.gamenest.model.TeamInvitation" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Lời mời vào nhóm - GameNest</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        body { display: block; padding: 30px 20px; }
        .page-wrap { max-width: 600px; margin: 0 auto; }
        .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 10px; }
        .tab-link { padding: 8px 16px; border-radius: 8px; border: 1px solid var(--border-color); color: var(--text-secondary); text-decoration: none; font-size: 0.88rem; }
        .invite-card { display: flex; align-items: center; gap: 12px; background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 10px; padding: 12px 16px; margin-bottom: 10px; }
        .invite-info { flex: 1; min-width: 0; }
        .invite-team-name { font-weight: 600; font-size: 0.94rem; }
        .invite-inviter { color: var(--text-secondary); font-size: 0.82rem; }
        .invite-time { color: var(--text-secondary); font-size: 0.76rem; }
        .invite-actions { display: flex; gap: 8px; flex-shrink: 0; }
        .inline-form { margin: 0; }
        .btn-accept { padding: 8px 14px; border-radius: 8px; border: none; background: linear-gradient(135deg, var(--accent-purple), var(--accent-cyan)); color: #fff; font-weight: 600; cursor: pointer; font-size: 0.82rem; }
        .btn-reject { padding: 8px 14px; border-radius: 8px; border: 1px solid var(--border-color); background: transparent; color: var(--text-primary); cursor: pointer; font-size: 0.82rem; }
        .empty { color: var(--text-secondary); margin-top: 20px; }
        .error-msg { color: var(--error-color); margin-bottom: 16px; }
        .pagination { margin-top: 24px; display: flex; gap: 8px; }
        .pagination a { padding: 8px 12px; border-radius: 6px; border: 1px solid var(--border-color); color: var(--text-primary); text-decoration: none; }
    </style>
</head>
<body>
<div class="page-wrap">
    <div class="page-header">
        <h1>Lời mời vào nhóm</h1>
        <a class="tab-link" href="${pageContext.request.contextPath}/account/teams">Nhóm của tôi</a>
    </div>

    <% if (request.getAttribute("error") != null) { %>
        <p class="error-msg"><%= HtmlUtils.escape((String) request.getAttribute("error")) %></p>
    <% } %>

    <%
        @SuppressWarnings("unchecked")
        List<TeamInvitation> invitations = (List<TeamInvitation>) request.getAttribute("invitations");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    %>
    <% if (invitations == null || invitations.isEmpty()) { %>
        <p class="empty">Không có lời mời nào.</p>
    <% } else { %>
        <% for (TeamInvitation inv : invitations) {
            String inviterLabel = inv.getInviterDisplayName() != null && !inv.getInviterDisplayName().isEmpty() ? inv.getInviterDisplayName() : inv.getInviterUsername();
        %>
        <div class="invite-card">
            <div class="invite-info">
                <div class="invite-team-name"><%= HtmlUtils.escape(inv.getTeamName()) %></div>
                <div class="invite-inviter">Mời bởi <%= HtmlUtils.escape(inviterLabel) %></div>
                <div class="invite-time"><%= inv.getCreatedAt() != null ? inv.getCreatedAt().format(fmt) : "" %></div>
            </div>
            <div class="invite-actions">
                <form class="inline-form" method="post" action="${pageContext.request.contextPath}/account/team/invite/accept">
                    <input type="hidden" name="invitationId" value="<%= inv.getInvitationId() %>">
                    <button type="submit" class="btn-accept">Chấp nhận</button>
                </form>
                <form class="inline-form" method="post" action="${pageContext.request.contextPath}/account/team/invite/reject">
                    <input type="hidden" name="invitationId" value="<%= inv.getInvitationId() %>">
                    <button type="submit" class="btn-reject">Từ chối</button>
                </form>
            </div>
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
                <a href="${pageContext.request.contextPath}/account/teams/invitations?page=<%= p %>"
                   style="<%= p == currentPage ? "border-color: var(--accent-purple);" : "" %>"><%= p %></a>
            <% } %>
        </div>
    <% } %>

    <p style="margin-top: 30px;"><a href="${pageContext.request.contextPath}/account/home.jsp" style="color: var(--text-secondary);">&larr; Về trang chủ</a></p>
</div>
</body>
</html>
