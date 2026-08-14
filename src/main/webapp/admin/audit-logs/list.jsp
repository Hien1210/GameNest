<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="com.gamenest.model.AuditLog" %>
<%@ page import="com.gamenest.model.AuditModule" %>
<%@ page import="com.gamenest.model.AuditAction" %>
<%@ page import="com.gamenest.model.AuditTargetType" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<% request.setAttribute("pageTitle", "Nhật ký hệ thống"); %>
<%@ include file="/WEB-INF/admin/layout/header.jsp" %>

<div class="admin-page-header">
    <h1>Nhật ký hệ thống</h1>
</div>

<% if (request.getAttribute("error") != null) { %>
    <p style="color: var(--error-color);"><%= HtmlUtils.escape((String) request.getAttribute("error")) %></p>
<% } %>

<%
    String fModule = (String) request.getAttribute("module");
    String fAction = (String) request.getAttribute("action");
    String fUsername = (String) request.getAttribute("username");
    String fTargetType = (String) request.getAttribute("targetType");
    String fDateFrom = (String) request.getAttribute("dateFrom");
    String fDateTo = (String) request.getAttribute("dateTo");
    if (fModule == null) fModule = "";
    if (fAction == null) fAction = "";
    if (fUsername == null) fUsername = "";
    if (fTargetType == null) fTargetType = "";
    if (fDateFrom == null) fDateFrom = "";
    if (fDateTo == null) fDateTo = "";
%>
<form class="admin-filter-bar" action="${pageContext.request.contextPath}/admin/audit-logs" method="get">
    <div class="admin-filter-field">
        <label for="module">Module</label>
        <select id="module" name="module">
            <option value="">Tất cả</option>
            <option value="<%= AuditModule.ACCOUNTS %>" <%= AuditModule.ACCOUNTS.equals(fModule) ? "selected" : "" %>>ACCOUNTS</option>
            <option value="<%= AuditModule.GAMES %>" <%= AuditModule.GAMES.equals(fModule) ? "selected" : "" %>>GAMES</option>
        </select>
    </div>
    <div class="admin-filter-field">
        <label for="action">Action</label>
        <select id="action" name="action">
            <option value="">Tất cả</option>
            <option value="<%= AuditAction.CREATE %>" <%= AuditAction.CREATE.equals(fAction) ? "selected" : "" %>>CREATE</option>
            <option value="<%= AuditAction.UPDATE %>" <%= AuditAction.UPDATE.equals(fAction) ? "selected" : "" %>>UPDATE</option>
            <option value="<%= AuditAction.STATUS_CHANGE %>" <%= AuditAction.STATUS_CHANGE.equals(fAction) ? "selected" : "" %>>STATUS_CHANGE</option>
        </select>
    </div>
    <div class="admin-filter-field">
        <label for="username">Username</label>
        <input type="text" id="username" name="username" value="<%= HtmlUtils.escape(fUsername) %>" placeholder="Actor username">
    </div>
    <div class="admin-filter-field">
        <label for="targetType">Target Type</label>
        <select id="targetType" name="targetType">
            <option value="">Tất cả</option>
            <option value="<%= AuditTargetType.ACCOUNT %>" <%= AuditTargetType.ACCOUNT.equals(fTargetType) ? "selected" : "" %>>ACCOUNT</option>
            <option value="<%= AuditTargetType.GAME %>" <%= AuditTargetType.GAME.equals(fTargetType) ? "selected" : "" %>>GAME</option>
        </select>
    </div>
    <div class="admin-filter-field">
        <label for="dateFrom">Từ ngày</label>
        <input type="date" id="dateFrom" name="dateFrom" value="<%= HtmlUtils.escape(fDateFrom) %>">
    </div>
    <div class="admin-filter-field">
        <label for="dateTo">Đến ngày</label>
        <input type="date" id="dateTo" name="dateTo" value="<%= HtmlUtils.escape(fDateTo) %>">
    </div>
    <div class="admin-filter-actions">
        <button type="submit" class="admin-btn">
            <span class="material-symbols-outlined" style="font-size: 18px;">filter_alt</span>
            <span>Lọc</span>
        </button>
        <a class="admin-btn-secondary" href="${pageContext.request.contextPath}/admin/audit-logs">
            <span class="material-symbols-outlined" style="font-size: 18px;">restart_alt</span>
            <span>Xóa lọc</span>
        </a>
    </div>
