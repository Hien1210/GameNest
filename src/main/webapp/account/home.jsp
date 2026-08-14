<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Trang chủ - GameNest</title>
</head>
<body>
<h1>Chào mừng, ${sessionScope.displayName}!</h1>
<p>Username: ${sessionScope.username}</p>

<p><a href="${pageContext.request.contextPath}/games">Games</a></p>
<p><a href="${pageContext.request.contextPath}/account/profile">Hồ sơ cá nhân</a></p>
<p><a href="${pageContext.request.contextPath}/account/settings">Cài đặt tài khoản</a></p>

<% if ("ADMIN".equals(session.getAttribute("role"))) { %>
<p><a href="${pageContext.request.contextPath}/admin/games">Quản lý Games (Admin)</a></p>
<% } %>

<form action="${pageContext.request.contextPath}/logout" method="post">
    <button type="submit">Đăng xuất</button>
</form>
</body>
</html>
