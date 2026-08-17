<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%@ page import="com.gamenest.model.Team" %>
<%@ page import="com.gamenest.model.TeamInvitation" %>
<%@ page import="com.gamenest.model.TeamMember" %>
<%@ page import="com.gamenest.model.TeamMemberRole" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <%
        Team team = (Team) request.getAttribute("team");
        String pageTitle = team == null ? "Nhóm" : team.getName();
    %>
    <title><%= HtmlUtils.escape(pageTitle) %> - GameNest</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        body { display: block; padding: 30px 20px; }
        .page-wrap { max-width: 760px; margin: 0 auto; }
        .team-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; margin-bottom: 6px; flex-wrap: wrap; }
        .team-title { font-size: 1.5rem; font-weight: 700; }
        .team-owner-label { color: var(--accent-cyan); font-size: 0.9rem; margin-bottom: 16px; }
        .info-card { background: var(--bg-secondary); border: 1px solid var(--border-color); border-radius: 12px; padding: 20px; margin-bottom: 18px; }
        .info-card h3 { margin-top: 0; margin-bottom: 14px; font-size: 1rem; }
        .team-desc { white-space: pre-wrap; line-height: 1.5; color: var(--text-secondary); }
        .member-row { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px solid var(--border-color); }
        .member-row:last-child { border-bottom: none; }
        .member-avatar-img, .member-avatar-circle { width: 34px; height: 34px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
        .member-avatar-circle { display: flex; align-items: center; justify-content: center; background: var(--accent-purple); color: #fff; font-weight: 600; font-size: 0.85rem; }
        .member-info { flex: 1; min-width: 0; }
        .member-name { font-size: 0.9rem; font-weight: 600; }
        .role-badge { display: inline-block; padding: 2px 8px; border-radius: 999px; font-size: 0.7rem; font-weight: 600; margin-left: 6px; }
        .role-owner { background: rgba(139,92,246,0.15); color: var(--accent-purple); }
        .role-member { background: rgba(255,255,255,0.06); color: var(--text-secondary); }
        .member-actions { display: flex; gap: 6px; }
        .inline-form { margin: 0; }
        .btn-sm { padding: 5px 10px; border-radius: 6px; border: 1px solid var(--border-color); background: transparent; color: var(--text-secondary); cursor: pointer; font-size: 0.74rem; white-space: nowrap; }
        .actions { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 18px; }
        .btn { padding: 10px 18px; border-radius: 8px; border: none; cursor: pointer; font-size: 0.88rem; font-weight: 600; text-decoration: none; display: inline-block; }
        .btn-primary { background: var(--accent-purple); color: #fff; }
        .btn-danger { background: rgba(239,68,68,0.15); color: var(--error-color); }
        .btn-secondary { background: var(--bg-secondary); border: 1px solid var(--border-color); color: var(--text-primary); }
        .invite-form { display: flex; gap: 8px; }
        .invite-form input { flex: 1; padding: 9px 12px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-primary); color: var(--text-primary); font-family: inherit; }
        .error-msg { color: var(--error-color); margin-bottom: 16px; }
        .empty { color: var(--text-secondary); font-size: 0.85rem; }
    </style>
</head>
<body>
<div class="page-wrap">
    <% if (request.getAttribute("error") != null) { %>
        <p class="error-msg"><%= HtmlUtils.escape((String) request.getAttribute("error")) %></p>
    <% } %>

    <% if (team == null) { %>
        <p>Nhóm không tồn tại.</p>
        <p><a href="${pageContext.request.contextPath}/account/teams" style="color: var(--text-secondary);">&larr; Quay lại danh sách</a></p>
    <% } else {
        boolean isOwner = Boolean.TRUE.equals(request.getAttribute("isOwner"));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
        @SuppressWarnings("unchecked")
        List<TeamMember> members = (List<TeamMember>) request.getAttribute("members");
    %>
        <div class="team-header">
            <span class="team-title"><%= HtmlUtils.escape(team.getName()) %></span>
        </div>
        <div class="team-owner-label">Chủ nhóm: <%= HtmlUtils.escape(team.getOwnerDisplayName() != null && !team.getOwnerDisplayName().isEmpty() ? team.getOwnerDisplayName() : team.getOwnerUsername()) %></div>

        <div class="actions">
            <a class="btn btn-primary" href="${pageContext.request.contextPath}/account/chat/team?id=<%= team.getTeamId() %>">Mở Chat nhóm</a>
            <% if (isOwner) { %>
            <form class="inline-form" method="post" action="${pageContext.request.contextPath}/account/team/delete"
                  onsubmit="return confirm('Xóa nhóm này? Hành động không thể hoàn tác.');">
                <input type="hidden" name="teamId" value="<%= team.getTeamId() %>">
                <button type="submit" class="btn btn-danger">Xóa nhóm</button>
            </form>
            <% } else { %>
            <form class="inline-form" method="post" action="${pageContext.request.contextPath}/account/team/leave"
                  onsubmit="return confirm('Rời khỏi nhóm này?');">
                <input type="hidden" name="teamId" value="<%= team.getTeamId() %>">
                <button type="submit" class="btn btn-secondary">Rời nhóm</button>
            </form>
            <% } %>
        </div>

        <% if (team.getDescription() != null && !team.getDescription().isEmpty()) { %>
        <div class="info-card">
            <div class="team-desc"><%= HtmlUtils.escape(team.getDescription()) %></div>
        </div>
        <% } %>

        <% if (isOwner) { %>
        <div class="info-card">
            <h3>Mời bạn bè</h3>
            <form class="invite-form" method="post" action="${pageContext.request.contextPath}/account/team/invite">
                <input type="hidden" name="teamId" value="<%= team.getTeamId() %>">
                <input type="text" name="username" placeholder="Nhập username của bạn bè..." required>
                <button type="submit" class="btn btn-primary" style="padding: 9px 16px;">Mời</button>
            </form>
            <p style="color: var(--text-secondary); font-size: 0.78rem; margin-top: 8px; margin-bottom: 0;">Chỉ có thể mời người đã là bạn bè (Friend) và không đang bị chặn.</p>
        </div>

        <%
            @SuppressWarnings("unchecked")
            List<TeamInvitation> pendingInvitations = (List<TeamInvitation>) request.getAttribute("pendingInvitations");
        %>
        <div class="info-card">
            <h3>Lời mời đang chờ (<%= pendingInvitations == null ? 0 : pendingInvitations.size() %>)</h3>
            <% if (pendingInvitations == null || pendingInvitations.isEmpty()) { %>
            <p class="empty">Không có lời mời nào đang chờ.</p>
            <% } else { %>
            <% for (TeamInvitation inv : pendingInvitations) {
                String inviteeLabel = inv.getInviteeDisplayName() != null && !inv.getInviteeDisplayName().isEmpty() ? inv.getInviteeDisplayName() : inv.getInviteeUsername();
            %>
            <div class="member-row">
                <div class="member-info">
                    <span class="member-name"><%= HtmlUtils.escape(inviteeLabel) %></span>
                    <span style="color: var(--text-secondary); font-size: 0.78rem;"> — <%= inv.getCreatedAt() != null ? inv.getCreatedAt().format(fmt) : "" %></span>
                </div>
                <form class="inline-form" method="post" action="${pageContext.request.contextPath}/account/team/invite/cancel">
                    <input type="hidden" name="invitationId" value="<%= inv.getInvitationId() %>">
                    <input type="hidden" name="teamId" value="<%= team.getTeamId() %>">
                    <button type="submit" class="btn-sm">Hủy lời mời</button>
                </form>
            </div>
            <% } %>
            <% } %>
        </div>
        <% } %>

        <div class="info-card">
            <h3>Thành viên (<%= members == null ? 0 : members.size() %>)</h3>
            <% if (members != null) { for (TeamMember m : members) {
                String memberLabel = m.getDisplayName() != null && !m.getDisplayName().isEmpty() ? m.getDisplayName() : m.getUsername();
                String initial = memberLabel != null && !memberLabel.isEmpty() ? memberLabel.substring(0, 1).toUpperCase() : "U";
                boolean rowIsOwner = TeamMemberRole.OWNER.equals(m.getRole());
            %>
            <div class="member-row">
                <% if (m.getAvatarUrl() != null && !m.getAvatarUrl().isEmpty()) { %>
                <img class="member-avatar-img" src="<%= HtmlUtils.escape(m.getAvatarUrl()) %>" alt="">
                <% } else { %>
                <span class="member-avatar-circle"><%= initial %></span>
                <% } %>
                <div class="member-info">
                    <span class="member-name"><%= HtmlUtils.escape(memberLabel) %></span>
                    <span class="role-badge <%= rowIsOwner ? "role-owner" : "role-member" %>"><%= rowIsOwner ? "Chủ nhóm" : "Thành viên" %></span>
                    <div style="color: var(--text-secondary); font-size: 0.76rem;">Tham gia <%= m.getJoinedAt() != null ? m.getJoinedAt().format(fmt) : "" %></div>
                </div>
                <% if (isOwner && !rowIsOwner) { %>
                <div class="member-actions">
                    <form class="inline-form" method="post" action="${pageContext.request.contextPath}/account/team/transfer-owner"
                          onsubmit="return confirm('Chuyển quyền chủ nhóm cho người này?');">
                        <input type="hidden" name="teamId" value="<%= team.getTeamId() %>">
                        <input type="hidden" name="targetAccountId" value="<%= m.getAccountId() %>">
                        <button type="submit" class="btn-sm">Chuyển quyền</button>
                    </form>
                    <form class="inline-form" method="post" action="${pageContext.request.contextPath}/account/team/remove-member"
                          onsubmit="return confirm('Loại thành viên này khỏi nhóm?');">
                        <input type="hidden" name="teamId" value="<%= team.getTeamId() %>">
                        <input type="hidden" name="targetAccountId" value="<%= m.getAccountId() %>">
                        <button type="submit" class="btn-sm">Loại khỏi nhóm</button>
                    </form>
                </div>
                <% } %>
            </div>
            <% } } %>
        </div>

        <p><a href="${pageContext.request.contextPath}/account/teams" style="color: var(--text-secondary);">&larr; Quay lại danh sách</a></p>
    <% } %>
</div>
</body>
</html>
