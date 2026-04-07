# SPEC - FlashLock

## 1) Mục tiêu sản phẩm

FlashLock là ứng dụng học từ vựng tập trung vào trải nghiệm học chủ động (in-app) và học thụ động (trên màn hình khóa), đảm bảo dữ liệu người dùng được bảo mật và đồng bộ đa thiết bị.

## 2) Phạm vi chức năng

### 2.1 Người dùng & hệ thống (User & System)

- Đăng ký tài khoản bằng Email, số điện thoại và mật khẩu.
- Đăng nhập bằng Email/Password.
- Đăng nhập nhanh bằng Google.
- Quên mật khẩu qua Email/SMS OTP.
- Đăng xuất có ghi nhận trạng thái phiên đăng nhập.
- Đồng bộ đám mây dữ liệu học tập lên Firebase.
- Quản lý hồ sơ cá nhân: tên hiển thị, ảnh đại diện.

### 2.2 Quản lý nội dung (Content Management)

- Thư viện chủ đề: duyệt các gói từ vựng có sẵn (TOEIC, IELTS, giao tiếp...).
- My Vocabulary (CRUD): thêm, xem, sửa, xóa từ vựng cá nhân.
- Tra từ nhanh (Mini Dictionary).

### 2.3 Học tập chủ động (Active Learning)

- Học flashcard với cơ chế lật thẻ.
- Đánh giá mức độ ghi nhớ bằng thao tác vuốt hoặc nút hành động.
- Text-to-Speech phát âm Anh/Mỹ.

### 2.4 Học tập thụ động (Passive Learning)

- Lớp phủ màn hình khóa hiển thị thẻ từ khi bật sáng màn hình.
- 3 hành động nhanh: Đã nhớ, Chưa nhớ, Bỏ qua.
- Áp dụng SRS để điều chỉnh tần suất lặp lại.
- Sinh ví dụ ngữ cảnh động bằng AI (Gemini/OpenAI).

### 2.5 Tiện ích & cài đặt (Settings & Utilities)

- Nhắc nhở học tập theo khung giờ.
- Bật/tắt tính năng màn hình khóa.
- Chọn chủ đề từ vựng hiển thị trên màn hình khóa.

## 3) Yêu cầu dữ liệu và đồng bộ

- Dữ liệu người dùng phải gắn theo `uid`.
- Tiến độ học và bộ từ cá nhân phải đồng bộ Firebase để không mất dữ liệu khi đổi máy.
- Trạng thái đồng bộ cần có thời điểm cập nhật gần nhất.

## 4) Yêu cầu bảo mật

- Dữ liệu cá nhân chỉ chủ tài khoản được đọc/ghi.
- API nhạy cảm phải có kiểm soát quyền truy cập.
- Luồng đăng xuất phải đảm bảo hủy phiên và token local.

## 5) Ngoài phạm vi tài liệu này

- Thiết kế chi tiết điều hướng và trạng thái UI nằm ở `app/DOCS/ScreenFlow.md`.
- Kế hoạch thực thi chi tiết nhóm Authentication nằm ở `app/DOCS/AuthExecution.byStep.md`.
