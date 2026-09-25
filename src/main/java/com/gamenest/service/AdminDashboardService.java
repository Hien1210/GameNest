package com.gamenest.service;

import com.gamenest.dao.AdminDashboardDAO;
import com.gamenest.dto.AdminDashboardStats;

import java.sql.SQLException;

public class AdminDashboardService {

    private final AdminDashboardDAO dashboardDAO;

    public AdminDashboardService() {
        this.dashboardDAO = new AdminDashboardDAO();
    }

    public AdminDashboardService(AdminDashboardDAO dashboardDAO) {
        this.dashboardDAO = dashboardDAO;
    }

    public AdminDashboardStats getStats() throws SQLException {
        AdminDashboardStats stats = new AdminDashboardStats();
        stats.setTotalAccounts(dashboardDAO.countAccounts());
        dashboardDAO.fillGameStats(stats);
        dashboardDAO.fillQuestionStats(stats);
        dashboardDAO.fillAnswerStats(stats);
        return stats;
    }
}
