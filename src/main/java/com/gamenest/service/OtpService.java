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
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Xác thực OTP - GameNest</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #0a0a0f; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; color: #f3f4f6; -webkit-font-smoothing: antialiased;">
                <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="background-color: #0a0a0f; padding: 40px 10px;">
                    <tr>
                        <td align="center">
                            <!-- Outer Wrapper Card -->
                            <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="max-width: 500px; background-color: #12121d; border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 16px; overflow: hidden; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);">
                                
                                <!-- Header -->
                                <tr>
                                    <td align="center" style="padding: 30px 40px 20px 40px; border-bottom: 1px solid rgba(255, 255, 255, 0.06);">
                                        <h1 style="margin: 0; font-size: 28px; font-weight: 800; letter-spacing: 1px; color: #06b6d4;">
                                            <span style="color: #8b5cf6;">Game</span>Nest
                                        </h1>
                                        <p style="margin: 5px 0 0 0; font-size: 11px; color: #9ca3af; text-transform: uppercase; letter-spacing: 2px;">
                                            Kết Nối. Chinh Phục. Tỏa Sáng.
                                        </p>
                                    </td>
                                </tr>
            
                                <!-- Content -->
                                <tr>
                                    <td style="padding: 40px 40px 30px 40px;">
                                        <h2 style="margin: 0 0 16px 0; font-size: 20px; font-weight: 600; color: #ffffff; text-align: center;">
                                            MÃ XÁC NHẬN OTP
                                        </h2>
                                        <p style="margin: 0 0 28px 0; font-size: 15px; line-height: 1.6; color: #d1d5db; text-align: center;">
                                            Bạn đã nhận được yêu cầu xác thực OTP để <strong>%s</strong> tài khoản GameNest. Vui lòng sử dụng mã dưới đây:
                                        </p>
            
                                        <!-- OTP Display -->
                                        <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="margin-bottom: 28px;">
                                            <tr>
                                                <td align="center">
                                                    <div style="background-color: #1b132e; border: 2px solid #8b5cf6; padding: 16px 28px; border-radius: 12px; display: inline-block; letter-spacing: 6px; font-size: 32px; font-weight: 800; color: #06b6d4; font-family: 'Courier New', Courier, monospace;">
                                                        %s
                                                    </div>
                                                </td>
                                            </tr>
                                        </table>
            
                                        <p style="margin: 0 0 10px 0; font-size: 14px; line-height: 1.5; color: #9ca3af; text-align: center;">
                                            Mã có hiệu lực trong vòng <strong>%d phút</strong>.
                                        </p>
                                        <p style="margin: 0; font-size: 13px; line-height: 1.5; color: #6b7280; text-align: center;">
                                            Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này một cách an toàn. Vui lòng không chia sẻ mã này cho bất kỳ ai.
                                        </p>
                                    </td>
                                </tr>
            
                                <!-- Footer -->
                                <tr>
                                    <td align="center" style="padding: 24px 40px; background-color: #0e0e17; border-top: 1px solid rgba(255, 255, 255, 0.06); font-size: 12px; color: #6b7280; text-align: center; line-height: 1.5;">
                                        © 2026 GameNest. Mọi quyền được bảo lưu.<br>
                                        Hệ thống quản lý cộng đồng game thủ hàng đầu.
                                    </td>
                                </tr>
                                
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(action, code, otpTtl.toMinutes());
    }
}
