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
    String sessionRole = (String) session.getAttribute("role");
    boolean isAdmin = AccountRole.ADMIN.equals(sessionRole);
    boolean isModerator = AccountRole.MODERATOR.equals(sessionRole);
    boolean updated = request.getParameter("updated") != null;
%>
<% if (isAdmin) {
    request.setAttribute("pageTitle", "Hồ sơ cá nhân");
%>
<%@ include file="/WEB-INF/admin/layout/header.jsp" %>
<% } else if (isModerator) {
    request.setAttribute("pageTitle", "Hồ sơ cá nhân");
%>
<%@ include file="/WEB-INF/moderator/layout/header.jsp" %>
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
            <div class="profile-avatar-wrapper" id="profileAvatarContainer">
                <% if (account.getAvatarUrl() != null && !account.getAvatarUrl().isEmpty()) { %>
                <img class="profile-avatar-img" id="avatarPreview" src="<%= HtmlUtils.escape(account.getAvatarUrl()) %>" alt="Avatar">
                <% } else { %>
                <span class="profile-avatar-circle" id="avatarInitial" aria-hidden="true"><%= initial %></span>
                <% } %>
            </div>
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
                
                <!-- Thanh tiến trình tải ảnh (Loading Bar) -->
                <div id="avatarProgressContainer" class="avatar-progress-container" style="display: none;">
                    <div class="avatar-progress-info">
                        <span id="avatarProgressStatus" class="avatar-progress-status">Đang tải ảnh...</span>
                        <span id="avatarProgressPercent" class="avatar-progress-percent">0%</span>
                    </div>
                    <div class="avatar-progress-track">
                        <div id="avatarProgressBar" class="avatar-progress-bar"></div>
                    </div>
                </div>
                <div id="avatarFileError" class="avatar-file-error" style="display: none;"></div>
            </div>
            <button type="submit" class="btn-submit">Lưu thay đổi</button>
        </form>
        <% } %>
    </div>
</div>

<script>
document.addEventListener('DOMContentLoaded', function() {
    var avatarInput = document.getElementById('avatarFile');
    if (!avatarInput) return;

    var progressContainer = document.getElementById('avatarProgressContainer');
    var progressBar = document.getElementById('avatarProgressBar');
    var progressStatus = document.getElementById('avatarProgressStatus');
    var progressPercent = document.getElementById('avatarProgressPercent');
    var errorContainer = document.getElementById('avatarFileError');
    var avatarContainer = document.getElementById('profileAvatarContainer');
    var progressTimer = null;

    avatarInput.addEventListener('change', function(e) {
        if (progressTimer) clearInterval(progressTimer);
        errorContainer.style.display = 'none';
        errorContainer.textContent = '';

        var file = e.target.files && e.target.files[0];
        if (!file) {
            progressContainer.style.display = 'none';
            return;
        }

        // Kiểm tra định dạng ảnh
        var validTypes = ['image/jpeg', 'image/png', 'image/webp'];
        if (validTypes.indexOf(file.type) === -1) {
            errorContainer.innerHTML = '<span class="material-symbols-outlined" style="font-size: 18px;">error</span> Định dạng ảnh không hợp lệ (chỉ hỗ trợ JPG, PNG, WEBP).';
            errorContainer.style.display = 'flex';
            progressContainer.style.display = 'none';
            avatarInput.value = '';
            return;
        }

        // Kiểm tra dung lượng (tối đa 2MB)
        var maxBytes = 2 * 1024 * 1024;
        if (file.size > maxBytes) {
            errorContainer.innerHTML = '<span class="material-symbols-outlined" style="font-size: 18px;">error</span> Dung lượng ảnh quá lớn (tối đa 2 MB).';
            errorContainer.style.display = 'flex';
            progressContainer.style.display = 'none';
            avatarInput.value = '';
            return;
        }

        // Bắt đầu hiệu ứng loading bar
        progressContainer.style.display = 'block';
        progressBar.style.width = '0%';
        progressPercent.textContent = '0%';
        progressStatus.className = 'avatar-progress-status';
        progressStatus.innerHTML = '<span class="material-symbols-outlined" style="font-size: 16px; animation: spin 0.8s linear infinite;">sync</span> Đang đọc tệp tin...';
        if (avatarContainer) avatarContainer.classList.add('loading');

        var reader = new FileReader();
        var progress = 0;

        // Mô phỏng tiến trình mượt mà
        progressTimer = setInterval(function() {
            progress += Math.floor(Math.random() * 15) + 12;
            if (progress >= 92) {
                progress = 92;
                clearInterval(progressTimer);
            }
            progressBar.style.width = progress + '%';
            progressPercent.textContent = progress + '%';
        }, 60);

        reader.onload = function(event) {
            clearInterval(progressTimer);
            progressBar.style.width = '100%';
            progressPercent.textContent = '100%';
            progressStatus.className = 'avatar-progress-status success';
            progressStatus.innerHTML = '<span class="material-symbols-outlined" style="font-size: 18px; color: #10b981;">check_circle</span> Đã tải ảnh xong';
            
            if (avatarContainer) {
                avatarContainer.classList.remove('loading');
                // Cập nhật ngay ảnh xem trước
                var existingImg = document.getElementById('avatarPreview');
                var existingCircle = document.getElementById('avatarInitial');
                if (existingImg) {
                    existingImg.src = event.target.result;
                } else if (existingCircle) {
                    var newImg = document.createElement('img');
                    newImg.id = 'avatarPreview';
                    newImg.className = 'profile-avatar-img';
                    newImg.src = event.target.result;
                    newImg.alt = 'Avatar';
                    existingCircle.parentNode.replaceChild(newImg, existingCircle);
                }
            }
        };

        reader.readAsDataURL(file);
    });
});
</script>

<% if (isAdmin) { %>
<%@ include file="/WEB-INF/admin/layout/footer.jsp" %>
<% } else if (isModerator) { %>
<%@ include file="/WEB-INF/moderator/layout/footer.jsp" %>
<% } else { %>
</body>
</html>
<% } %>
