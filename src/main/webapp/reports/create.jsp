<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.gamenest.model.AccountRole" %>
<%@ page import="com.gamenest.model.ReportReason" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<%
    // Report creation — self-service, ACCOUNT/QUESTION/ANSWER only. Same
    // dual-render convention as /account/profile and /account/settings.
    String error = (String) request.getAttribute("error");
    boolean success = Boolean.TRUE.equals(request.getAttribute("success"));
    String targetType = (String) request.getAttribute("targetType");
    Object targetIdAttr = request.getAttribute("targetId");
    String targetLabel = (String) request.getAttribute("targetLabel");
    boolean isAdmin = AccountRole.ADMIN.equals(session.getAttribute("role"));
%>
<% if (isAdmin) {
    request.setAttribute("pageTitle", "Báo cáo");
%>
<%@ include file="/WEB-INF/admin/layout/header.jsp" %>
<% } else { %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Báo cáo - GameNest</title>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined" rel="stylesheet" />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<% } %>

<div class="profile-container">
    <div class="profile-card">
        <h1 class="profile-title">Báo cáo</h1>

        <% if (success) { %>
        <div class="alert alert-success">
            <span class="material-symbols-outlined alert-icon">check_circle</span>
            <span>Báo cáo của bạn đã được gửi. Admin sẽ xem xét sớm nhất.</span>
        </div>
        <% } %>
        <% if (error != null) { %>
        <div class="alert alert-error">
            <span class="material-symbols-outlined alert-icon">error</span>
            <span><%= HtmlUtils.escape(error) %></span>
        </div>
        <% } %>

        <% if (targetType != null && targetIdAttr != null) { %>
        <dl class="profile-info">
            <dt>Đối tượng</dt>
            <dd><%= HtmlUtils.escape(targetType) %> #<%= targetIdAttr %><%= targetLabel != null ? " — " + HtmlUtils.escape(targetLabel) : "" %></dd>
        </dl>

        <form action="${pageContext.request.contextPath}/reports/create" method="post" class="profile-edit-form">
            <input type="hidden" name="targetType" value="<%= HtmlUtils.escape(targetType) %>">
            <input type="hidden" name="targetId" value="<%= targetIdAttr %>">

            <div class="form-group">
                <label for="reason">Lý do</label>
                <div class="input-wrapper">
                    <select id="reason" name="reason" required style="width:100%; background:transparent; border:none; color:inherit; font-family:inherit; font-size:inherit;">
                        <option value="<%= ReportReason.SPAM %>">Spam</option>
                        <option value="<%= ReportReason.HARASSMENT %>">Quấy rối</option>
                        <option value="<%= ReportReason.INAPPROPRIATE_CONTENT %>">Nội dung không phù hợp</option>
                        <option value="<%= ReportReason.HATE_SPEECH %>">Phát ngôn thù ghét</option>
                        <option value="<%= ReportReason.MISINFORMATION %>">Thông tin sai lệch</option>
                        <option value="<%= ReportReason.CHEATING %>">Gian lận</option>
                        <option value="<%= ReportReason.OTHER %>">Khác</option>
                    </select>
                </div>
            </div>

            <div class="form-group">
                <label for="description">Mô tả thêm (tùy chọn)</label>
                <textarea id="description" name="description" maxlength="1000" placeholder="Mô tả chi tiết vấn đề..."></textarea>
            </div>

            <button type="submit" class="btn-submit">Gửi báo cáo</button>
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
