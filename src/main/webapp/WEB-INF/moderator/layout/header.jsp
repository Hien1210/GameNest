<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<%
    // Shared Moderator Portal layout header — statically included by every
    // /moderator/* JSP (and by /account/profile, /account/settings when the
    // logged-in account is a MODERATOR) so the sidebar/topbar are defined
    // exactly once. Independent from the Admin layout — see css/moderator.css.
    // Active-menu detection uses the request's servlet path (server-side),
    // never client-side JS or a request parameter.
    String ctx = request.getContextPath();
    String servletPath = request.getServletPath();
    String pageTitle = request.getAttribute("pageTitle") == null ? "Moderator" : (String) request.getAttribute("pageTitle");

    Object displayNameAttr = session.getAttribute("displayName");
    Object usernameAttr = session.getAttribute("username");
    String modLabel = displayNameAttr != null ? (String) displayNameAttr
            : (usernameAttr != null ? (String) usernameAttr : null);
    String avatarInitial = (modLabel != null && !modLabel.isEmpty())
            ? modLabel.substring(0, 1).toUpperCase()
            : "M";
    Object avatarUrlAttr = session.getAttribute("avatarUrl");
    String avatarUrl = (avatarUrlAttr instanceof String && !((String) avatarUrlAttr).isEmpty())
            ? (String) avatarUrlAttr
            : null;

    boolean navDashboard = servletPath.equals("/moderator/dashboard");
    boolean navReports = servletPath.startsWith("/moderator/reports");
    boolean navQuestions = servletPath.startsWith("/moderator/questions");
    boolean navAnswers = servletPath.startsWith("/moderator/answers");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= HtmlUtils.escape(pageTitle) %> - GameNest Moderator</title>
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined" rel="stylesheet" />
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
    <link rel="stylesheet" href="<%= ctx %>/css/moderator.css">
</head>
<body class="mod-body">

<header class="mod-topbar">
    <button type="button" class="mod-sidebar-toggle" aria-label="Menu"
            onclick="document.querySelector('.mod-sidebar').classList.toggle('open')">
        <span class="material-symbols-outlined">menu</span>
    </button>
    <span class="mod-brand">GameNest Moderator</span>
    <div class="mod-topbar-right">
        <div class="mod-avatar-menu" id="modAvatarMenu">
            <button type="button" class="mod-avatar-btn" id="modAvatarBtn"
                    aria-haspopup="true" aria-expanded="false" aria-controls="modAvatarDropdown"
                    aria-label="Menu tài khoản moderator">
                <% if (avatarUrl != null) { %>
                    <img class="mod-avatar-img" src="<%= HtmlUtils.escape(avatarUrl) %>" alt="Avatar">
                <% } else { %>
                    <span class="mod-avatar-circle" aria-hidden="true"><%= avatarInitial %></span>
                <% } %>
                <% if (modLabel != null) { %>
                    <span class="mod-avatar-name"><%= HtmlUtils.escape(modLabel) %></span>
                <% } %>
                <span class="material-symbols-outlined mod-avatar-caret" aria-hidden="true">expand_more</span>
            </button>

            <div class="mod-avatar-dropdown" id="modAvatarDropdown" role="menu" aria-hidden="true">
                <div class="mod-avatar-dropdown-header">
                    <% if (avatarUrl != null) { %>
                        <img class="mod-avatar-img mod-avatar-img-lg" src="<%= HtmlUtils.escape(avatarUrl) %>" alt="Avatar">
                    <% } else { %>
                        <span class="mod-avatar-circle mod-avatar-circle-lg" aria-hidden="true"><%= avatarInitial %></span>
                    <% } %>
                    <div>
                        <div class="mod-avatar-dropdown-name"><%= HtmlUtils.escape(modLabel != null ? modLabel : "Moderator") %></div>
                        <span class="mod-avatar-dropdown-role">MODERATOR</span>
                    </div>
                </div>
                <div class="mod-avatar-dropdown-divider"></div>

                <a class="mod-avatar-dropdown-item" role="menuitem" href="<%= ctx %>/moderator/dashboard">
                    <span class="material-symbols-outlined">dashboard</span>
                    <span>Moderator Dashboard</span>
                </a>
                <a class="mod-avatar-dropdown-item" role="menuitem" href="<%= ctx %>/account/profile">
                    <span class="material-symbols-outlined">person</span>
                    <span>Hồ sơ cá nhân</span>
                </a>
                <a class="mod-avatar-dropdown-item" role="menuitem" href="<%= ctx %>/account/settings">
                    <span class="material-symbols-outlined">settings</span>
                    <span>Cài đặt tài khoản</span>
                </a>

                <div class="mod-avatar-dropdown-divider"></div>
                <form action="<%= ctx %>/logout" method="post" class="mod-avatar-dropdown-form">
                    <button type="submit" class="mod-avatar-dropdown-item mod-avatar-dropdown-logout" role="menuitem">
                        <span class="material-symbols-outlined">logout</span>
                        <span>Đăng xuất</span>
                    </button>
                </form>
            </div>
        </div>
    </div>
</header>

<div class="mod-shell">
    <aside class="mod-sidebar">
        <nav class="mod-nav">
            <a class="mod-nav-item<%= navDashboard ? " active" : "" %>" href="<%= ctx %>/moderator/dashboard">
                <span class="material-symbols-outlined">dashboard</span>
                <span>Dashboard</span>
            </a>
            <a class="mod-nav-item<%= navReports ? " active" : "" %>" href="<%= ctx %>/moderator/reports">
                <span class="material-symbols-outlined">flag</span>
                <span>Reports</span>
            </a>
            <a class="mod-nav-item<%= navQuestions ? " active" : "" %>" href="<%= ctx %>/moderator/questions">
                <span class="material-symbols-outlined">quiz</span>
                <span>Questions</span>
            </a>
            <a class="mod-nav-item<%= navAnswers ? " active" : "" %>" href="<%= ctx %>/moderator/answers">
                <span class="material-symbols-outlined">forum</span>
                <span>Answers</span>
            </a>
        </nav>

        <div class="mod-nav-footer">
            <a class="mod-nav-item" href="<%= ctx %>/">
                <span class="material-symbols-outlined">arrow_outward</span>
                <span>Trang chủ GameNest</span>
            </a>
            <form action="<%= ctx %>/logout" method="post" style="margin: 0;">
                <button type="submit" class="mod-nav-item" style="width: 100%; background: transparent; border: none; text-align: left; cursor: pointer; font-family: inherit;">
                    <span class="material-symbols-outlined">logout</span>
                    <span>Đăng xuất</span>
                </button>
            </form>
        </div>
    </aside>

    <main class="mod-content">
