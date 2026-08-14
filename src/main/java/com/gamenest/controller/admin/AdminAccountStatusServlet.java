package com.gamenest.controller.admin;

import com.gamenest.exception.AccountNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.Account;
import com.gamenest.model.AccountStatus;
import com.gamenest.model.AuditAction;
import com.gamenest.model.AuditModule;
import com.gamenest.model.AuditTargetType;
import com.gamenest.service.AccountService;
import com.gamenest.service.AuditLogService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Status change only — Accounts are core business data and must never be
 * hard-deleted (see CLAUDE.md §7.1). There is no delete endpoint. The acting
 * admin's identity is always read from the session, never from a request
 * parameter, so a caller cannot spoof who performed the action.
 */
@WebServlet(name = "AdminAccountStatusServlet", urlPatterns = {"/admin/accounts/status"})
public class AdminAccountStatusServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdminAccountStatusServlet.class.getName());
    private static final String LIST_VIEW = "/admin/accounts/list.jsp";
    private static final String DETAIL_VIEW = "/admin/accounts/detail.jsp";

    private final AccountService accountService = new AccountService();
    private final AuditLogService auditLogService = new AuditLogService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int accountId = parseId(request.getParameter("id"));
        String targetStatus = mapAction(request.getParameter("action"));

        HttpSession session = request.getSession(false);
        Object actingAdminId = session == null ? null : session.getAttribute("accountId");

        if (targetStatus == null || !(actingAdminId instanceof Integer)) {
            request.setAttribute("error", "Hành động không hợp lệ.");
            request.getRequestDispatcher(LIST_VIEW).forward(request, response);
            return;
        }

        try {
            Account before = accountService.getAccountForAdmin(accountId);
            String oldStatus = before.getStatus();

            accountService.changeAccountStatus(accountId, targetStatus, (Integer) actingAdminId);

            String description = "đã thay đổi trạng thái Account \"" + before.getUsername()
                    + "\" từ " + oldStatus + " sang " + targetStatus + ".";
            auditLogService.log(request, AuditModule.ACCOUNTS, AuditAction.STATUS_CHANGE,
                    accountId, AuditTargetType.ACCOUNT, description);

            response.sendRedirect(request.getContextPath() + "/admin/accounts/detail?id=" + accountId);

        } catch (AccountNotFoundException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(LIST_VIEW).forward(request, response);

        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            forwardToDetail(request, response, accountId, (Integer) actingAdminId);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while changing account status", e);
            request.setAttribute("error", "Đã có lỗi xảy ra, vui lòng thử lại sau.");
            request.getRequestDispatcher(LIST_VIEW).forward(request, response);
        }
    }

    /**
     * Re-fetches the account so the detail page can re-render with the error
     * message; falls back to the list page if the account can no longer be
     * loaded.
     */
    private void forwardToDetail(HttpServletRequest request, HttpServletResponse response,
                                  int accountId, int actingAdminId)
            throws ServletException, IOException {
        try {
            Account account = accountService.getAccountForAdmin(accountId);
            request.setAttribute("account", account);
            request.setAttribute("isSelf", account.getAccountId() == actingAdminId);
            request.getRequestDispatcher(DETAIL_VIEW).forward(request, response);
        } catch (AccountNotFoundException | SQLException e) {
            request.getRequestDispatcher(LIST_VIEW).forward(request, response);
        }
    }

    private String mapAction(String action) {
        if (action == null) {
            return null;
        }
        return switch (action) {
            case "activate" -> AccountStatus.ACTIVE;
            case "ban" -> AccountStatus.BANNED;
            case "suspend" -> AccountStatus.SUSPENDED;
            case "delete" -> AccountStatus.DELETED;
            default -> null;
        };
    }

    private int parseId(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException | NullPointerException e) {
            return -1;
        }
    }
}
