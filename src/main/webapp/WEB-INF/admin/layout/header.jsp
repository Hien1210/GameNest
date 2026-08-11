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
    String adminLabel = displayNameAttr != null ? (String) displayNameAttr
            : (usernameAttr != null ? (String) usernameAttr : null);

    boolean navDashboard = servletPath.equals("/admin/dashboard");
    boolean navGames = servletPath.startsWith("/admin/games");
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
        <% if (adminLabel != null) { %>
            <span class="admin-user"><%= HtmlUtils.escape(adminLabel) %></span>
        <% } %>
        <form action="<%= ctx %>/logout" method="post" class="admin-logout-form">
            <button type="submit" class="admin-logout-btn">
                <span class="material-symbols-outlined">logout</span>
                <span class="admin-logout-label">Đăng xuất</span>
            </button>
        </form>
    </div>
</header>

<div class="admin-shell">
    <aside class="admin-sidebar">
        <nav class="admin-nav">
            <a class="admin-nav-item<%= navDashboard ? " active" : "" %>" href="<%= ctx %>/admin/dashboard">
                <span class="material-symbols-outlined">dashboard</span>
                <span>Dashboard</span>
            </a>
            <span class="admin-nav-item disabled" title="Chưa triển khai">
                <span class="material-symbols-outlined">group</span>
                <span>Accounts</span>
                <span class="admin-soon">Sắp có</span>
            </span>
            <a class="admin-nav-item<%= navGames ? " active" : "" %>" href="<%= ctx %>/admin/games">
                <span class="material-symbols-outlined">sports_esports</span>
                <span>Games</span>
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
