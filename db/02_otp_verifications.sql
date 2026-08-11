-- ============================================================
-- GameNest — OTP Verification table
-- Dùng cho: xác thực OTP khi đăng ký, đặt lại mật khẩu quên
-- Chạy bằng tài khoản admin/DBA (không dùng GameNestAppLogin)
-- ============================================================

USE GameNestDB;
GO

CREATE TABLE dbo.OtpVerifications (
    otp_id          INT             IDENTITY(1,1)   NOT NULL,
    email           NVARCHAR(255)   NOT NULL,
    purpose         VARCHAR(20)     NOT NULL,
    otp_hash        CHAR(64)        NOT NULL,   -- SHA-256 hex digest, không lưu OTP dạng plaintext
    expires_at      DATETIME2(0)    NOT NULL,
    is_used         BIT             NOT NULL CONSTRAINT DF_OtpVerifications_is_used DEFAULT (0),
    attempt_count   INT             NOT NULL CONSTRAINT DF_OtpVerifications_attempt_count DEFAULT (0),
    created_at      DATETIME2(0)    NOT NULL CONSTRAINT DF_OtpVerifications_created_at DEFAULT (SYSUTCDATETIME()),

    CONSTRAINT PK_OtpVerifications PRIMARY KEY (otp_id),
    CONSTRAINT CK_OtpVerifications_purpose CHECK (purpose IN ('REGISTER', 'RESET_PASSWORD'))
);
GO

CREATE INDEX IX_OtpVerifications_email_purpose ON dbo.OtpVerifications (email, purpose, is_used);
GO

-- Không cần GRANT DELETE riêng: module này chỉ dùng INSERT/SELECT/UPDATE,
-- đã được cấp sẵn ở cấp schema cho GameNestAppRole.
