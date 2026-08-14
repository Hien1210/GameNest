<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng nhập - GameNest</title>
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
            <p>Kết nối cộng đồng game thủ</p>
        </div>

        <% if (request.getParameter("registered") != null) { %>
            <div class="alert alert-success">
                <span class="material-symbols-outlined alert-icon">check_circle</span>
                <span>Đăng ký thành công. Vui lòng đăng nhập.</span>
            </div>
        <% } %>
        
        <% if (request.getParameter("reset") != null) { %>
            <div class="alert alert-success">
                <span class="material-symbols-outlined alert-icon">check_circle</span>
                <span>Đặt lại mật khẩu thành công. Vui lòng đăng nhập.</span>
            </div>
        <% } %>

        <% if (request.getParameter("passwordChanged") != null) { %>
            <div class="alert alert-success">
                <span class="material-symbols-outlined alert-icon">check_circle</span>
                <span>Mật khẩu đã được thay đổi. Vui lòng đăng nhập lại.</span>
            </div>
        <% } %>

        <% if (request.getAttribute("error") != null) { %>
            <div class="alert alert-error">
                <span class="material-symbols-outlined alert-icon">error</span>
                <span><%= request.getAttribute("error") %></span>
            </div>
        <% } %>

        <form action="${pageContext.request.contextPath}/login" method="post">
            <div class="form-group">
                <label for="identifier">Username hoặc Email</label>
                <div class="input-wrapper">
                    <input type="text" id="identifier" name="identifier" value="${identifier}" required placeholder="Tên đăng nhập hoặc email">
                    <span class="material-symbols-outlined input-icon">person</span>
                </div>
            </div>

            <div class="form-group">
                <label for="password">Mật khẩu</label>
                <div class="input-wrapper">
                    <input type="password" id="password" name="password" required placeholder="Mật khẩu của bạn">
                    <span class="material-symbols-outlined input-icon">lock</span>
                    <span class="material-symbols-outlined password-toggle" onclick="togglePasswordVisibility()">visibility</span>
                </div>
            </div>

            <button type="submit" class="btn-submit">Đăng nhập</button>
        </form>

        <div class="auth-footer">
            <p>Chưa có tài khoản? <a href="${pageContext.request.contextPath}/register">Đăng ký ngay</a></p>
            <p><a href="${pageContext.request.contextPath}/forgot-password">Quên mật khẩu?</a></p>
        </div>
    </div>
</div>

<script>
function togglePasswordVisibility() {
    const passwordInput = document.getElementById('password');
    const toggleIcon = document.querySelector('.password-toggle');
    if (passwordInput.type === 'password') {
        passwordInput.type = 'text';
        toggleIcon.textContent = 'visibility_off';
    } else {
        passwordInput.type = 'password';
        toggleIcon.textContent = 'visibility';
    }
}
</script>
</body>
</html>
