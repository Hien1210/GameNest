-- ============================================================
-- GameNest — Add CHANGE_EMAIL purpose to OtpVerifications
-- Cần thiết cho chức năng Account Settings > Đổi Email: OTP đổi email
-- phải dùng purpose riêng, tách biệt với RESET_PASSWORD, để không lẫn
-- nội dung email và không rủi ro hai luồng ghi đè/xác nhận chéo OTP
-- của nhau khi cùng một địa chỉ email đang được dùng cho cả hai flow.
--
-- Đây là thay đổi additive: chỉ mở rộng danh sách giá trị hợp lệ của
-- CHECK constraint, không đổi kiểu dữ liệu, không đổi dữ liệu hiện có,
-- không ảnh hưởng luồng REGISTER/RESET_PASSWORD đang chạy.
-- Chạy bằng tài khoản admin/DBA (không dùng GameNestSvcLogin/AppLogin)
-- ============================================================

USE GameNestDB;
GO

ALTER TABLE dbo.OtpVerifications
    DROP CONSTRAINT CK_OtpVerifications_purpose;
GO

ALTER TABLE dbo.OtpVerifications
    ADD CONSTRAINT CK_OtpVerifications_purpose
        CHECK (purpose IN ('REGISTER', 'RESET_PASSWORD', 'CHANGE_EMAIL'));
GO
