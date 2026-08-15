<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.Map" %>
<%@ page import="com.gamenest.model.SystemSetting" %>
<%@ page import="com.gamenest.model.SystemSettingKey" %>
<%@ page import="com.gamenest.util.HtmlUtils" %>
<% request.setAttribute("pageTitle", "Cấu hình hệ thống"); %>
<%@ include file="/WEB-INF/admin/layout/header.jsp" %>

<%
    @SuppressWarnings("unchecked")
    Map<String, SystemSetting> settings = (Map<String, SystemSetting>) request.getAttribute("settings");
    if (settings == null) {
        settings = java.util.Collections.emptyMap();
    }

    String siteName = settings.containsKey(SystemSettingKey.SITE_NAME)
            ? settings.get(SystemSettingKey.SITE_NAME).getSettingValue() : "";
    String siteDescription = settings.containsKey(SystemSettingKey.SITE_DESCRIPTION)
            ? settings.get(SystemSettingKey.SITE_DESCRIPTION).getSettingValue() : "";
    String systemStatus = settings.containsKey(SystemSettingKey.SYSTEM_STATUS)
            ? settings.get(SystemSettingKey.SYSTEM_STATUS).getSettingValue() : "ONLINE";

    String registrationEnabled = settings.containsKey(SystemSettingKey.REGISTRATION_ENABLED)
            ? settings.get(SystemSettingKey.REGISTRATION_ENABLED).getSettingValue() : "true";
    String questionsEnabled = settings.containsKey(SystemSettingKey.QUESTIONS_ENABLED)
            ? settings.get(SystemSettingKey.QUESTIONS_ENABLED).getSettingValue() : "true";
    String answersEnabled = settings.containsKey(SystemSettingKey.ANSWERS_ENABLED)
            ? settings.get(SystemSettingKey.ANSWERS_ENABLED).getSettingValue() : "true";
    String reportsEnabled = settings.containsKey(SystemSettingKey.REPORTS_ENABLED)
            ? settings.get(SystemSettingKey.REPORTS_ENABLED).getSettingValue() : "true";

    String otpExpiration = settings.containsKey(SystemSettingKey.OTP_EXPIRATION_MINUTES)
            ? settings.get(SystemSettingKey.OTP_EXPIRATION_MINUTES).getSettingValue() : "5";
    String otpCooldown = settings.containsKey(SystemSettingKey.OTP_RESEND_COOLDOWN_SECONDS)
            ? settings.get(SystemSettingKey.OTP_RESEND_COOLDOWN_SECONDS).getSettingValue() : "60";
    String otpMaxAttempts = settings.containsKey(SystemSettingKey.OTP_MAX_ATTEMPTS)
            ? settings.get(SystemSettingKey.OTP_MAX_ATTEMPTS).getSettingValue() : "5";
%>

