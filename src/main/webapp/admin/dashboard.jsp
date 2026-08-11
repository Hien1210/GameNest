<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.gamenest.dto.AdminDashboardStats" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<% request.setAttribute("pageTitle", "Dashboard"); %>
<%@ include file="/WEB-INF/admin/layout/header.jsp" %>

<h1>Dashboard</h1>

<% if (request.getAttribute("error") != null) { %>
    <p style="color: var(--error-color); margin-bottom: 20px;"><%= HtmlUtils.escape((String) request.getAttribute("error")) %></p>
<% } %>

<%
    AdminDashboardStats stats = (AdminDashboardStats) request.getAttribute("stats");
%>
<% if (stats != null) { %>
    <div class="admin-stat-grid">
        <div class="admin-stat-card">
            <div class="label">Accounts</div>
            <div class="value"><%= stats.getTotalAccounts() %></div>
        </div>
        <a class="admin-stat-card" href="${pageContext.request.contextPath}/admin/games">
            <div class="label">Games</div>
            <div class="value"><%= stats.getTotalGames() %></div>
        </a>
        <div class="admin-stat-card">
            <div class="label">Questions</div>
            <div class="value"><%= stats.getTotalQuestions() %></div>
        </div>
        <div class="admin-stat-card">
            <div class="label">Answers</div>
            <div class="value"><%= stats.getTotalAnswers() %></div>
        </div>
    </div>

    <div class="admin-breakdown-section">
        <h2>Games</h2>
        <div class="admin-breakdown-row">
            <div class="admin-breakdown-item active">
                <div class="label">ACTIVE</div>
                <div class="value"><%= stats.getActiveGames() %></div>
            </div>
            <div class="admin-breakdown-item inactive">
                <div class="label">INACTIVE</div>
                <div class="value"><%= stats.getInactiveGames() %></div>
            </div>
        </div>
    </div>

    <div class="admin-breakdown-section">
        <h2>Questions</h2>
        <div class="admin-breakdown-row">
            <div class="admin-breakdown-item active">
                <div class="label">ACTIVE</div>
                <div class="value"><%= stats.getActiveQuestions() %></div>
            </div>
            <div class="admin-breakdown-item deleted">
                <div class="label">DELETED</div>
                <div class="value"><%= stats.getDeletedQuestions() %></div>
            </div>
        </div>
    </div>

    <div class="admin-breakdown-section">
        <h2>Answers</h2>
        <div class="admin-breakdown-row">
            <div class="admin-breakdown-item active">
                <div class="label">ACTIVE</div>
                <div class="value"><%= stats.getActiveAnswers() %></div>
            </div>
            <div class="admin-breakdown-item deleted">
                <div class="label">DELETED</div>
                <div class="value"><%= stats.getDeletedAnswers() %></div>
            </div>
        </div>
    </div>
<% } %>

<%@ include file="/WEB-INF/admin/layout/footer.jsp" %>
