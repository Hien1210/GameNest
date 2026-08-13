<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.gamenest.model.Account" %>
<%@ page import="com.gamenest.model.AccountRole" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%
    // Personal Profile — self-service view/edit of the currently logged-in
    // account. Rendered inside the Admin layout for ADMIN, or as a
    // standalone auth-card page (matching login/register) for USER, since
    // no shared "logged-in user" layout exists yet in this project.
    Account account = (Account) request.getAttribute("account");
    String error = (String) request.getAttribute("error");
    boolean isAdmin = AccountRole.ADMIN.equals(session.getAttribute("role"));
    boolean updated = request.getParameter("updated") != null;
%>
<% if (isAdmin) {
    request.setAttribute("pageTitle", "Hồ sơ cá nhân");
%>
<%@ include file="/WEB-INF/admin/layout/header.jsp" %>
<% } else { %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hồ sơ cá nhân - GameNest</title>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined" rel="stylesheet" />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<% } %>

<div class="profile-container">
    <div class="profile-card">
        <h1 class="profile-title">Hồ sơ cá nhân</h1>

        <% if (error != null) { %>
        <div class="alert alert-error">
            <span class="material-symbols-outlined alert-icon">error</span>
            <span><%= HtmlUtils.escape(error) %></span>
        </div>
        <% } %>
        <% if (updated) { %>
        <div class="alert alert-success">
            <span class="material-symbols-outlined alert-icon">check_circle</span>
            <span>Cập nhật hồ sơ thành công.</span>
        </div>
        <% } %>

        <% if (account == null) { %>
        <p>Không thể tải thông tin tài khoản.</p>
        <% } else {
            String label = (account.getDisplayName() != null && !account.getDisplayName().isEmpty())
                    ? account.getDisplayName() : account.getUsername();
            String initial = (label != null && !label.isEmpty()) ? label.substring(0, 1).toUpperCase() : "U";
            boolean accountIsAdmin = AccountRole.ADMIN.equals(account.getRole());
            String createdAtDisplay = account.getCreatedAt() != null
                    ? account.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                    : "-";
        %>
        <div class="profile-header">
            <% if (account.getAvatarUrl() != null && !account.getAvatarUrl().isEmpty()) { %>
            <img class="profile-avatar-img" src="<%= HtmlUtils.escape(account.getAvatarUrl()) %>" alt="Avatar">
            <% } else { %>
            <span class="profile-avatar-circle" aria-hidden="true"><%= initial %></span>
            <% } %>
            <div>
                <div class="profile-name"><%= HtmlUtils.escape(label) %></div>
                <span class="profile-role-badge<%= accountIsAdmin ? " admin" : "" %>"><%= HtmlUtils.escape(account.getRole()) %></span>
            </div>
        </div>

        <dl class="profile-info">
            <dt>Username</dt><dd><%= HtmlUtils.escape(account.getUsername()) %></dd>
            <dt>Email</dt><dd><%= HtmlUtils.escape(account.getEmail()) %></dd>
            <dt>Trạng thái</dt><dd><%= HtmlUtils.escape(account.getStatus()) %></dd>
            <dt>Ngày tạo</dt><dd><%= createdAtDisplay %></dd>
        </dl>

        <form action="${pageContext.request.contextPath}/account/profile" method="post" enctype="multipart/form-data" class="profile-edit-form">
            <div class="form-group">
                <label for="displayName">Tên hiển thị</label>
                <div class="input-wrapper">
                    <input type="text" id="displayName" name="displayName"
                           value="<%= HtmlUtils.escape(account.getDisplayName() != null ? account.getDisplayName() : "") %>"
                           maxlength="100" required placeholder="Tên hiển thị">
                    <span class="material-symbols-outlined input-icon">badge</span>
                </div>
            </div>
            <div class="form-group">
                <label for="avatarFile">Ảnh đại diện (tối đa 2 MB)</label>
                <input type="file" id="avatarFile" name="avatarFile" accept="image/jpeg,image/png,image/webp">
            </div>
            <button type="submit" class="btn-submit">Lưu thay đổi</button>
        </form>
        <% } %>
    </div>
</div>

<% if (isAdmin) { %>
<%@ include file="/WEB-INF/admin/layout/footer.jsp" %>
<% } else { %>
</body>
</html>
<% } %>
