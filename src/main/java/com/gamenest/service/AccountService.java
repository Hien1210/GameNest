package com.gamenest.service;

import com.gamenest.dao.AccountDAO;
import com.gamenest.dto.PendingRegistration;
import com.gamenest.exception.AccountStatusException;
import com.gamenest.exception.AuthenticationException;
import com.gamenest.exception.DuplicateAccountException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.Account;
import com.gamenest.model.AccountStatus;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.Optional;
import java.util.regex.Pattern;

public class AccountService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,50}$");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int DISPLAY_NAME_MAX_LENGTH = 100;

    private final AccountDAO accountDAO;

    public AccountService() {
        this.accountDAO = new AccountDAO();
    }

    public AccountService(AccountDAO accountDAO) {
        this.accountDAO = accountDAO;
    }

    /**
     * Validates registration input and checks for duplicates, but does NOT
     * touch the database yet — the account is only created after the OTP
     * sent to the email is verified (see {@link #createAccount}). Only
     * username/email/password/displayName are accepted; status is decided
     * solely by this service, never by the caller.
     */
    public PendingRegistration prepareRegistration(String username, String email, String rawPassword,
                                                     String displayName)
            throws ValidationException, DuplicateAccountException, SQLException {

        username = username == null ? null : username.trim();
        email = email == null ? null : email.trim().toLowerCase();
        displayName = displayName == null ? null : displayName.trim();

        validateUsername(username);
        validateEmail(email);
        validatePassword(rawPassword);
        validateDisplayName(displayName);

        if (accountDAO.findByUsername(username).isPresent()) {
            throw new DuplicateAccountException("Username đã được sử dụng.");
        }
        if (accountDAO.findByEmail(email).isPresent()) {
            throw new DuplicateAccountException("Email đã được sử dụng.");
        }

        String passwordHash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
        String finalDisplayName = (displayName == null || displayName.isEmpty()) ? username : displayName;

        return new PendingRegistration(username, email, passwordHash, finalDisplayName);
    }

    /**
     * Persists a registration that has already passed OTP verification.
     * The database UNIQUE constraints on username/email remain the final
     * safety net against a duplicate created in the time between
     * {@link #prepareRegistration} and this call.
     */
    public Account createAccount(PendingRegistration pending) throws DuplicateAccountException, SQLException {
        Account account = new Account();
        account.setUsername(pending.getUsername());
        account.setEmail(pending.getEmail());
        account.setPasswordHash(pending.getPasswordHash());
        account.setDisplayName(pending.getDisplayName());
        account.setStatus(AccountStatus.ACTIVE);

        return accountDAO.insert(account);
    }

    /**
     * Authenticates a user by username or email. Not-found and wrong-password
     * both raise AuthenticationException with the same generic message so the
     * caller cannot use the error to enumerate registered accounts.
     */
    public Account login(String usernameOrEmail, String rawPassword)
            throws AuthenticationException, AccountStatusException, SQLException {

        if (usernameOrEmail == null || usernameOrEmail.isBlank() || rawPassword == null || rawPassword.isEmpty()) {
            throw new AuthenticationException("Tên đăng nhập hoặc mật khẩu không đúng.");
        }

        Optional<Account> found = accountDAO.findByUsernameOrEmail(usernameOrEmail.trim());
        if (found.isEmpty() || !BCrypt.checkpw(rawPassword, found.get().getPasswordHash())) {
            throw new AuthenticationException("Tên đăng nhập hoặc mật khẩu không đúng.");
        }

        Account account = found.get();
        if (!AccountStatus.ACTIVE.equals(account.getStatus())) {
            throw new AccountStatusException(statusMessage(account.getStatus()));
        }

        return account;
    }

    /**
     * Looks up an account by email for the forgot-password flow. Callers
     * must not reveal to the end user whether the email was found — always
     * respond with the same generic message regardless of the result, so an
     * attacker cannot use this to enumerate registered emails.
     */
    public Optional<Account> findByEmail(String email) throws SQLException {
        return accountDAO.findByEmail(email == null ? null : email.trim().toLowerCase());
    }

    /**
     * Sets a new password for an account. Caller is responsible for having
     * already verified an OTP for this email before calling this method.
     */
    public void resetPassword(String email, String newRawPassword) throws ValidationException, SQLException {
        validatePassword(newRawPassword);
        String hash = BCrypt.hashpw(newRawPassword, BCrypt.gensalt());
        accountDAO.updatePasswordByEmail(email.trim().toLowerCase(), hash);
    }

    private String statusMessage(String status) {
        return switch (status) {
            case AccountStatus.BANNED -> "Tài khoản đã bị cấm.";
            case AccountStatus.SUSPENDED -> "Tài khoản đang bị tạm khóa.";
            case AccountStatus.DELETED -> "Tài khoản không tồn tại.";
            default -> "Tài khoản không thể đăng nhập.";
        };
    }

    private void validateUsername(String username) throws ValidationException {
        if (username == null || username.isEmpty()) {
            throw new ValidationException("Username không được để trống.");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new ValidationException("Username phải dài 3-50 ký tự, chỉ gồm chữ, số và dấu gạch dưới.");
        }
    }

    private void validateEmail(String email) throws ValidationException {
        if (email == null || email.isEmpty()) {
            throw new ValidationException("Email không được để trống.");
        }
        if (email.length() > 255 || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Email không hợp lệ.");
        }
    }

    private void validatePassword(String password) throws ValidationException {
        if (password == null || password.length() < PASSWORD_MIN_LENGTH) {
            throw new ValidationException("Mật khẩu phải có ít nhất " + PASSWORD_MIN_LENGTH + " ký tự.");
        }
    }

    private void validateDisplayName(String displayName) throws ValidationException {
        if (displayName != null && displayName.length() > DISPLAY_NAME_MAX_LENGTH) {
            throw new ValidationException("Tên hiển thị không được vượt quá " + DISPLAY_NAME_MAX_LENGTH + " ký tự.");
        }
    }
}
