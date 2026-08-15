package com.gamenest.controller.admin;

import com.gamenest.dto.SettingChange;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.AuditAction;
import com.gamenest.model.AuditModule;
import com.gamenest.model.AuditTargetType;
import com.gamenest.model.SystemSetting;
import com.gamenest.model.SystemSettingKey;
import com.gamenest.service.AuditLogService;
import com.gamenest.service.SystemSettingsService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Admin System Settings (Cấu hình hệ thống) — /admin/settings. Access is
 * gated by {@link com.gamenest.filter.AdminAuthorizationFilter} on
 * /admin/*. The acting admin's identity is always read from the session,
 * never from a request parameter (task spec §13).
 */
@WebServlet(name = "AdminSystemSettingsServlet", urlPatterns = {"/admin/settings"})
public class AdminSystemSettingsServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdminSystemSettingsServlet.class.getName());
    private static final String VIEW = "/admin/settings.jsp";

    private final SystemSettingsService systemSettingsService = new SystemSettingsService();
    private final AuditLogService auditLogService = new AuditLogService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        loadAndForward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        Object adminIdAttr = session == null ? null : session.getAttribute("accountId");
        if (!(adminIdAttr instanceof Integer)) {
            response.sendRedirect(request.getContextPath() + "/account/login.jsp");
            return;
        }
        int adminAccountId = (Integer) adminIdAttr;

        Map<String, String> submitted = new LinkedHashMap<>();
        for (String key : SystemSettingKey.ALL_KEYS) {
            String value = request.getParameter(key);
            if (value != null) {
                submitted.put(key, value);
            }
        }

        try {
            List<SettingChange> changes = systemSettingsService.updateSettings(submitted, adminAccountId);

            for (SettingChange change : changes) {
                String description = "đã cập nhật \"" + change.getKey() + "\" từ \"" + change.getOldValue()
                        + "\" thành \"" + change.getNewValue() + "\".";
                auditLogService.log(request, AuditModule.SETTINGS, AuditAction.UPDATE,
                        change.getSettingId(), AuditTargetType.SETTING, description);
            }

            response.sendRedirect(request.getContextPath() + "/admin/settings?updated=1");

        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            loadAndForward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while updating system settings", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            loadAndForward(request, response);
        }
    }

    private void loadAndForward(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            List<SystemSetting> all = systemSettingsService.getAllSettings();
            Map<String, SystemSetting> byKey = new LinkedHashMap<>();
            for (SystemSetting setting : all) {
                byKey.put(setting.getSettingKey(), setting);
            }
            request.setAttribute("settings", byKey);
            request.getRequestDispatcher(VIEW).forward(request, response);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while loading system settings", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.setAttribute("settings", new LinkedHashMap<String, SystemSetting>());
            request.getRequestDispatcher(VIEW).forward(request, response);
        }
    }
}
