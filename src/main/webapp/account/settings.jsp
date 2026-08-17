<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.gamenest.model.Account" %>
<%@ page import="com.gamenest.model.AccountRole" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<%
    // Account Settings — self-service Đổi mật khẩu / Đổi email. Same
    // dual-render convention as /account/profile: rendered inside the
    // Admin layout for ADMIN, or as a standalone auth-card page for USER,
    // since no shared "logged-in user" layout exists yet in this project.
    Account account = (Account) request.getAttribute("account");
    String pageError = (String) request.getAttribute("error");
    String passwordError = (String) request.getAttribute("passwordError");
    String emailError = (String) request.getAttribute("emailError");
    String emailInfo = (String) request.getAttribute("emailInfo");
    String pendingNewEmail = (String) request.getAttribute("pendingNewEmail");
    String sessionRole = (String) session.getAttribute("role");
    boolean isAdmin = AccountRole.ADMIN.equals(sessionRole);
    boolean isModerator = AccountRole.MODERATOR.equals(sessionRole);
    boolean emailChanged = request.getParameter("emailChanged") != null;
%>
<% if (isAdmin) {
    request.setAttribute("pageTitle", "Cài đặt tài khoản");
%>
<%@ include file="/WEB-INF/admin/layout/header.jsp" %>
<% } else if (isModerator) {
    request.setAttribute("pageTitle", "Cài đặt tài khoản");
%>
<%@ include file="/WEB-INF/moderator/layout/header.jsp" %>
<% } else { %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cài đặt tài khoản - GameNest</title>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined" rel="stylesheet" />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<% } %>

<div class="profile-container">
    <% if (pageError != null) { %>
    <div class="profile-card" style="margin-bottom: 20px;">
        <div class="alert alert-error">
            <span class="material-symbols-outlined alert-icon">error</span>
            <span><%= HtmlUtils.escape(pageError) %></span>
        </div>
    </div>
    <% } else { %>

    <!-- Đổi mật khẩu -->
    <div class="profile-card" style="margin-bottom: 20px;">
        <h1 class="profile-title">Đổi mật khẩu</h1>

        <% if (passwordError != null) { %>
        <div class="alert alert-error">
            <span class="material-symbols-outlined alert-icon">error</span>
            <span><%= HtmlUtils.escape(passwordError) %></span>
        </div>
        <% } %>

        <form action="${pageContext.request.contextPath}/account/settings/password" method="post" class="profile-edit-form">
            <div class="form-group">
                <label for="currentPassword">Mật khẩu hiện tại</label>
                <div class="input-wrapper">
                    <input type="password" id="currentPassword" name="currentPassword" required placeholder="Mật khẩu hiện tại">
                    <span class="material-symbols-outlined input-icon">lock</span>
                    <span class="material-symbols-outlined password-toggle" onclick="toggleVisibility('currentPassword', this)">visibility</span>
                </div>
            </div>
            <div class="form-group">
                <label for="newPassword">Mật khẩu mới</label>
                <div class="input-wrapper">
                    <input type="password" id="newPassword" name="newPassword" required minlength="8" placeholder="Ít nhất 8 ký tự">
                    <span class="material-symbols-outlined input-icon">lock_reset</span>
                    <span class="material-symbols-outlined password-toggle" onclick="toggleVisibility('newPassword', this)">visibility</span>
                </div>
            </div>
            <div class="form-group">
                <label for="confirmPassword">Xác nhận mật khẩu mới</label>
                <div class="input-wrapper">
                    <input type="password" id="confirmPassword" name="confirmPassword" required minlength="8" placeholder="Nhập lại mật khẩu mới">
                    <span class="material-symbols-outlined input-icon">check_circle</span>
                    <span class="material-symbols-outlined password-toggle" onclick="toggleVisibility('confirmPassword', this)">visibility</span>
                </div>
            </div>
            <button type="submit" class="btn-submit">Đổi mật khẩu</button>
        </form>
    </div>

    <!-- Đổi Email -->
    <div class="profile-card">
        <h1 class="profile-title">Email</h1>

        <% if (emailChanged) { %>
        <div class="alert alert-success">
            <span class="material-symbols-outlined alert-icon">check_circle</span>
            <span>Đổi email thành công.</span>
        </div>
        <% } %>
        <% if (emailInfo != null) { %>
        <div class="alert alert-success">
            <span class="material-symbols-outlined alert-icon">check_circle</span>
            <span><%= HtmlUtils.escape(emailInfo) %></span>
        </div>
        <% } %>
        <% if (emailError != null) { %>
        <div class="alert alert-error">
            <span class="material-symbols-outlined alert-icon">error</span>
            <span><%= HtmlUtils.escape(emailError) %></span>
        </div>
        <% } %>

        <dl class="profile-info">
            <dt>Email hiện tại</dt>
            <dd><%= account != null ? HtmlUtils.escape(account.getEmail()) : "-" %></dd>
        </dl>

        <% if (pendingNewEmail != null) { %>
        <div class="otp-info">
            <p>Mã OTP đã được gửi tới email mới:</p>
            <strong><%= HtmlUtils.escape(pendingNewEmail) %></strong>
        </div>

        <form action="${pageContext.request.contextPath}/account/settings/email/verify" method="post" class="profile-edit-form">
            <div class="form-group">
                <label for="otp">Mã OTP (6 chữ số)</label>
                <div class="input-wrapper">
                    <input type="text" id="otp" name="otp" inputmode="numeric" maxlength="6" required autofocus placeholder="Nhập mã OTP">
                    <span class="material-symbols-outlined input-icon">pin</span>
                </div>
            </div>
            <button type="submit" class="btn-submit">Xác nhận OTP</button>
        </form>

        <form action="${pageContext.request.contextPath}/account/settings/email/verify" method="post" style="margin-top: 12px;">
            <input type="hidden" name="resend" value="1">
            <button type="submit" class="btn-secondary">Gửi lại mã OTP</button>
        </form>
        <% } else { %>

        <form action="${pageContext.request.contextPath}/account/settings/email" method="post" class="profile-edit-form">
            <div class="form-group">
                <label for="newEmail">Email mới</label>
                <div class="input-wrapper">
                    <input type="email" id="newEmail" name="newEmail" required placeholder="email-moi@example.com">
                    <span class="material-symbols-outlined input-icon">mail</span>
                </div>
            </div>
            <button type="submit" class="btn-submit">Gửi OTP</button>
        </form>
        <% } %>
    </div>
    <% } %>

    <% if (!isAdmin && !isModerator) { %>
    <div style="margin-top: 20px; text-align: center;">
        <a href="${pageContext.request.contextPath}/account/home.jsp" style="color: var(--accent-cyan); text-decoration: none; font-size: 0.92rem; display: inline-flex; align-items: center; gap: 6px; padding: 8px 16px; border-radius: 8px; background: rgba(255,255,255,0.04); border: 1px solid var(--border-color);">
            <span class="material-symbols-outlined" style="font-size: 18px;">arrow_back</span>
            <span>Quay lại trang chủ GameNest</span>
        </a>
    </div>
    <% } %>
</div>

<script>
function toggleVisibility(inputId, iconEl) {
    var input = document.getElementById(inputId);
    if (input.type === 'password') {
        input.type = 'text';
        iconEl.textContent = 'visibility_off';
    } else {
        input.type = 'password';
        iconEl.textContent = 'visibility';
    }
}
</script>

<% if (isAdmin) { %>
<%@ include file="/WEB-INF/admin/layout/footer.jsp" %>
<% } else if (isModerator) { %>
<%@ include file="/WEB-INF/moderator/layout/footer.jsp" %>
<% } else { %>
</body>
</html>
<% } %>
