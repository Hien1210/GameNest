<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.gamenest.model.Account" %>
<%@ page import="com.gamenest.model.AccountStatus" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<% request.setAttribute("pageTitle", "Chi tiết tài khoản"); %>
<%@ include file="/WEB-INF/admin/layout/header.jsp" %>

<div class="admin-page-header">
    <h1>Chi tiết tài khoản</h1>
</div>

<% if (request.getAttribute("error") != null) { %>
    <p style="color: var(--error-color);"><%= HtmlUtils.escape((String) request.getAttribute("error")) %></p>
<% } %>

<%
    Account account = (Account) request.getAttribute("account");
    boolean isSelf = Boolean.TRUE.equals(request.getAttribute("isSelf"));
%>
<% if (account == null) { %>
    <p>Tài khoản không tồn tại.</p>
<% } else { %>
    <div class="admin-detail-card">
        <dl>
            <dt>ID</dt><dd><%= account.getAccountId() %></dd>
            <dt>Username</dt><dd><%= HtmlUtils.escape(account.getUsername()) %></dd>
            <dt>Email</dt><dd><%= HtmlUtils.escape(account.getEmail()) %></dd>
            <dt>Tên hiển thị</dt><dd><%= HtmlUtils.escape(account.getDisplayName()) %></dd>
            <dt>Vai trò</dt><dd><%= HtmlUtils.escape(account.getRole()) %></dd>
            <dt>Trạng thái</dt>
            <%
                String st = account.getStatus();
                String bClass = "admin-badge-inactive";
                if (AccountStatus.ACTIVE.equals(st)) bClass = "admin-badge-active";
                else if (AccountStatus.BANNED.equals(st)) bClass = "admin-badge-banned";
                else if (AccountStatus.SUSPENDED.equals(st)) bClass = "admin-badge-suspended";
                else if (AccountStatus.DELETED.equals(st)) bClass = "admin-badge-deleted";
            %>
            <dd class="<%= bClass %>"><%= st %></dd>
            <dt>Ngày tạo</dt><dd><%= account.getCreatedAt() != null ? account.getCreatedAt().toString() : "-" %></dd>
            <dt>Cập nhật gần nhất</dt><dd><%= account.getUpdatedAt() != null ? account.getUpdatedAt().toString() : "-" %></dd>
        </dl>

        <% if (isSelf) { %>
            <p style="color: var(--text-secondary);">Bạn không thể thay đổi trạng thái của chính tài khoản đang đăng nhập.</p>
        <% } else { %>
            <div class="admin-detail-actions">
                <% if (!AccountStatus.ACTIVE.equals(account.getStatus())) { %>
                <form action="${pageContext.request.contextPath}/admin/accounts/status" method="post">
                    <input type="hidden" name="id" value="<%= account.getAccountId() %>">
                    <input type="hidden" name="action" value="activate">
                    <button type="submit" class="btn-success">Kích hoạt</button>
                </form>
                <% } %>
                <% if (!AccountStatus.SUSPENDED.equals(account.getStatus())) { %>
                <form action="${pageContext.request.contextPath}/admin/accounts/status" method="post">
                    <input type="hidden" name="id" value="<%= account.getAccountId() %>">
                    <input type="hidden" name="action" value="suspend">
                    <button type="submit" class="btn-danger">Tạm khóa</button>
                </form>
                <% } %>
                <% if (!AccountStatus.BANNED.equals(account.getStatus())) { %>
                <form action="${pageContext.request.contextPath}/admin/accounts/status" method="post">
                    <input type="hidden" name="id" value="<%= account.getAccountId() %>">
                    <input type="hidden" name="action" value="ban">
                    <button type="submit" class="btn-danger">Cấm</button>
                </form>
                <% } %>
                <% if (!AccountStatus.DELETED.equals(account.getStatus())) { %>
                <form action="${pageContext.request.contextPath}/admin/accounts/status" method="post">
                    <input type="hidden" name="id" value="<%= account.getAccountId() %>">
                    <input type="hidden" name="action" value="delete">
                    <button type="submit" class="btn-danger">Đánh dấu đã xóa</button>
                </form>
                <% } %>
            </div>
        <% } %>
    </div>
<% } %>

<div class="admin-back-link">
    <a href="${pageContext.request.contextPath}/admin/accounts">&larr; Quay lại danh sách</a>
</div>

<%@ include file="/WEB-INF/admin/layout/footer.jsp" %>
