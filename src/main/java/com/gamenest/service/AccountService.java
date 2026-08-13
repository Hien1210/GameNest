package com.gamenest.service;

import com.gamenest.dao.AccountDAO;
import com.gamenest.dto.PendingRegistration;
import com.gamenest.exception.AccountNotFoundException;
import com.gamenest.exception.AccountStatusException;
import com.gamenest.exception.AuthenticationException;
import com.gamenest.exception.DuplicateAccountException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.Account;
import com.gamenest.model.AccountStatus;
import com.gamenest.util.CloudinaryUploader;
import jakarta.servlet.http.Part;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class AccountService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,50}$");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int DISPLAY_NAME_MAX_LENGTH = 100;
    private static final int ADMIN_PAGE_SIZE = 20;
    private static final long AVATAR_MAX_SIZE_BYTES = 2 * 1024 * 1024;
    private static final Map<String, String> AVATAR_EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

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

    // ---- Admin ----

    public List<Account> listAllAccountsForAdmin(int page) throws SQLException {
        int offset = (clampPage(page) - 1) * ADMIN_PAGE_SIZE;
        return accountDAO.findAllForAdmin(offset, ADMIN_PAGE_SIZE);
    }

    public int countAllAccountsForAdmin() throws SQLException {
        return accountDAO.countAllForAdmin();
    }

    public List<Account> searchAccountsForAdmin(String query, int page) throws SQLException {
        int offset = (clampPage(page) - 1) * ADMIN_PAGE_SIZE;
        return accountDAO.searchForAdmin(query == null ? "" : query.trim(), offset, ADMIN_PAGE_SIZE);
    }

    public int countSearchAccountsForAdmin(String query) throws SQLException {
        return accountDAO.countSearchForAdmin(query == null ? "" : query.trim());
    }

    public int getAdminPageSize() {
        return ADMIN_PAGE_SIZE;
    }

    public Account getAccountForAdmin(int accountId) throws AccountNotFoundException, SQLException {
        return accountDAO.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Tài khoản không tồn tại."));
    }

    // ---- Personal Profile (self-service) ----

    /**
     * Looks up the profile of the currently logged-in account. The caller
     * must pass the accountId taken from the session — never from a request
     * parameter — so a user can only ever load their own profile.
     */
    public Account getOwnProfile(int accountId) throws AccountNotFoundException, SQLException {
        return accountDAO.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Tài khoản không tồn tại."));
    }

    /**
     * Updates the display name of the currently logged-in account. This is
     * the only field Profile is allowed to edit — username, email, role,
     * status and password all stay read-only here. Reuses the same
     * validation rule as registration; an empty value falls back to the
     * account's username, matching {@link #prepareRegistration}.
     */
    public void updateOwnDisplayName(int accountId, String newDisplayName)
            throws AccountNotFoundException, ValidationException, SQLException {

        String trimmed = newDisplayName == null ? null : newDisplayName.trim();
        validateDisplayName(trimmed);

        Account account = accountDAO.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Tài khoản không tồn tại."));

        String finalName = (trimmed == null || trimmed.isEmpty()) ? account.getUsername() : trimmed;
        int updated = accountDAO.updateDisplayName(accountId, finalName);
        if (updated == 0) {
            throw new AccountNotFoundException("Tài khoản không tồn tại.");
        }
    }

    /**
     * Uploads and stores a new avatar image for the currently logged-in
     * account. The content-type is validated against a strict whitelist
     * (never trusting the client-submitted filename) and the size is
     * re-checked here as defense-in-depth alongside the servlet's
     * {@code @MultipartConfig} limit.
     */
    public String updateOwnAvatar(int accountId, Part avatarFile)
            throws AccountNotFoundException, ValidationException, SQLException, IOException {

        if (avatarFile.getSize() > AVATAR_MAX_SIZE_BYTES) {
            throw new ValidationException("Ảnh đại diện không được vượt quá 2 MB.");
        }

        String extension = AVATAR_EXTENSIONS_BY_CONTENT_TYPE.get(avatarFile.getContentType());
        if (extension == null) {
            throw new ValidationException("Ảnh đại diện phải là định dạng JPEG, PNG hoặc WEBP.");
        }

        accountDAO.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Tài khoản không tồn tại."));

        String avatarUrl;
        try (var in = avatarFile.getInputStream()) {
            avatarUrl = CloudinaryUploader.uploadAvatar(accountId, in);
        }

        int updated = accountDAO.updateAvatarUrl(accountId, avatarUrl);
        if (updated == 0) {
            throw new AccountNotFoundException("Tài khoản không tồn tại.");
        }

        return avatarUrl;
    }

    /**
     * Changes an account's lifecycle status. Status is validated against the
     * known enum values rather than trusted verbatim from the caller, and an
     * admin may never change the status of the account they are currently
     * logged in as — this would risk an accidental self-lockout and there is
     * no approved business rule allowing it.
     */
    public void changeAccountStatus(int accountId, String newStatus, int actingAdminAccountId)
            throws AccountNotFoundException, ValidationException, SQLException {

        if (accountId == actingAdminAccountId) {
            throw new ValidationException("Không thể tự thay đổi trạng thái của chính tài khoản đang đăng nhập.");
        }

        String validStatus = validateAccountStatus(newStatus);

        int updated = accountDAO.updateStatus(accountId, validStatus);
        if (updated == 0) {
            throw new AccountNotFoundException("Tài khoản không tồn tại.");
        }
    }

    private String validateAccountStatus(String status) throws ValidationException {
        if (!AccountStatus.ACTIVE.equals(status) && !AccountStatus.BANNED.equals(status)
                && !AccountStatus.SUSPENDED.equals(status) && !AccountStatus.DELETED.equals(status)) {
            throw new ValidationException("Trạng thái tài khoản không hợp lệ.");
        }
        return status;
    }

    private int clampPage(int page) {
        return Math.max(page, 1);
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
