# GameNest

**GameNest** là một nền tảng **cộng đồng game thủ**: nơi người chơi chọn một tựa game rồi hỏi đáp, thảo luận, tìm đồng đội và lập đội với những người cùng chơi game đó.

> GameNest **không** phải là game launcher, cửa hàng bán game hay nền tảng stream game. Trọng tâm của dự án là **cộng đồng xoay quanh từng tựa game**.

---

## Ý tưởng cốt lõi

Mọi thứ trong GameNest đều xoay quanh **Game**:

```text
Game
 ├── Hỏi & Đáp (Questions & Answers)
 ├── Tìm đồng đội (LFG – Looking For Group)
 └── Đội / Cộng đồng (Teams, Chat, Bạn bè)
```

Người chơi vào một game, đặt câu hỏi khi gặp khó khăn, đăng bài tìm người chơi cùng, kết bạn, lập đội và trò chuyện trực tiếp – tất cả trong một nơi, thay vì phải rải rác qua nhiều diễn đàn và nhóm chat khác nhau.

---

## Dự án dành cho ai?

| Vai trò | Mô tả |
|---------|-------|
| **Người chơi (User)** | Hỏi đáp, tìm đồng đội, kết bạn, lập đội, chat. |
| **Kiểm duyệt viên (Moderator)** | Xử lý báo cáo và kiểm duyệt nội dung câu hỏi/câu trả lời. |
| **Quản trị viên (Admin)** | Quản lý tài khoản, danh mục game, báo cáo, nhật ký hệ thống và cấu hình chung. |

---

## Tính năng chính

**Tài khoản & hồ sơ**
- Đăng ký xác thực email bằng mã OTP, đăng nhập, quên/đặt lại mật khẩu, đổi email/mật khẩu.
- Hồ sơ cá nhân và hồ sơ công khai, ảnh đại diện, danh sách game đang chơi.

**Game**
- Danh mục game do quản trị viên quản lý; mỗi game có trang riêng để xem câu hỏi và bài tìm đồng đội liên quan.

**Hỏi & Đáp**
- Đặt câu hỏi theo từng game, trả lời, chỉnh sửa và chấp nhận câu trả lời phù hợp.

**Tìm đồng đội (LFG)**
- Đăng bài tìm người chơi cùng theo game, chế độ chơi, rank, khu vực và số lượng người.
- Tham gia / rời nhóm với giới hạn số người và trạng thái bài đăng (mở, đầy, đóng, hết hạn).

**Đội (Teams)**
- Lập đội theo game, mời thành viên, chấp nhận/từ chối lời mời, chuyển quyền chủ đội.

**Mạng xã hội thu nhỏ**
- Theo dõi, kết bạn, chặn người dùng.

**Chat & thông báo thời gian thực**
- Chat riêng và chat theo đội: trả lời tin nhắn, biểu cảm (reaction), đính kèm tệp, tìm kiếm, trạng thái online, hiển thị đang gõ.
- Thông báo cập nhật tức thì qua WebSocket.

**Kiểm duyệt & quản trị**
- Báo cáo nội dung/tài khoản vi phạm, bảng điều khiển kiểm duyệt và quản trị, nhật ký hoạt động (audit log), cấu hình hệ thống.

---

## Nguyên tắc thiết kế

- **Không xóa cứng dữ liệu cộng đồng** – nội dung người dùng tạo ra được ẩn/đánh dấu trạng thái (soft delete) thay vì bị xóa khỏi cơ sở dữ liệu.
- **Ứng dụng không được tin tưởng quá mức** – tài khoản database của ứng dụng chỉ có quyền tối thiểu, không có quyền xóa hay sửa cấu trúc bảng; các ràng buộc quan trọng được đặt ngay ở SQL Server chứ không chỉ trong code Java.
- **Phân quyền ở phía server** – mọi thao tác nhạy cảm đều được kiểm tra quyền trên server, không dựa vào giao diện.
- **Đơn giản, dễ hiểu** – một ứng dụng monolith có phân tầng rõ ràng, không thêm framework hay hạ tầng phức tạp khi chưa cần.

---

## Công nghệ

- **Backend:** Java 17, Jakarta Servlet & WebSocket, JDBC
- **Giao diện:** JSP, HTML, CSS, JavaScript
- **Cơ sở dữ liệu:** Microsoft SQL Server
- **Máy chủ ứng dụng:** Apache Tomcat 10.1+
- **Build:** Maven (đóng gói WAR)
- **Dịch vụ ngoài (tùy chọn):** Cloudinary (ảnh đại diện), Gmail SMTP (gửi OTP)

Kiến trúc phân tầng:

```text
Browser → Servlet/Controller → Service → DAO → JDBC → SQL Server
```

---

## Tài liệu liên quan

- [`Database.md`](Database.md) – thiết kế cơ sở dữ liệu chi tiết.
- [`db/`](db/) – các migration script của SQL Server.
- [`CLAUDE.md`](CLAUDE.md) – quy tắc kiến trúc và nghiệp vụ của dự án.

---

## Trạng thái

GameNest là dự án học tập, **đang trong quá trình phát triển**. Một số module (ví dụ bình luận, bình chọn, uy tín người dùng) nằm trong định hướng nhưng chưa được triển khai.