<div style="max-width: 820px; margin: 0 auto; padding-bottom: 40px;">
    <div class="admin-page-header">
        <h1>Cấu hình hệ thống</h1>
    </div>

    <% if (request.getParameter("updated") != null) { %>
        <p style="color: var(--success-color); margin-bottom: 16px;">Đã lưu thay đổi thành công.</p>
    <% } %>
    <% if (request.getAttribute("error") != null) { %>
        <p style="color: var(--error-color); margin-bottom: 16px;"><%= HtmlUtils.escape((String) request.getAttribute("error")) %></p>
    <% } %>

    <div class="admin-detail-card" style="margin-bottom: 24px;">
        <h2 style="margin-top: 0; display: flex; align-items: center; gap: 8px;">
            <span class="material-symbols-outlined" style="color: var(--primary-color, #a855f7);">tune</span>
            <span>Cấu hình chung</span>
        </h2>
        <form action="${pageContext.request.contextPath}/admin/settings" method="post">
            <div class="admin-form-group">
                <label for="siteName">Tên website / Hệ thống</label>
                <input type="text" id="siteName" name="<%= SystemSettingKey.SITE_NAME %>"
                       value="<%= HtmlUtils.escape(siteName) %>" maxlength="500" required>
            </div>
            <div class="admin-form-group">
                <label for="siteDescription">Mô tả website</label>
                <textarea id="siteDescription" name="<%= SystemSettingKey.SITE_DESCRIPTION %>" maxlength="500"><%= HtmlUtils.escape(siteDescription) %></textarea>
            </div>
            <div class="admin-form-group">
                <label for="systemStatus">Trạng thái hệ thống</label>
                <select id="systemStatus" name="<%= SystemSettingKey.SYSTEM_STATUS %>">
                    <option value="ONLINE" <%= "ONLINE".equals(systemStatus) ? "selected" : "" %>>Hoạt động (ONLINE)</option>
                    <option value="MAINTENANCE" <%= "MAINTENANCE".equals(systemStatus) ? "selected" : "" %>>Bảo trì (MAINTENANCE)</option>
                </select>
            </div>
            <button type="submit" class="admin-btn">Lưu thay đổi</button>
        </form>
    </div>

    <div class="admin-detail-card" style="margin-bottom: 24px;">
        <h2 style="margin-top: 0; display: flex; align-items: center; gap: 8px;">
            <span class="material-symbols-outlined" style="color: var(--primary-color, #a855f7);">groups</span>
            <span>Người dùng &amp; Cộng đồng</span>
        </h2>
        <form action="${pageContext.request.contextPath}/admin/settings" method="post">
            <div class="admin-form-group">
                <label for="registrationEnabled">Cho phép đăng ký tài khoản mới</label>
                <select id="registrationEnabled" name="<%= SystemSettingKey.REGISTRATION_ENABLED %>">
                    <option value="true" <%= "true".equals(registrationEnabled) ? "selected" : "" %>>Bật</option>
                    <option value="false" <%= "false".equals(registrationEnabled) ? "selected" : "" %>>Tắt</option>
                </select>
            </div>
            <div class="admin-form-group">
                <label for="questionsEnabled">Cho phép đặt câu hỏi</label>
                <select id="questionsEnabled" name="<%= SystemSettingKey.QUESTIONS_ENABLED %>">
                    <option value="true" <%= "true".equals(questionsEnabled) ? "selected" : "" %>>Bật</option>
                    <option value="false" <%= "false".equals(questionsEnabled) ? "selected" : "" %>>Tắt</option>
                </select>
            </div>
            <div class="admin-form-group">
                <label for="answersEnabled">Cho phép trả lời câu hỏi</label>
                <select id="answersEnabled" name="<%= SystemSettingKey.ANSWERS_ENABLED %>">
                    <option value="true" <%= "true".equals(answersEnabled) ? "selected" : "" %>>Bật</option>
                    <option value="false" <%= "false".equals(answersEnabled) ? "selected" : "" %>>Tắt</option>
                </select>
            </div>
            <div class="admin-form-group">
                <label for="reportsEnabled">Cho phép gửi báo cáo vi phạm</label>
                <select id="reportsEnabled" name="<%= SystemSettingKey.REPORTS_ENABLED %>">
                    <option value="true" <%= "true".equals(reportsEnabled) ? "selected" : "" %>>Bật</option>
                    <option value="false" <%= "false".equals(reportsEnabled) ? "selected" : "" %>>Tắt</option>
                </select>
            </div>
            <button type="submit" class="admin-btn">Lưu thay đổi</button>
        </form>
    </div>

    <div class="admin-detail-card" style="margin-bottom: 24px;">
        <h2 style="margin-top: 0; display: flex; align-items: center; gap: 8px;">
            <span class="material-symbols-outlined" style="color: var(--primary-color, #a855f7);">lock</span>
            <span>Bảo mật &amp; Mã OTP</span>
        </h2>
        <form action="${pageContext.request.contextPath}/admin/settings" method="post">
            <div class="admin-form-group">
                <label for="otpExpiration">Thời hạn mã OTP (phút)</label>
                <input type="number" id="otpExpiration" name="<%= SystemSettingKey.OTP_EXPIRATION_MINUTES %>"
                       value="<%= HtmlUtils.escape(otpExpiration) %>" min="1" required>
            </div>
            <div class="admin-form-group">
                <label for="otpCooldown">Thời gian chờ gửi lại OTP (giây)</label>
                <input type="number" id="otpCooldown" name="<%= SystemSettingKey.OTP_RESEND_COOLDOWN_SECONDS %>"
                       value="<%= HtmlUtils.escape(otpCooldown) %>" min="1" required>
            </div>
            <div class="admin-form-group">
                <label for="otpMaxAttempts">Số lần nhập sai tối đa</label>
                <input type="number" id="otpMaxAttempts" name="<%= SystemSettingKey.OTP_MAX_ATTEMPTS %>"
                       value="<%= HtmlUtils.escape(otpMaxAttempts) %>" min="1" required>
            </div>
            <button type="submit" class="admin-btn">Lưu thay đổi</button>
        </form>
    </div>
</div>

<%@ include file="/WEB-INF/admin/layout/footer.jsp" %>
