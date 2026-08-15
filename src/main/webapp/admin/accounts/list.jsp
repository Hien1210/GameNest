<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.gamenest.model.Account" %>
<%@ page import="com.gamenest.model.AccountStatus" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<% request.setAttribute("pageTitle", "Quản lý tài khoản"); %>
<%@ include file="/WEB-INF/admin/layout/header.jsp" %>

<div class="admin-page-header">
    <h1>Quản lý tài khoản</h1>
</div>

<% if (request.getAttribute("error") != null) { %>
    <p style="color: var(--error-color);"><%= HtmlUtils.escape((String) request.getAttribute("error")) %></p>
<% } %>

<%
    String q = request.getAttribute("q") == null ? "" : (String) request.getAttribute("q");
%>
<form class="admin-search-bar" action="${pageContext.request.contextPath}/admin/accounts" method="get">
    <input type="text" name="q" placeholder="Tìm theo username hoặc email..." value="<%= HtmlUtils.escape(q) %>">
    <button type="submit" class="admin-btn">Tìm kiếm</button>
</form>

<%
    @SuppressWarnings("unchecked")
    List<Account> accounts = (List<Account>) request.getAttribute("accounts");
%>
<table class="admin-table">
    <thead>
        <tr>
            <th>ID</th>
            <th>Username</th>
            <th>Email</th>
            <th>Vai trò</th>
            <th>Trạng thái</th>
            <th>Ngày tạo</th>
            <th>Hành động</th>
        </tr>
    </thead>
    <tbody>
    <% if (accounts == null || accounts.isEmpty()) { %>
        <tr><td colspan="7">Không tìm thấy tài khoản nào.</td></tr>
    <% } else {
        for (Account a : accounts) { %>
        <tr>
            <td><%= a.getAccountId() %></td>
            <td><%= HtmlUtils.escape(a.getUsername()) %></td>
            <td><%= HtmlUtils.escape(a.getEmail()) %></td>
            <%
                String roleText = a.getRole();
                if ("ADMIN".equalsIgnoreCase(roleText)) roleText = "Quản trị viên";
                else if ("USER".equalsIgnoreCase(roleText)) roleText = "Người dùng";
            %>
            <td><%= HtmlUtils.escape(roleText) %></td>
            <%
                String st = a.getStatus();
                String bClass = "admin-badge-inactive";
                String statusText = st;
                if (AccountStatus.ACTIVE.equals(st)) {
                    bClass = "admin-badge-active";
                    statusText = "Hoạt động";
                } else if (AccountStatus.BANNED.equals(st)) {
                    bClass = "admin-badge-banned";
                    statusText = "Bị cấm";
                } else if (AccountStatus.SUSPENDED.equals(st)) {
                    bClass = "admin-badge-suspended";
                    statusText = "Tạm khóa";
                } else if (AccountStatus.DELETED.equals(st)) {
                    bClass = "admin-badge-deleted";
                    statusText = "Đã xóa";
                }
            %>
            <td class="<%= bClass %>"><%= statusText %></td>
            <td><%= a.getCreatedAt() != null ? a.getCreatedAt().toLocalDate().toString() : "-" %></td>
            <td class="admin-actions">
                <a class="btn-edit" href="${pageContext.request.contextPath}/admin/accounts/detail?id=<%= a.getAccountId() %>">Chi tiết</a>
            </td>
        </tr>
    <% } } %>
    </tbody>
</table>

<%
    int currentPage = request.getAttribute("page") == null ? 1 : (int) request.getAttribute("page");
    int totalPages = request.getAttribute("totalPages") == null ? 1 : (int) request.getAttribute("totalPages");
    String qParam = q.isEmpty() ? "" : "&q=" + java.net.URLEncoder.encode(q, java.nio.charset.StandardCharsets.UTF_8);
%>
<% if (totalPages > 1) { %>
    <div class="admin-pagination">
        <% for (int p = 1; p <= totalPages; p++) { %>
            <a class="<%= p == currentPage ? "current" : "" %>"
               href="${pageContext.request.contextPath}/admin/accounts?page=<%= p %><%= qParam %>"><%= p %></a>
        <% } %>
    </div>
<% } %>

<%@ include file="/WEB-INF/admin/layout/footer.jsp" %>