</form>

<%
    @SuppressWarnings("unchecked")
    List<AuditLog> logs = (List<AuditLog>) request.getAttribute("logs");
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
%>
<div style="overflow-x: auto;">
<table class="admin-table">
    <thead>
        <tr>
            <th>Thời gian</th>
            <th>Actor</th>
            <th>Role</th>
            <th>Module</th>
            <th>Action</th>
            <th>Target</th>
            <th>Description</th>
            <th>IP</th>
            <th>User-Agent</th>
        </tr>
    </thead>
    <tbody>
    <% if (logs == null || logs.isEmpty()) { %>
        <tr><td colspan="9">Không có nhật ký nào phù hợp.</td></tr>
    <% } else {
        for (AuditLog log : logs) {
            String target = log.getTargetType() != null
                    ? HtmlUtils.escape(log.getTargetType()) + (log.getTargetId() != null ? " #" + log.getTargetId() : "")
                    : "-";
    %>
        <tr>
            <td><%= log.getCreatedAt() != null ? log.getCreatedAt().format(fmt) : "-" %></td>
            <td><%= HtmlUtils.escape(log.getUsername()) %></td>
            <td><%= HtmlUtils.escape(log.getRoleName()) %></td>
            <td><%= HtmlUtils.escape(log.getModule()) %></td>
            <td><%= HtmlUtils.escape(log.getAction()) %></td>
            <td><%= target %></td>
            <td><%= HtmlUtils.escape(log.getDescription()) %></td>
            <td><%= log.getIpAddress() != null ? HtmlUtils.escape(log.getIpAddress()) : "-" %></td>
            <td class="admin-cell-truncate" title="<%= HtmlUtils.escape(log.getUserAgent() != null ? log.getUserAgent() : "") %>">
                <%= log.getUserAgent() != null ? HtmlUtils.escape(log.getUserAgent()) : "-" %>
            </td>
        </tr>
    <% } } %>
    </tbody>
</table>
</div>

<%
    int currentPage = request.getAttribute("page") == null ? 1 : (int) request.getAttribute("page");
    int totalPages = request.getAttribute("totalPages") == null ? 1 : (int) request.getAttribute("totalPages");

    StringBuilder qs = new StringBuilder();
    if (!fModule.isEmpty()) qs.append("&module=").append(URLEncoder.encode(fModule, StandardCharsets.UTF_8));
    if (!fAction.isEmpty()) qs.append("&action=").append(URLEncoder.encode(fAction, StandardCharsets.UTF_8));
    if (!fUsername.isEmpty()) qs.append("&username=").append(URLEncoder.encode(fUsername, StandardCharsets.UTF_8));
    if (!fTargetType.isEmpty()) qs.append("&targetType=").append(URLEncoder.encode(fTargetType, StandardCharsets.UTF_8));
    if (!fDateFrom.isEmpty()) qs.append("&dateFrom=").append(URLEncoder.encode(fDateFrom, StandardCharsets.UTF_8));
    if (!fDateTo.isEmpty()) qs.append("&dateTo=").append(URLEncoder.encode(fDateTo, StandardCharsets.UTF_8));
    String qParam = qs.toString();
%>
<% if (totalPages > 1) { %>
    <div class="admin-pagination">
        <% for (int p = 1; p <= totalPages; p++) { %>
            <a class="<%= p == currentPage ? "current" : "" %>"
               href="${pageContext.request.contextPath}/admin/audit-logs?page=<%= p %><%= qParam %>"><%= p %></a>
        <% } %>
    </div>
<% } %>

<%@ include file="/WEB-INF/admin/layout/footer.jsp" %>
