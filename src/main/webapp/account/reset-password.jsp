<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đặt lại mật khẩu - GameNest</title>
    <!-- Google Fonts & Material Symbols -->
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined" rel="stylesheet" />
    <!-- Custom Style -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>

<div class="auth-container">
    <div class="auth-card">
        <div class="auth-logo">
            <h1>GameNest</h1>
            <p>Đặt lại mật khẩu</p>
        </div>

        <% if (request.getAttribute("info") != null) { %>
            <div class="alert alert-success">
                <span class="material-symbols-outlined alert-icon">check_circle</span>
                <span><%= request.getAttribute("info") %></span>
            </div>
        <% } %>

        <% if (request.getAttribute("error") != null) { %>
            <div class="alert alert-error">
                <span class="material-symbols-outlined alert-icon">error</span>
                <span><%= request.getAttribute("error") %></span>
            </div>
        <% } %>

        <form id="resetForm" action="${pageContext.request.contextPath}/reset-password" method="post" onsubmit="return validatePasswords()">
            <div class="form-group">
                <label for="email">Địa chỉ Email</label>
                <div class="input-wrapper">
                    <input type="email" id="email" name="email" value="${email}" required placeholder="example@domain.com">
                    <span class="material-symbols-outlined input-icon">mail</span>
                </div>
            </div>

            <div class="form-group">
                <label for="otp">Mã xác thực OTP</label>
                <div class="input-wrapper">
                    <input type="text" id="otp" name="otp" inputmode="numeric" maxlength="6" required placeholder="Nhập 6 chữ số OTP">
                    <span class="material-symbols-outlined input-icon">pin</span>
                </div>
            </div>

            <div class="form-group">
                <label for="newPassword">Mật khẩu mới</label>
                <div class="input-wrapper">
                    <input type="password" id="newPassword" name="newPassword" required minlength="8" placeholder="Tối thiểu 8 ký tự">
                    <span class="material-symbols-outlined input-icon">lock</span>
                    <span class="material-symbols-outlined password-toggle" onclick="togglePasswordVisibility('newPassword', this)">visibility</span>
                </div>
            </div>

            <div class="form-group">
                <label for="confirmPassword">Xác nhận mật khẩu mới</label>
                <div class="input-wrapper">
                    <input type="password" id="confirmPassword" name="confirmPassword" required minlength="8" placeholder="Nhập lại mật khẩu mới">
                    <span class="material-symbols-outlined input-icon">lock_reset</span>
                    <span class="material-symbols-outlined password-toggle" onclick="togglePasswordVisibility('confirmPassword', this)">visibility</span>
                </div>
                <div id="passwordError" class="alert alert-error" style="display: none; margin-top: 10px; margin-bottom: 0;">
                    <span class="material-symbols-outlined alert-icon">error</span>
                    <span>Mật khẩu xác nhận không khớp!</span>
                </div>
            </div>

            <button type="submit" class="btn-submit">Đặt lại mật khẩu</button>
        </form>

        <div class="auth-footer">
            <p>Chưa nhận được mã? <a href="${pageContext.request.contextPath}/forgot-password">Gửi lại yêu cầu</a></p>
            <p><a href="${pageContext.request.contextPath}/login">Quay lại đăng nhập</a></p>
        </div>
    </div>
</div>

<script>
function togglePasswordVisibility(inputId, toggleIcon) {
    const passwordInput = document.getElementById(inputId);
    if (passwordInput.type === 'password') {
        passwordInput.type = 'text';
        toggleIcon.textContent = 'visibility_off';
    } else {
        passwordInput.type = 'password';
        toggleIcon.textContent = 'visibility';
    }
}

function validatePasswords() {
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    const errorDiv = document.getElementById('passwordError');
    
    if (newPassword !== confirmPassword) {
        errorDiv.style.display = 'flex';
        return false;
    }
    errorDiv.style.display = 'none';
    return true;
}
</script>
</body>
</html>
