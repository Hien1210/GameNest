<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<%@ page import="com.gamenest.service.NotificationService" %>
<%@ page import="com.gamenest.service.AccountFriendService" %>
<%@ page import="com.gamenest.service.TeamService" %>
<%@ page import="com.gamenest.service.ChatService" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="java.util.logging.Level" %>
<%@ page import="java.util.logging.Logger" %>
<%
    String displayName = (String) session.getAttribute("displayName");
    String username = (String) session.getAttribute("username");
    String role = (String) session.getAttribute("role");
    String avatarUrl = (String) session.getAttribute("avatarUrl");

    String nameToShow = displayName != null && !displayName.isEmpty() ? displayName : (username != null ? username : "Game thủ");
    String avatarInitial = nameToShow.substring(0, 1).toUpperCase();
    boolean isAdmin = "ADMIN".equals(role);
    boolean isModerator = "MODERATOR".equals(role);

    int unreadNotificationCount = 0;
    int incomingFriendRequestCount = 0;
    int incomingTeamInvitationCount = 0;
    int unreadChatCount = 0;
    Object accountIdAttr = session.getAttribute("accountId");
    if (accountIdAttr instanceof Integer) {
        try {
            unreadNotificationCount = new NotificationService().countUnread((Integer) accountIdAttr);
        } catch (SQLException e) {
            Logger.getLogger("home.jsp").log(Level.WARNING, "Failed to load unread notification count", e);
        }
        try {
            incomingFriendRequestCount = new AccountFriendService().countIncomingRequests((Integer) accountIdAttr);
        } catch (SQLException e) {
            Logger.getLogger("home.jsp").log(Level.WARNING, "Failed to load incoming friend request count", e);
        }
        try {
            incomingTeamInvitationCount = new TeamService().countIncomingInvitations((Integer) accountIdAttr);
        } catch (SQLException e) {
            Logger.getLogger("home.jsp").log(Level.WARNING, "Failed to load incoming team invitation count", e);
        }
        try {
            unreadChatCount = new ChatService().countUnreadConversations((Integer) accountIdAttr);
        } catch (SQLException e) {
            Logger.getLogger("home.jsp").log(Level.WARNING, "Failed to load unread chat count", e);
        }
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Trang chủ Game thủ - GameNest</title>
    <!-- Google Fonts & Material Symbols -->
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined" rel="stylesheet" />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        :root {
            --bg-primary: #0a0a0f;
            --bg-secondary: rgba(18, 18, 29, 0.75);
            --bg-card: rgba(26, 26, 42, 0.6);
            --bg-card-hover: rgba(36, 36, 58, 0.8);
            --accent-purple: #8b5cf6;
            --accent-cyan: #06b6d4;
            --text-primary: #f3f4f6;
            --text-secondary: #9ca3af;
            --border-color: rgba(255, 255, 255, 0.08);
            --glass-shadow: 0 12px 36px 0 rgba(0, 0, 0, 0.4);
        }

        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Be Vietnam Pro', sans-serif;
            background-color: var(--bg-primary);
            color: var(--text-primary);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            overflow-x: hidden;
            line-height: 1.6;
        }

        /* Ambient Background */
        .ambient-bg {
            position: fixed;
            top: 0;
            left: 0;
            width: 100vw;
            height: 100vh;
            z-index: -1;
            overflow: hidden;
            background: 
                radial-gradient(circle at 10% 20%, rgba(139, 92, 246, 0.15), transparent 40%),
                radial-gradient(circle at 90% 80%, rgba(6, 182, 212, 0.12), transparent 40%),
                radial-gradient(circle at 50% 50%, rgba(10, 10, 15, 0.95), var(--bg-primary));
        }

        /* Navbar */
        .navbar {
            position: sticky;
            top: 0;
            width: 100%;
            padding: 14px 5%;
            display: flex;
            justify-content: space-between;
            align-items: center;
            background: rgba(10, 10, 15, 0.85);
            backdrop-filter: blur(16px);
            -webkit-backdrop-filter: blur(16px);
            border-bottom: 1px solid var(--border-color);
            z-index: 100;
        }

        .logo {
            font-size: 1.6rem;
            font-weight: 800;
            background: linear-gradient(135deg, var(--accent-cyan), var(--accent-purple));
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            text-decoration: none;
            letter-spacing: 0.5px;
        }

        .nav-links {
            display: flex;
            gap: 28px;
            align-items: center;
        }

        .nav-links a {
            color: var(--text-secondary);
            text-decoration: none;
            font-weight: 500;
            font-size: 0.95rem;
            transition: color 0.2s ease;
            display: flex;
            align-items: center;
            gap: 6px;
        }

        .nav-links a:hover,
        .nav-links a.active {
            color: var(--text-primary);
        }

        .nav-links .material-symbols-outlined {
            font-size: 20px;
        }

        /* User Menu & Dropdown */
        .user-menu-wrap {
            position: relative;
        }

        .user-menu-btn {
            display: flex;
            align-items: center;
            gap: 10px;
            background: rgba(255, 255, 255, 0.05);
            border: 1px solid var(--border-color);
            border-radius: 999px;
            padding: 4px 12px 4px 4px;
            color: var(--text-primary);
            cursor: pointer;
            transition: all 0.2s ease;
        }

        .user-menu-btn:hover {
            background: rgba(255, 255, 255, 0.1);
            border-color: rgba(255, 255, 255, 0.2);
        }

        .user-avatar-circle,
        .user-avatar-img {
            width: 34px;
            height: 34px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 700;
            font-size: 0.95rem;
            color: #fff;
            object-fit: cover;
        }

        .user-avatar-circle {
            background: linear-gradient(135deg, var(--accent-purple), var(--accent-cyan));
        }

        .user-name {
            font-size: 0.92rem;
            font-weight: 600;
            max-width: 140px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .user-caret {
            font-size: 18px;
            color: var(--text-secondary);
            transition: transform 0.2s ease;
        }

        .user-menu-wrap.open .user-caret {
            transform: rotate(180deg);
        }

        .user-dropdown {
            position: absolute;
            top: calc(100% + 10px);
            right: 0;
            width: 240px;
            background: #141422;
            border: 1px solid var(--border-color);
            border-radius: 14px;
            padding: 8px;
            box-shadow: 0 16px 36px rgba(0, 0, 0, 0.5);
            display: none;
            flex-direction: column;
            gap: 4px;
            z-index: 110;
            animation: dropdownFade 0.2s cubic-bezier(0.16, 1, 0.3, 1) forwards;
        }

        .user-menu-wrap.open .user-dropdown {
            display: flex;
        }

        @keyframes dropdownFade {
            from { opacity: 0; transform: translateY(-8px); }
            to { opacity: 1; transform: translateY(0); }
        }

        .dropdown-header {
            padding: 10px 12px;
            border-bottom: 1px solid var(--border-color);
            margin-bottom: 4px;
        }

        .dropdown-header .dh-name {
            font-weight: 600;
            font-size: 0.95rem;
            color: var(--text-primary);
        }

        .dropdown-header .dh-role {
            font-size: 0.72rem;
            font-weight: 600;
            padding: 2px 8px;
            border-radius: 999px;
            background: rgba(139, 92, 246, 0.15);
            color: var(--accent-purple);
            border: 1px solid rgba(139, 92, 246, 0.3);
            display: inline-block;
            margin-top: 4px;
        }

        .dropdown-item {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 9px 12px;
            color: var(--text-secondary);
            text-decoration: none;
            font-size: 0.88rem;
            border-radius: 8px;
            transition: all 0.2s ease;
            background: transparent;
            border: none;
            width: 100%;
            text-align: left;
            cursor: pointer;
            font-family: inherit;
        }

        .dropdown-item:hover {
            background: rgba(255, 255, 255, 0.06);
            color: var(--text-primary);
        }

        .dropdown-item .material-symbols-outlined {
            font-size: 19px;
            color: var(--text-secondary);
        }

        .dropdown-item.logout {
            color: #ef4444;
        }

        .dropdown-item.logout .material-symbols-outlined {
            color: #ef4444;
        }

        .dropdown-divider {
            height: 1px;
            background: var(--border-color);
            margin: 4px 0;
        }

        /* Main Container */
        .home-container {
            max-width: 1160px;
            margin: 0 auto;
            padding: 36px 20px 60px;
            flex: 1;
            width: 100%;
        }

        /* Welcome Banner */
        .welcome-banner {
            background: linear-gradient(135deg, rgba(139, 92, 246, 0.18) 0%, rgba(6, 182, 212, 0.12) 100%), var(--bg-card);
            border: 1px solid rgba(139, 92, 246, 0.3);
            border-radius: 20px;
            padding: 32px 36px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 24px;
            box-shadow: var(--glass-shadow);
            margin-bottom: 36px;
            backdrop-filter: blur(12px);
            -webkit-backdrop-filter: blur(12px);
            position: relative;
            overflow: hidden;
        }

        .welcome-banner::after {
            content: '';
            position: absolute;
            right: -60px;
            top: -60px;
            width: 220px;
            height: 220px;
            background: radial-gradient(circle, rgba(139, 92, 246, 0.25) 0%, transparent 70%);
            pointer-events: none;
        }

        .welcome-text h1 {
            font-size: 1.85rem;
            font-weight: 800;
            margin-bottom: 8px;
            color: var(--text-primary);
        }

        .welcome-text h1 span {
            background: linear-gradient(135deg, var(--accent-cyan), var(--accent-purple));
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }

        .welcome-text p {
            color: var(--text-secondary);
            font-size: 0.98rem;
            max-width: 580px;
            margin-bottom: 20px;
        }

        .welcome-actions {
            display: flex;
            gap: 12px;
            flex-wrap: wrap;
        }

        .btn-action-primary {
            background: linear-gradient(135deg, var(--accent-purple), var(--accent-cyan));
            color: #fff;
            padding: 10px 22px;
            border-radius: 10px;
            font-weight: 600;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            font-size: 0.92rem;
            box-shadow: 0 4px 16px rgba(139, 92, 246, 0.35);
            transition: all 0.25s ease;
        }

        .btn-action-primary:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 22px rgba(139, 92, 246, 0.55);
        }

        .btn-action-secondary {
            background: rgba(255, 255, 255, 0.07);
            color: var(--text-primary);
            padding: 10px 20px;
            border-radius: 10px;
            font-weight: 500;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            font-size: 0.92rem;
            border: 1px solid var(--border-color);
            transition: all 0.25s ease;
        }

        .btn-action-secondary:hover {
            background: rgba(255, 255, 255, 0.12);
            border-color: rgba(255, 255, 255, 0.2);
        }

        .user-quick-profile {
            display: flex;
            align-items: center;
            gap: 18px;
            background: rgba(10, 10, 15, 0.5);
            padding: 16px 20px;
            border-radius: 16px;
            border: 1px solid var(--border-color);
            flex-shrink: 0;
        }

        .uqp-avatar {
            width: 58px;
            height: 58px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.4rem;
            font-weight: 700;
            color: #fff;
            background: linear-gradient(135deg, var(--accent-purple), var(--accent-cyan));
            object-fit: cover;
            box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
        }

        .uqp-info {
            display: flex;
            flex-direction: column;
            gap: 4px;
        }

        .uqp-name {
            font-size: 1.05rem;
            font-weight: 700;
            color: var(--text-primary);
        }

        .uqp-tag {
            font-size: 0.78rem;
            color: var(--text-secondary);
        }

        .uqp-badge {
            align-self: flex-start;
            margin-top: 2px;
            font-size: 0.7rem;
            font-weight: 600;
            padding: 2px 8px;
            border-radius: 999px;
            background: rgba(6, 182, 212, 0.15);
            color: var(--accent-cyan);
            border: 1px solid rgba(6, 182, 212, 0.3);
        }

        /* Section Title */
        .section-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20px;
        }

        .section-title {
            font-size: 1.3rem;
            font-weight: 700;
            display: flex;
            align-items: center;
            gap: 10px;
            color: var(--text-primary);
        }

        .section-title .material-symbols-outlined {
            color: var(--accent-cyan);
            font-size: 24px;
        }

        /* Feature Cards Grid */
        .cards-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
            gap: 20px;
            margin-bottom: 40px;
        }

        .feature-box {
            background: var(--bg-card);
            border: 1px solid var(--border-color);
            border-radius: 16px;
            padding: 24px;
            text-decoration: none;
            color: var(--text-primary);
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
            position: relative;
            overflow: hidden;
            backdrop-filter: blur(10px);
            -webkit-backdrop-filter: blur(10px);
        }

        .feature-box:hover {
            transform: translateY(-4px);
            border-color: rgba(139, 92, 246, 0.4);
            box-shadow: 0 12px 30px rgba(0, 0, 0, 0.45);
            background: var(--bg-card-hover);
        }

        .feature-icon-wrap {
            width: 48px;
            height: 48px;
            border-radius: 12px;
            background: rgba(139, 92, 246, 0.12);
            border: 1px solid rgba(139, 92, 246, 0.25);
            display: flex;
            align-items: center;
            justify-content: center;
            margin-bottom: 16px;
            color: var(--accent-purple);
            transition: all 0.3s ease;
        }

        .feature-box:hover .feature-icon-wrap {
            background: linear-gradient(135deg, var(--accent-purple), var(--accent-cyan));
            color: #fff;
            border-color: transparent;
            transform: scale(1.05);
        }

        .feature-box h3 {
            font-size: 1.1rem;
            font-weight: 700;
            margin-bottom: 8px;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .feature-box p {
            color: var(--text-secondary);
            font-size: 0.88rem;
            line-height: 1.5;
            margin-bottom: 18px;
            flex-grow: 1;
        }

        .feature-footer {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 0.84rem;
            font-weight: 600;
            color: var(--accent-cyan);
        }

        .feature-footer .material-symbols-outlined {
            font-size: 18px;
            transition: transform 0.2s ease;
        }

        .feature-box:hover .feature-footer .material-symbols-outlined {
            transform: translateX(4px);
        }

        /* Footer */
        .footer {
            border-top: 1px solid var(--border-color);
            padding: 24px 5%;
            text-align: center;
            color: var(--text-secondary);
            font-size: 0.85rem;
            background: rgba(10, 10, 15, 0.9);
            margin-top: auto;
        }

        /* Responsive */
        @media (max-width: 860px) {
            .welcome-banner {
                flex-direction: column;
                align-items: flex-start;
                padding: 24px;
            }
            .user-quick-profile {
                width: 100%;
            }
            .nav-links {
                display: none;
            }
        }
    </style>
</head>
<body>

    <div class="ambient-bg"></div>

    <!-- Navigation Header -->
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/account/home.jsp" class="logo">GameNest</a>

        <div class="nav-links">
            <a href="${pageContext.request.contextPath}/account/home.jsp" class="active">
                <span class="material-symbols-outlined">home</span>
                <span>Trang chủ</span>
            </a>
            <a href="${pageContext.request.contextPath}/games">
                <span class="material-symbols-outlined">sports_esports</span>
                <span>Trò chơi</span>
            </a>
            <a href="${pageContext.request.contextPath}/questions">
                <span class="material-symbols-outlined">quiz</span>
                <span>Hỏi đáp</span>
            </a>
            <a href="${pageContext.request.contextPath}/account/profile">
                <span class="material-symbols-outlined">person</span>
                <span>Hồ sơ</span>
            </a>
        </div>

        <a href="${pageContext.request.contextPath}/account/notifications"
           style="position: relative; display: inline-flex; align-items: center; justify-content: center; width: 40px; height: 40px; border-radius: 50%; color: var(--text-primary); text-decoration: none; margin-right: 6px;"
           title="Thông báo">
            <span class="material-symbols-outlined">notifications</span>
            <% if (unreadNotificationCount > 0) { %>
            <span style="position: absolute; top: 2px; right: 2px; min-width: 16px; height: 16px; padding: 0 3px; border-radius: 999px; background: var(--accent-purple); color: #fff; font-size: 0.65rem; line-height: 16px; text-align: center;"><%= unreadNotificationCount > 99 ? "99+" : unreadNotificationCount %></span>
            <% } %>
        </a>

        <div class="user-menu-wrap" id="userMenuWrap">
            <button type="button" class="user-menu-btn" id="userMenuBtn" aria-expanded="false">
                <% if (avatarUrl != null && !avatarUrl.isEmpty()) { %>
                    <img src="<%= HtmlUtils.escape(avatarUrl) %>" class="user-avatar-img" alt="Avatar">
                <% } else { %>
                    <div class="user-avatar-circle"><%= avatarInitial %></div>
                <% } %>
                <span class="user-name"><%= HtmlUtils.escape(nameToShow) %></span>
                <span class="material-symbols-outlined user-caret">expand_more</span>
            </button>

            <div class="user-dropdown" id="userDropdown">
                <div class="dropdown-header">
                    <div class="dh-name"><%= HtmlUtils.escape(nameToShow) %></div>
                    <div class="dh-role">
                        <%= isAdmin ? "QUẢN TRỊ VIÊN" : (isModerator ? "KIỂM DUYỆT VIÊN" : "THÀNH VIÊN") %>
                    </div>
                </div>

                <a class="dropdown-item" href="${pageContext.request.contextPath}/account/profile">
                    <span class="material-symbols-outlined">person</span>
                    <span>Hồ sơ cá nhân</span>
                </a>
                <a class="dropdown-item" href="${pageContext.request.contextPath}/account/settings">
                    <span class="material-symbols-outlined">settings</span>
                    <span>Cài đặt tài khoản</span>
                </a>
                <a class="dropdown-item" href="${pageContext.request.contextPath}/account/notifications">
                    <span class="material-symbols-outlined">notifications</span>
                    <span>Thông báo<%= unreadNotificationCount > 0 ? " (" + (unreadNotificationCount > 99 ? "99+" : unreadNotificationCount) + ")" : "" %></span>
                </a>
                <a class="dropdown-item" href="${pageContext.request.contextPath}/account/friends">
                    <span class="material-symbols-outlined">group</span>
                    <span>Bạn bè<%= incomingFriendRequestCount > 0 ? " (" + (incomingFriendRequestCount > 99 ? "99+" : incomingFriendRequestCount) + ")" : "" %></span>
                </a>
                <a class="dropdown-item" href="${pageContext.request.contextPath}/account/blocked">
                    <span class="material-symbols-outlined">block</span>
                    <span>Đã chặn</span>
                </a>
                <a class="dropdown-item" href="${pageContext.request.contextPath}/account/teams">
                    <span class="material-symbols-outlined">groups</span>
                    <span>Nhóm<%= incomingTeamInvitationCount > 0 ? " (" + (incomingTeamInvitationCount > 99 ? "99+" : incomingTeamInvitationCount) + ")" : "" %></span>
                </a>
                <a class="dropdown-item" href="${pageContext.request.contextPath}/account/chats">
                    <span class="material-symbols-outlined">chat</span>
                    <span>Trò chuyện<%= unreadChatCount > 0 ? " (" + (unreadChatCount > 99 ? "99+" : unreadChatCount) + ")" : "" %></span>
                </a>

                <% if (isAdmin) { %>
                    <a class="dropdown-item" href="${pageContext.request.contextPath}/admin/dashboard">
                        <span class="material-symbols-outlined">dashboard</span>
                        <span>Trang Quản trị (Admin)</span>
                    </a>
                <% } else if (isModerator) { %>
                    <a class="dropdown-item" href="${pageContext.request.contextPath}/moderator/dashboard">
                        <span class="material-symbols-outlined">shield_person</span>
                        <span>Trang Kiểm duyệt</span>
                    </a>
                <% } %>

                <div class="dropdown-divider"></div>

                <form action="${pageContext.request.contextPath}/logout" method="post" style="margin: 0;">
                    <button type="submit" class="dropdown-item logout">
                        <span class="material-symbols-outlined">logout</span>
                        <span>Đăng xuất</span>
                    </button>
                </form>
            </div>
        </div>
    </nav>

    <!-- Main Content -->
    <main class="home-container">
        <!-- Welcome Banner -->
        <section class="welcome-banner">
            <div class="welcome-text">
                <h1>Chào mừng trở lại, <span><%= HtmlUtils.escape(nameToShow) %></span>! 🎮</h1>
                <p>Khám phá kho thư viện trò chơi đa dạng, tham gia thảo luận cùng hàng ngàn game thủ và xây dựng đội ngũ của riêng bạn trên GameNest.</p>
                <div class="welcome-actions">
                    <a href="${pageContext.request.contextPath}/games" class="btn-action-primary">
                        <span class="material-symbols-outlined">sports_esports</span>
                        <span>Khám phá Trò chơi</span>
                    </a>
                    <a href="${pageContext.request.contextPath}/questions" class="btn-action-secondary">
                        <span class="material-symbols-outlined">quiz</span>
                        <span>Cộng đồng Hỏi đáp</span>
                    </a>
                </div>
            </div>

            <div class="user-quick-profile">
                <% if (avatarUrl != null && !avatarUrl.isEmpty()) { %>
                    <img src="<%= HtmlUtils.escape(avatarUrl) %>" class="uqp-avatar" alt="Avatar">
                <% } else { %>
                    <div class="uqp-avatar"><%= avatarInitial %></div>
                <% } %>
                <div class="uqp-info">
                    <div class="uqp-name"><%= HtmlUtils.escape(nameToShow) %></div>
                    <div class="uqp-tag">@<%= HtmlUtils.escape(username != null ? username : "") %></div>
                    <div class="uqp-badge">
                        <%= isAdmin ? "Quản trị viên" : (isModerator ? "Kiểm duyệt viên" : "Thành viên") %>
                    </div>
                </div>
            </div>
        </section>

        <!-- Feature Hub -->
        <div class="section-header">
            <h2 class="section-title">
                <span class="material-symbols-outlined">explore</span>
                <span>Khám phá các phân hệ GameNest</span>
            </h2>
        </div>

        <div class="cards-grid">
            <!-- Card 1: Thư viện Game -->
            <a href="${pageContext.request.contextPath}/games" class="feature-box">
                <div>
                    <div class="feature-icon-wrap">
                        <span class="material-symbols-outlined">sports_esports</span>
                    </div>
                    <h3>Thư viện Game</h3>
                    <p>Tra cứu thông tin, ngày phát hành và chi tiết hàng trăm tựa game thịnh hành trên thị trường.</p>
                </div>
                <div class="feature-footer">
                    <span>Xem danh sách game</span>
                    <span class="material-symbols-outlined">arrow_forward</span>
                </div>
            </a>

            <!-- Card 2: Hỏi Đáp -->
            <a href="${pageContext.request.contextPath}/questions" class="feature-box">
                <div>
                    <div class="feature-icon-wrap">
                        <span class="material-symbols-outlined">quiz</span>
                    </div>
                    <h3>Hỏi Đáp & Thảo luận</h3>
                    <p>Đặt câu hỏi về các màn chơi khó, tìm mẹo hay hoặc đóng góp câu trả lời giúp đỡ cộng đồng game thủ.</p>
                </div>
                <div class="feature-footer">
                    <span>Vào khu thảo luận</span>
                    <span class="material-symbols-outlined">arrow_forward</span>
                </div>
            </a>

            <!-- Card 3: Hồ sơ cá nhân -->
            <a href="${pageContext.request.contextPath}/account/profile" class="feature-box">
                <div>
                    <div class="feature-icon-wrap">
                        <span class="material-symbols-outlined">person</span>
                    </div>
                    <h3>Hồ sơ cá nhân</h3>
                    <p>Tùy chỉnh tên hiển thị, cập nhật ảnh đại diện cá tính để tỏa sáng trước bạn bè trong cộng đồng.</p>
                </div>
                <div class="feature-footer">
                    <span>Chỉnh sửa hồ sơ</span>
                    <span class="material-symbols-outlined">arrow_forward</span>
                </div>
            </a>

            <!-- Card 4: Cài đặt tài khoản -->
            <a href="${pageContext.request.contextPath}/account/settings" class="feature-box">
                <div>
                    <div class="feature-icon-wrap">
                        <span class="material-symbols-outlined">lock</span>
                    </div>
                    <h3>Bảo mật & Cài đặt</h3>
                    <p>Đổi mật khẩu định kỳ, xác thực mã OTP và quản lý thông tin bảo mật tài khoản an toàn.</p>
                </div>
                <div class="feature-footer">
                    <span>Quản lý bảo mật</span>
                    <span class="material-symbols-outlined">arrow_forward</span>
                </div>
            </a>
        </div>
    </main>

    <!-- Footer -->
    <footer class="footer">
        <p>&copy; 2026 GameNest. Nền tảng kết nối và đồng hành cùng cộng đồng game thủ.</p>
    </footer>

    <!-- Dropdown Toggle Script -->
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            var menuWrap = document.getElementById('userMenuWrap');
            var menuBtn = document.getElementById('userMenuBtn');

            if (menuBtn && menuWrap) {
                menuBtn.addEventListener('click', function(e) {
                    e.stopPropagation();
                    var isOpen = menuWrap.classList.toggle('open');
                    menuBtn.setAttribute('aria-expanded', isOpen);
                });

                document.addEventListener('click', function(e) {
                    if (!menuWrap.contains(e.target)) {
                        menuWrap.classList.remove('open');
                        menuBtn.setAttribute('aria-expanded', 'false');
                    }
                });
            }
        });
    </script>
</body>
</html>
