package com.gamenest.service;

import com.gamenest.dao.OtpDAO;
import com.gamenest.exception.OtpException;
import com.gamenest.model.OtpPurpose;
import com.gamenest.model.OtpRecord;
import com.gamenest.model.SystemSettingKey;
import com.gamenest.util.MailSender;
import jakarta.mail.MessagingException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public class OtpService {

    private static final int OTP_LENGTH_DIGITS = 6;

    // Fallback values used if System Settings is unreadable (missing row,
    // DB error) — identical to the values this service hard-coded before
    // System Settings existed, so a Settings problem can never break OTP.
    private static final int DEFAULT_OTP_TTL_MINUTES = 5;
    private static final int DEFAULT_RESEND_COOLDOWN_SECONDS = 60;
    private static final int DEFAULT_MAX_ATTEMPTS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpDAO otpDAO;
    private final SystemSettingsService systemSettingsService;

    public OtpService() {
        this.otpDAO = new OtpDAO();
        this.systemSettingsService = new SystemSettingsService();
    }

    public OtpService(OtpDAO otpDAO) {
        this.otpDAO = otpDAO;
        this.systemSettingsService = new SystemSettingsService();
    }

    /**
     * Generates a new OTP, stores its hash, and emails it. Rejects requests
     * made too soon after a still-valid OTP was already issued for the same
     * email + purpose, to prevent mailbox/SMTP-quota abuse.
     */
    public void generateAndSend(String email, String purpose)
            throws OtpException, SQLException, MessagingException {

        Duration resendCooldown = Duration.ofSeconds(systemSettingsService.getIntSetting(
                SystemSettingKey.OTP_RESEND_COOLDOWN_SECONDS, DEFAULT_RESEND_COOLDOWN_SECONDS));
        Duration otpTtl = Duration.ofMinutes(systemSettingsService.getIntSetting(
                SystemSettingKey.OTP_EXPIRATION_MINUTES, DEFAULT_OTP_TTL_MINUTES));

        Optional<OtpRecord> active = otpDAO.findLatestActive(email, purpose);
        if (active.isPresent()) {
            Duration since = Duration.between(active.get().getCreatedAt(), LocalDateTime.now());
            if (since.compareTo(resendCooldown) < 0) {
                long waitSeconds = resendCooldown.minus(since).toSeconds();
                throw new OtpException("Vui lòng đợi " + waitSeconds + " giây trước khi yêu cầu gửi lại OTP.");
            }
        }

        String code = generateCode();
        String hash = hash(code);
        LocalDateTime expiresAt = LocalDateTime.now().plus(otpTtl);

        otpDAO.insert(email, purpose, hash, expiresAt);

        MailSender.send(email, emailSubject(purpose), emailBody(purpose, code, otpTtl));
    }

    /**
     * Verifies a submitted code against the latest active OTP for the given
     * email + purpose. Consumes (marks used) the OTP on success so it cannot
     * be replayed.
     */
    public void verify(String email, String purpose, String submittedCode) throws OtpException, SQLException {
        if (submittedCode == null || submittedCode.isBlank()) {
            throw new OtpException("Vui lòng nhập mã OTP.");
        }

        int maxAttempts = systemSettingsService.getIntSetting(
                SystemSettingKey.OTP_MAX_ATTEMPTS, DEFAULT_MAX_ATTEMPTS);

        OtpRecord record = otpDAO.findLatestActive(email, purpose)
                .orElseThrow(() -> new OtpException("Mã OTP không hợp lệ hoặc đã hết hạn."));

        if (record.getAttemptCount() >= maxAttempts) {
            throw new OtpException("Bạn đã nhập sai quá nhiều lần. Vui lòng yêu cầu mã OTP mới.");
        }

        String submittedHash = hash(submittedCode.trim());
        if (!MessageDigest.isEqual(submittedHash.getBytes(), record.getOtpHash().getBytes())) {
            otpDAO.incrementAttempt(record.getOtpId());
            throw new OtpException("Mã OTP không đúng.");
        }

        otpDAO.markUsed(record.getOtpId());
    }

    private String generateCode() {
        int bound = (int) Math.pow(10, OTP_LENGTH_DIGITS);
        int code = RANDOM.nextInt(bound);
        return String.format("%0" + OTP_LENGTH_DIGITS + "d", code);
    }

    private String hash(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(code.getBytes());
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String emailSubject(String purpose) {
        return switch (purpose) {
            case OtpPurpose.REGISTER -> "GameNest - Mã xác thực đăng ký";
            case OtpPurpose.CHANGE_EMAIL -> "GameNest - Mã xác thực đổi email";
            default -> "GameNest - Mã xác thực đặt lại mật khẩu";
        };
    }

    private String emailBody(String purpose, String code, Duration otpTtl) {
        String action = switch (purpose) {
            case OtpPurpose.REGISTER -> "hoàn tất đăng ký";
            case OtpPurpose.CHANGE_EMAIL -> "xác nhận đổi email";
            default -> "đặt lại mật khẩu";
        };
        return "<p>Mã OTP để " + action + " tài khoản GameNest của bạn là:</p>"
                + "<h2>" + code + "</h2>"
                + "<p>Mã có hiệu lực trong " + otpTtl.toMinutes() + " phút. "
                + "Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email.</p>";
    }
}
