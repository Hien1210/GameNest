<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<%
    // Shared Admin layout header — statically included by every /admin/*
    // JSP so the sidebar/topbar are defined exactly once. Active-menu
    // detection uses the request's servlet path (server-side), never
    // client-side JS or a request parameter.
    String ctx = request.getContextPath();
    String servletPath = request.getServletPath();
    String pageTitle = request.getAttribute("pageTitle") == null ? "Admin" : (String) request.getAttribute("pageTitle");

    Object displayNameAttr = session.getAttribute("displayName");
    Object usernameAttr = session.getAttribute("username");
    Object roleAttr = session.getAttribute("role");
    String adminLabel = displayNameAttr != null ? (String) displayNameAttr
            : (usernameAttr != null ? (String) usernameAttr : null);
    String adminRole = roleAttr != null ? (String) roleAttr : "ADMIN";
    String avatarInitial = (adminLabel != null && !adminLabel.isEmpty())
            ? adminLabel.substring(0, 1).toUpperCase()
            : "A";
    Object avatarUrlAttr = session.getAttribute("avatarUrl");
    String avatarUrl = (avatarUrlAttr instanceof String && !((String) avatarUrlAttr).isEmpty())
            ? (String) avatarUrlAttr
            : null;

    boolean navDashboard = servletPath.equals("/admin/dashboard");
    boolean navAccounts = servletPath.startsWith("/admin/accounts");
    boolean navGames = servletPath.startsWith("/admin/games");
    boolean navAuditLogs = servletPath.startsWith("/admin/audit-logs");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= HtmlUtils.escape(pageTitle) %> - GameNest Admin</title>
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined" rel="stylesheet" />
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
    <link rel="stylesheet" href="<%= ctx %>/css/admin.css">
</head>
<body class="admin-body">

<header class="admin-topbar">
    <button type="button" class="admin-sidebar-toggle" aria-label="Menu"
            onclick="document.querySelector('.admin-sidebar').classList.toggle('open')">
        <span class="material-symbols-outlined">menu</span>
    </button>
    <span class="admin-brand">GameNest Admin</span>
    <div class="admin-topbar-right">
        <div class="admin-avatar-menu" id="adminAvatarMenu">
            <button type="button" class="admin-avatar-btn" id="adminAvatarBtn"
                    aria-haspopup="true" aria-expanded="false" aria-controls="adminAvatarDropdown"
                    aria-label="Menu tài khoản admin">
                <% if (avatarUrl != null) { %>
                    <img class="admin-avatar-img" src="<%= HtmlUtils.escape(avatarUrl) %>" alt="Avatar">
                <% } else { %>
                    <span class="admin-avatar-circle" aria-hidden="true"><%= avatarInitial %></span>
                <% } %>
                <% if (adminLabel != null) { %>
                    <span class="admin-avatar-name"><%= HtmlUtils.escape(adminLabel) %></span>
                <% } %>
                <span class="material-symbols-outlined admin-avatar-caret" aria-hidden="true">expand_more</span>
            </button>

            <div class="admin-avatar-dropdown" id="adminAvatarDropdown" role="menu" aria-hidden="true">
                <div class="admin-avatar-dropdown-header">
                    <% if (avatarUrl != null) { %>
                        <img class="admin-avatar-img admin-avatar-img-lg" src="<%= HtmlUtils.escape(avatarUrl) %>" alt="Avatar">
                    <% } else { %>
                        <span class="admin-avatar-circle admin-avatar-circle-lg" aria-hidden="true"><%= avatarInitial %></span>
                    <% } %>
                    <div>
                        <div class="admin-avatar-dropdown-name"><%= HtmlUtils.escape(adminLabel != null ? adminLabel : "Admin") %></div>
                        <span class="admin-avatar-dropdown-role"><%= HtmlUtils.escape(adminRole) %></span>
                    </div>
                </div>
                <div class="admin-avatar-dropdown-divider"></div>

                <a class="admin-avatar-dropdown-item" role="menuitem" href="<%= ctx %>/admin/dashboard">
                    <span class="material-symbols-outlined">dashboard</span>
                    <span>Admin Dashboard</span>
                </a>
                <a class="admin-avatar-dropdown-item" role="menuitem" href="<%= ctx %>/account/profile">
                    <span class="material-symbols-outlined">person</span>
                    <span>Hồ sơ cá nhân</span>
                </a>
                <a class="admin-avatar-dropdown-item" role="menuitem" href="<%= ctx %>/account/settings">
                    <span class="material-symbols-outlined">settings</span>
                    <span>Cài đặt tài khoản</span>
                </a>

                <div class="admin-avatar-dropdown-divider"></div>
                <form action="<%= ctx %>/logout" method="post" class="admin-avatar-dropdown-form">
                    <button type="submit" class="admin-avatar-dropdown-item admin-avatar-dropdown-logout" role="menuitem">
                        <span class="material-symbols-outlined">logout</span>
                        <span>Đăng xuất</span>
                    </button>
                </form>
            </div>
        </div>
    </div>
</header>

<div class="admin-shell">
    <aside class="admin-sidebar">
        <nav class="admin-nav">
            <a class="admin-nav-item<%= navDashboard ? " active" : "" %>" href="<%= ctx %>/admin/dashboard">
                <span class="material-symbols-outlined">dashboard</span>
                <span>Dashboard</span>
            </a>
            <a class="admin-nav-item<%= navAccounts ? " active" : "" %>" href="<%= ctx %>/admin/accounts">
                <span class="material-symbols-outlined">group</span>
                <span>Accounts</span>
            </a>
            <a class="admin-nav-item<%= navGames ? " active" : "" %>" href="<%= ctx %>/admin/games">
                <span class="material-symbols-outlined">sports_esports</span>
                <span>Games</span>
            </a>
            <a class="admin-nav-item<%= navAuditLogs ? " active" : "" %>" href="<%= ctx %>/admin/audit-logs">
                <span class="material-symbols-outlined">history</span>
                <span>Audit Log</span>
            </a>
            <span class="admin-nav-item disabled" title="Chưa triển khai">
                <span class="material-symbols-outlined">quiz</span>
                <span>Questions</span>
                <span class="admin-soon">Sắp có</span>
            </span>
            <span class="admin-nav-item disabled" title="Chưa triển khai">
                <span class="material-symbols-outlined">forum</span>
                <span>Answers</span>
                <span class="admin-soon">Sắp có</span>
            </span>
        </nav>

        <div class="admin-nav-footer">
            <a class="admin-nav-item" href="<%= ctx %>/">
                <span class="material-symbols-outlined">arrow_outward</span>
                <span>Trang chủ GameNest</span>
            </a>
        </div>
    </aside>

    <main class="admin-content">
