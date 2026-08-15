<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.gamenest.dto.AdminDashboardStats" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<% request.setAttribute("pageTitle", "Tổng quan"); %>
<%@ include file="/WEB-INF/admin/layout/header.jsp" %>

<div class="admin-page-header">
    <h1>Tổng quan</h1>
</div>

<% if (request.getAttribute("error") != null) { %>
    <p style="color: var(--error-color); margin-bottom: 20px;"><%= HtmlUtils.escape((String) request.getAttribute("error")) %></p>
<% } %>

<%
    AdminDashboardStats stats = (AdminDashboardStats) request.getAttribute("stats");
%>
<% if (stats != null) { %>
    <div class="admin-stat-grid">
        <div class="admin-stat-card">
            <div class="label">Tài khoản</div>
            <div class="value"><%= stats.getTotalAccounts() %></div>
        </div>
        <a class="admin-stat-card" href="${pageContext.request.contextPath}/admin/games">
            <div class="label">Trò chơi</div>
            <div class="value"><%= stats.getTotalGames() %></div>
        </a>
        <div class="admin-stat-card">
            <div class="label">Câu hỏi</div>
            <div class="value"><%= stats.getTotalQuestions() %></div>
        </div>
        <div class="admin-stat-card">
            <div class="label">Câu trả lời</div>
            <div class="value"><%= stats.getTotalAnswers() %></div>
        </div>
    </div>

    <div class="admin-breakdown-section">
        <h2>Trò chơi</h2>
        <div class="admin-breakdown-row">
            <div class="admin-breakdown-item active">
                <div class="label">Hoạt động</div>
                <div class="value"><%= stats.getActiveGames() %></div>
            </div>
            <div class="admin-breakdown-item inactive">
                <div class="label">Tạm ẩn</div>
                <div class="value"><%= stats.getInactiveGames() %></div>
            </div>
        </div>
    </div>

    <div class="admin-breakdown-section">
        <h2>Câu hỏi</h2>
        <div class="admin-breakdown-row">
            <div class="admin-breakdown-item active">
                <div class="label">Hoạt động</div>
                <div class="value"><%= stats.getActiveQuestions() %></div>
            </div>
            <div class="admin-breakdown-item deleted">
                <div class="label">Đã xóa</div>
                <div class="value"><%= stats.getDeletedQuestions() %></div>
            </div>
        </div>
    </div>

    <div class="admin-breakdown-section">
        <h2>Câu trả lời</h2>
        <div class="admin-breakdown-row">
            <div class="admin-breakdown-item active">
                <div class="label">Hoạt động</div>
                <div class="value"><%= stats.getActiveAnswers() %></div>
            </div>
            <div class="admin-breakdown-item deleted">
                <div class="label">Đã xóa</div>
                <div class="value"><%= stats.getDeletedAnswers() %></div>
            </div>
        </div>
    </div>
<% } %>

<%@ include file="/WEB-INF/admin/layout/footer.jsp" %>
