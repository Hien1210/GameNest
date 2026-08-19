<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.gamenest.model.Account" %>
<%@ page import="com.gamenest.model.AccountGame" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%
    // Public Profile — read-only view of another account's profile,
    // identified by username. Anonymous-accessible (see
    // PublicProfileServlet). Never shows email/password/role/status —
    // only the minimum field list this task specifies.
    Account account = (Account) request.getAttribute("account");
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= account != null ? HtmlUtils.escape(account.getUsername()) + " - " : "" %>Hồ sơ - GameNest</title>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined" rel="stylesheet" />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>

<div class="profile-container">
    <div class="profile-card">
        <h1 class="profile-title">Hồ sơ</h1>

        <% if (error != null) { %>
        <div class="alert alert-error">
            <span class="material-symbols-outlined alert-icon">error</span>
            <span><%= HtmlUtils.escape(error) %></span>
        </div>
        <% } %>

        <% if (account == null) { %>
        <p>Không tìm thấy người dùng.</p>
        <% } else {
            String label = (account.getDisplayName() != null && !account.getDisplayName().isEmpty())
                    ? account.getDisplayName() : account.getUsername();
            String initial = (label != null && !label.isEmpty()) ? label.substring(0, 1).toUpperCase() : "U";
            String createdAtDisplay = account.getCreatedAt() != null
                    ? account.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                    : "-";
        %>
        <div class="profile-header">
            <div class="profile-avatar-wrapper">
                <% if (account.getAvatarUrl() != null && !account.getAvatarUrl().isEmpty()) { %>
                <img class="profile-avatar-img" src="<%= HtmlUtils.escape(account.getAvatarUrl()) %>" alt="Avatar">
                <% } else { %>
                <span class="profile-avatar-circle" aria-hidden="true"><%= HtmlUtils.escape(initial) %></span>
                <% } %>
            </div>
            <div>
                <div class="profile-name" style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
                    <span><%= HtmlUtils.escape(label) %></span>
                    <%
                        Boolean presenceOnline = (Boolean) request.getAttribute("presenceOnline");
                        if (presenceOnline != null) {
                    %>
                    <span style="display: inline-flex; align-items: center; gap: 5px; font-size: 0.72rem; font-weight: 600; color: <%= presenceOnline ? "var(--accent-cyan)" : "var(--text-secondary)" %>;">
                        <span style="width: 8px; height: 8px; border-radius: 50%; display: inline-block; background: <%= presenceOnline ? "var(--accent-cyan)" : "var(--text-secondary)" %>;"></span><%= presenceOnline ? "Online" : "Offline" %>
                    </span>
                    <% } %>
                </div>
                <span class="profile-role-badge">@<%= HtmlUtils.escape(account.getUsername()) %></span>
            </div>
        </div>

        <%
            boolean isSelf = Boolean.TRUE.equals(request.getAttribute("isSelf"));
            boolean isFollowing = Boolean.TRUE.equals(request.getAttribute("isFollowing"));
            boolean isBlockedByViewer = Boolean.TRUE.equals(request.getAttribute("isBlockedByViewer"));
            boolean blockedEitherDirection = Boolean.TRUE.equals(request.getAttribute("blockedEitherDirection"));
            boolean viewerLoggedIn = session != null && session.getAttribute("accountId") != null;
            Integer followersCount = (Integer) request.getAttribute("followersCount");
            Integer followingCount = (Integer) request.getAttribute("followingCount");
            String encodedUsername = HtmlUtils.escape(account.getUsername());
        %>
        <div style="display: flex; align-items: center; gap: 16px; margin: 14px 0; flex-wrap: wrap;">
            <% if (isSelf) { %>
            <a href="${pageContext.request.contextPath}/account/profile"
               style="text-decoration: none; padding: 9px 18px; border-radius: 8px; background: linear-gradient(135deg, var(--accent-purple), var(--accent-cyan)); color: #fff; font-weight: 600;">Chỉnh sửa Profile</a>
            <% } else if (viewerLoggedIn && !blockedEitherDirection) { %>
                <% if (isFollowing) { %>
                <form method="post" action="${pageContext.request.contextPath}/account/unfollow" style="margin: 0;">
                    <input type="hidden" name="username" value="<%= encodedUsername %>">
                    <button type="submit" style="padding: 9px 18px; border-radius: 8px; border: 1px solid var(--border-color); background: transparent; color: var(--text-primary); font-weight: 600; cursor: pointer;">Following</button>
                </form>
                <% } else { %>
                <form method="post" action="${pageContext.request.contextPath}/account/follow" style="margin: 0;">
                    <input type="hidden" name="username" value="<%= encodedUsername %>">
                    <button type="submit" style="padding: 9px 18px; border-radius: 8px; border: none; background: linear-gradient(135deg, var(--accent-purple), var(--accent-cyan)); color: #fff; font-weight: 600; cursor: pointer;">Follow</button>
                </form>
                <% } %>
            <% } %>

            <a href="${pageContext.request.contextPath}/account/followers?username=<%= encodedUsername %>" style="color: var(--text-secondary); text-decoration: none; font-size: 0.9rem;">
                <strong style="color: var(--text-primary);"><%= followersCount != null ? followersCount : 0 %></strong> Followers
            </a>
            <a href="${pageContext.request.contextPath}/account/following?username=<%= encodedUsername %>" style="color: var(--text-secondary); text-decoration: none; font-size: 0.9rem;">
                <strong style="color: var(--text-primary);"><%= followingCount != null ? followingCount : 0 %></strong> Following
            </a>
        </div>

        <%
            boolean isFriend = Boolean.TRUE.equals(request.getAttribute("isFriend"));
            boolean friendOutgoingPending = Boolean.TRUE.equals(request.getAttribute("friendOutgoingPending"));
            boolean friendIncomingPending = Boolean.TRUE.equals(request.getAttribute("friendIncomingPending"));
            Integer friendshipId = (Integer) request.getAttribute("friendshipId");
        %>
        <% if (!isSelf && viewerLoggedIn && !blockedEitherDirection) { %>
        <div style="display: flex; align-items: center; gap: 10px; margin: 0 0 14px 0; flex-wrap: wrap;">
            <% if (isFriend) { %>
            <span style="padding: 8px 16px; border-radius: 8px; background: rgba(16,185,129,0.12); color: var(--success-color); font-weight: 600; font-size: 0.88rem;">Bạn bè</span>
            <form method="post" action="${pageContext.request.contextPath}/account/friend/unfriend" style="margin: 0;">
                <input type="hidden" name="friendshipId" value="<%= friendshipId %>">
                <input type="hidden" name="username" value="<%= encodedUsername %>">
                <button type="submit" style="padding: 8px 16px; border-radius: 8px; border: 1px solid var(--border-color); background: transparent; color: var(--text-primary); font-weight: 600; cursor: pointer;">Hủy kết bạn</button>
            </form>
            <% } else if (friendOutgoingPending) { %>
            <span style="padding: 8px 16px; border-radius: 8px; background: rgba(255,255,255,0.06); color: var(--text-secondary); font-weight: 600; font-size: 0.88rem;">Đã gửi lời mời</span>
            <form method="post" action="${pageContext.request.contextPath}/account/friend/cancel" style="margin: 0;">
                <input type="hidden" name="friendshipId" value="<%= friendshipId %>">
                <input type="hidden" name="username" value="<%= encodedUsername %>">
                <button type="submit" style="padding: 8px 16px; border-radius: 8px; border: 1px solid var(--border-color); background: transparent; color: var(--text-primary); font-weight: 600; cursor: pointer;">Hủy lời mời</button>
            </form>
            <% } else if (friendIncomingPending) { %>
            <form method="post" action="${pageContext.request.contextPath}/account/friend/accept" style="margin: 0;">
                <input type="hidden" name="friendshipId" value="<%= friendshipId %>">
                <input type="hidden" name="username" value="<%= encodedUsername %>">
                <button type="submit" style="padding: 8px 16px; border-radius: 8px; border: none; background: linear-gradient(135deg, var(--accent-purple), var(--accent-cyan)); color: #fff; font-weight: 600; cursor: pointer;">Chấp nhận</button>
            </form>
            <form method="post" action="${pageContext.request.contextPath}/account/friend/reject" style="margin: 0;">
                <input type="hidden" name="friendshipId" value="<%= friendshipId %>">
                <input type="hidden" name="username" value="<%= encodedUsername %>">
                <button type="submit" style="padding: 8px 16px; border-radius: 8px; border: 1px solid var(--border-color); background: transparent; color: var(--text-primary); font-weight: 600; cursor: pointer;">Từ chối</button>
            </form>
            <% } else { %>
            <form method="post" action="${pageContext.request.contextPath}/account/friend/request" style="margin: 0;">
                <input type="hidden" name="username" value="<%= encodedUsername %>">
                <button type="submit" style="padding: 8px 16px; border-radius: 8px; border: 1px solid var(--border-color); background: transparent; color: var(--text-primary); font-weight: 600; cursor: pointer;">Kết bạn</button>
            </form>
            <% } %>
        </div>
        <% } %>

        <% if (!isSelf && viewerLoggedIn) { %>
        <div style="display: flex; align-items: center; gap: 10px; margin: 0 0 14px 0; flex-wrap: wrap;">
            <% if (isBlockedByViewer) { %>
            <span style="padding: 8px 16px; border-radius: 8px; background: rgba(239,68,68,0.12); color: var(--error-color); font-weight: 600; font-size: 0.88rem;">Đã chặn</span>
            <form method="post" action="${pageContext.request.contextPath}/account/unblock" style="margin: 0;">
                <input type="hidden" name="username" value="<%= encodedUsername %>">
                <input type="hidden" name="returnTo" value="profile">
                <button type="submit" style="padding: 8px 16px; border-radius: 8px; border: 1px solid var(--border-color); background: transparent; color: var(--text-primary); font-weight: 600; cursor: pointer;">Bỏ chặn</button>
            </form>
            <% } else { %>
            <form method="post" action="${pageContext.request.contextPath}/account/block"
                  onsubmit="return confirm('Chặn người dùng này? Follow và bạn bè giữa hai bên (nếu có) sẽ bị hủy.');" style="margin: 0;">
                <input type="hidden" name="username" value="<%= encodedUsername %>">
                <button type="submit" style="padding: 8px 16px; border-radius: 8px; border: 1px solid var(--border-color); background: transparent; color: var(--text-secondary); font-weight: 600; cursor: pointer; font-size: 0.85rem;">Chặn</button>
            </form>
            <% } %>
        </div>
        <% } %>

        <dl class="profile-info">
            <dt>Ngày tham gia</dt><dd><%= createdAtDisplay %></dd>
        </dl>

        <%
            @SuppressWarnings("unchecked")
            List<AccountGame> playingGames = (List<AccountGame>) request.getAttribute("playingGames");
            @SuppressWarnings("unchecked")
            List<AccountGame> favoriteGames = (List<AccountGame>) request.getAttribute("favoriteGames");
        %>
        <div class="profile-games-section" style="margin-top: 20px;">
            <h2 class="profile-games-heading">Đang chơi</h2>
            <% if (playingGames == null || playingGames.isEmpty()) { %>
            <p class="profile-games-empty">Chưa có game nào.</p>
            <% } else { %>
            <ul class="profile-games-list">
                <% for (AccountGame ag : playingGames) { %>
                <li class="profile-game-chip"><span><%= HtmlUtils.escape(ag.getGameName()) %></span></li>
                <% } %>
            </ul>
            <% } %>
        </div>

        <div class="profile-games-section">
            <h2 class="profile-games-heading">Yêu thích</h2>
            <% if (favoriteGames == null || favoriteGames.isEmpty()) { %>
            <p class="profile-games-empty">Chưa có game nào.</p>
            <% } else { %>
            <ul class="profile-games-list">
                <% for (AccountGame ag : favoriteGames) { %>
                <li class="profile-game-chip"><span><%= HtmlUtils.escape(ag.getGameName()) %></span></li>
                <% } %>
            </ul>
            <% } %>
        </div>
        <% } %>
    </div>
</div>

</body>
</html>
