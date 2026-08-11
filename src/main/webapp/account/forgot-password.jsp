<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quên mật khẩu - GameNest</title>
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
            <p>Khôi phục mật khẩu</p>
        </div>

        <div class="otp-info">
            <p>Nhập email tài khoản của bạn để nhận mã xác thực OTP đặt lại mật khẩu.</p>
        </div>

        <% if (request.getAttribute("error") != null) { %>
            <div class="alert alert-error">
                <span class="material-symbols-outlined alert-icon">error</span>
                <span><%= request.getAttribute("error") %></span>
            </div>
        <% } %>

        <form action="${pageContext.request.contextPath}/forgot-password" method="post">
            <div class="form-group">
                <label for="email">Địa chỉ Email</label>
                <div class="input-wrapper">
                    <input type="email" id="email" name="email" required placeholder="example@domain.com">
                    <span class="material-symbols-outlined input-icon">mail</span>
                </div>
            </div>

            <button type="submit" class="btn-submit">Gửi mã OTP</button>
        </form>

        <div class="auth-footer">
            <a href="${pageContext.request.contextPath}/login">Quay lại đăng nhập</a>
        </div>
    </div>
</div>

</body>
</html>
