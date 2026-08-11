<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng ký - GameNest</title>
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
            <p>Tạo tài khoản mới</p>
        </div>

        <% if (request.getAttribute("error") != null) { %>
            <div class="alert alert-error">
                <span class="material-symbols-outlined alert-icon">error</span>
                <span><%= request.getAttribute("error") %></span>
            </div>
        <% } %>

        <form action="${pageContext.request.contextPath}/register" method="post">
            <div class="form-group">
                <label for="username">Tên đăng nhập (Username)</label>
                <div class="input-wrapper">
                    <input type="text" id="username" name="username" value="${username}" required maxlength="50" placeholder="Tên đăng nhập viết liền, không dấu">
                    <span class="material-symbols-outlined input-icon">person</span>
                </div>
            </div>

            <div class="form-group">
                <label for="email">Địa chỉ Email</label>
                <div class="input-wrapper">
                    <input type="email" id="email" name="email" value="${email}" required maxlength="255" placeholder="example@domain.com">
                    <span class="material-symbols-outlined input-icon">mail</span>
                </div>
            </div>

            <div class="form-group">
                <label for="password">Mật khẩu</label>
                <div class="input-wrapper">
                    <input type="password" id="password" name="password" required minlength="8" placeholder="Tối thiểu 8 ký tự">
                    <span class="material-symbols-outlined input-icon">lock</span>
                    <span class="material-symbols-outlined password-toggle" onclick="togglePasswordVisibility()">visibility</span>
                </div>
            </div>

            <div class="form-group">
                <label for="displayName">Tên hiển thị (Tùy chọn)</label>
                <div class="input-wrapper">
                    <input type="text" id="displayName" name="displayName" value="${displayName}" maxlength="100" placeholder="Biệt danh hiển thị trên cộng đồng">
                    <span class="material-symbols-outlined input-icon">badge</span>
                </div>
            </div>

            <button type="submit" class="btn-submit">Đăng ký tài khoản</button>
        </form>

        <div class="auth-footer">
            <p>Đã có tài khoản? <a href="${pageContext.request.contextPath}/login">Đăng nhập</a></p>
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
