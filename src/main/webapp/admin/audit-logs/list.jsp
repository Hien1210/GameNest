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
        <label for="module">Phân hệ</label>
        <select id="module" name="module">
            <option value="">Tất cả</option>
            <option value="<%= AuditModule.ACCOUNTS %>" <%= AuditModule.ACCOUNTS.equals(fModule) ? "selected" : "" %>>Tài khoản</option>
            <option value="<%= AuditModule.GAMES %>" <%= AuditModule.GAMES.equals(fModule) ? "selected" : "" %>>Trò chơi</option>
        </select>
    </div>
    <div class="admin-filter-field">
        <label for="action">Hành động</label>
        <select id="action" name="action">
            <option value="">Tất cả</option>
            <option value="<%= AuditAction.CREATE %>" <%= AuditAction.CREATE.equals(fAction) ? "selected" : "" %>>Tạo mới</option>
            <option value="<%= AuditAction.UPDATE %>" <%= AuditAction.UPDATE.equals(fAction) ? "selected" : "" %>>Cập nhật</option>
            <option value="<%= AuditAction.STATUS_CHANGE %>" <%= AuditAction.STATUS_CHANGE.equals(fAction) ? "selected" : "" %>>Đổi trạng thái</option>
        </select>
    </div>
    <div class="admin-filter-field">
        <label for="username">Tên tài khoản</label>
        <input type="text" id="username" name="username" value="<%= HtmlUtils.escape(fUsername) %>" placeholder="Tên người thực hiện">
    </div>
    <div class="admin-filter-field">
        <label for="targetType">Loại mục tiêu</label>
        <select id="targetType" name="targetType">
            <option value="">Tất cả</option>
            <option value="<%= AuditTargetType.ACCOUNT %>" <%= AuditTargetType.ACCOUNT.equals(fTargetType) ? "selected" : "" %>>Tài khoản</option>
            <option value="<%= AuditTargetType.GAME %>" <%= AuditTargetType.GAME.equals(fTargetType) ? "selected" : "" %>>Trò chơi</option>
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
            <th>Người thực hiện</th>
            <th>Vai trò</th>
            <th>Phân hệ</th>
            <th>Hành động</th>
            <th>Mục tiêu</th>
            <th>Mô tả</th>
            <th>IP</th>
            <th>Trình duyệt</th>
        </tr>
    </thead>
    <tbody>
    <% if (logs == null || logs.isEmpty()) { %>
        <tr><td colspan="9">Không có nhật ký nào phù hợp.</td></tr>
    <% } else {
        for (AuditLog log : logs) {
            String roleText = log.getRoleName();
            if ("ADMIN".equalsIgnoreCase(roleText)) roleText = "Quản trị viên";
            else if ("USER".equalsIgnoreCase(roleText)) roleText = "Người dùng";

            String moduleText = log.getModule();
            if (AuditModule.ACCOUNTS.equals(moduleText)) moduleText = "Tài khoản";
            else if (AuditModule.GAMES.equals(moduleText)) moduleText = "Trò chơi";

            String actionText = log.getAction();
            if (AuditAction.CREATE.equals(actionText)) actionText = "Tạo mới";
            else if (AuditAction.UPDATE.equals(actionText)) actionText = "Cập nhật";
            else if (AuditAction.STATUS_CHANGE.equals(actionText)) actionText = "Đổi trạng thái";

            String targetTypeFormatted = log.getTargetType();
            if (AuditTargetType.ACCOUNT.equals(targetTypeFormatted)) targetTypeFormatted = "Tài khoản";
            else if (AuditTargetType.GAME.equals(targetTypeFormatted)) targetTypeFormatted = "Trò chơi";

            String target = targetTypeFormatted != null
                    ? HtmlUtils.escape(targetTypeFormatted) + (log.getTargetId() != null ? " #" + log.getTargetId() : "")
                    : "-";
    %>
        <tr>
            <td><%= log.getCreatedAt() != null ? log.getCreatedAt().format(fmt) : "-" %></td>
            <td><%= HtmlUtils.escape(log.getUsername()) %></td>
            <td><%= HtmlUtils.escape(roleText) %></td>
            <td><%= HtmlUtils.escape(moduleText) %></td>
            <td><%= HtmlUtils.escape(actionText) %></td>
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
