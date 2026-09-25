package com.gamenest.dto;

/**
 * Read-only aggregate counts for the Admin Dashboard. Populated entirely
 * from SQL COUNT()/aggregate queries — never hand-built or hard-coded.
 */
public class AdminDashboardStats {

    private long totalAccounts;

    private long totalGames;
    private long activeGames;
    private long inactiveGames;

    private long totalQuestions;
    private long activeQuestions;
    private long deletedQuestions;

    private long totalAnswers;
    private long activeAnswers;
    private long deletedAnswers;

    public long getTotalAccounts() {
        return totalAccounts;
    }

    public void setTotalAccounts(long totalAccounts) {
        this.totalAccounts = totalAccounts;
    }

    public long getTotalGames() {
        return totalGames;
    }

    public void setTotalGames(long totalGames) {
        this.totalGames = totalGames;
    }

    public long getActiveGames() {
        return activeGames;
    }

    public void setActiveGames(long activeGames) {
        this.activeGames = activeGames;
    }

    public long getInactiveGames() {
        return inactiveGames;
    }

    public void setInactiveGames(long inactiveGames) {
        this.inactiveGames = inactiveGames;
    }

    public long getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(long totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public long getActiveQuestions() {
        return activeQuestions;
    }

    public void setActiveQuestions(long activeQuestions) {
        this.activeQuestions = activeQuestions;
    }

    public long getDeletedQuestions() {
        return deletedQuestions;
    }

    public void setDeletedQuestions(long deletedQuestions) {
        this.deletedQuestions = deletedQuestions;
    }

    public long getTotalAnswers() {
        return totalAnswers;
    }

    public void setTotalAnswers(long totalAnswers) {
        this.totalAnswers = totalAnswers;
    }

    public long getActiveAnswers() {
        return activeAnswers;
    }

    public void setActiveAnswers(long activeAnswers) {
        this.activeAnswers = activeAnswers;
    }

    public long getDeletedAnswers() {
        return deletedAnswers;
    }

    public void setDeletedAnswers(long deletedAnswers) {
        this.deletedAnswers = deletedAnswers;
    }
}
