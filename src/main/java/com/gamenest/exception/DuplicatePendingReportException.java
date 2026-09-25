package com.gamenest.exception;

/**
 * Thrown when the reporter already has a PENDING Report for the same
 * target — anti-spam rule (task spec §4). Also thrown by
 * {@link com.gamenest.dao.ReportDAO#insert} if the DB's filtered unique
 * index (UQ_Reports_pending_target) rejects a concurrent duplicate that
 * slipped past the Service-layer pre-check.
 */
public class DuplicatePendingReportException extends Exception {
    public DuplicatePendingReportException(String message) {
        super(message);
    }
}
