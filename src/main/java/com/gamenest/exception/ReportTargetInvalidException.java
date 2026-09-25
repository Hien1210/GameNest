package com.gamenest.exception;

/**
 * Thrown when a Report's target (ACCOUNT/QUESTION/ANSWER) does not exist,
 * is not in a reportable state, or is the reporter's own content/account.
 */
public class ReportTargetInvalidException extends Exception {
    public ReportTargetInvalidException(String message) {
        super(message);
    }
}
