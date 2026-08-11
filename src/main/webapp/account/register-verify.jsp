<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Xác thực OTP - GameNest</title>
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
            <p>Xác thực tài khoản</p>
        </div>

        <div class="otp-info">
            <p>Mã OTP đã được gửi tới email của bạn:</p>
            <strong>${email}</strong>
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

        <form action="${pageContext.request.contextPath}/register/verify" method="post">
            <div class="form-group">
                <label for="otp">Mã OTP (6 chữ số)</label>
                <div class="input-wrapper">
                    <input type="text" id="otp" name="otp" inputmode="numeric" maxlength="6" required autofocus placeholder="Nhập mã OTP">
                    <span class="material-symbols-outlined input-icon">pin</span>
                </div>
            </div>

            <button type="submit" class="btn-submit">Xác nhận</button>
        </form>

        <form action="${pageContext.request.contextPath}/register/verify" method="post" style="margin-top: 15px;">
            <input type="hidden" name="resend" value="1">
            <button type="submit" class="btn-secondary">Gửi lại mã OTP</button>
        </form>

        <div class="auth-footer" style="margin-top: 20px;">
            <a href="${pageContext.request.contextPath}/login">Quay lại đăng nhập</a>
        </div>
    </div>
</div>

</body>
</html>
