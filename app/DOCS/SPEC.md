**TỔNG HỢP CHỨC NĂNG ỨNG DỤNG FLASHLOCK**

1. **Phân hệ Người dùng & Hệ thống (User & System Module)**s

*Đây là phân hệ nền tảng, đảm bảo dữ liệu người dùng được bảo mật và đồng bộ.*

- **Đăng ký tài khoản (Register):** Tạo tài khoản mới bằng Email, SĐT, Mật khẩu.
- **Đăng nhập (Login):**
    - Đăng nhập tài khoản đã có.
    - Tích hợp đăng nhập nhanh qua Google.
- **Quên mật khẩu:** Gửi OTP qua SMS/Email yêu cầu thay đổi mật khẩu.
- **Đăng xuất:** Đăng xuất không phải chỉ chuyển sang form khác, phải ć thông báo / token ghi nhận đã đăng xuất.
- **Đồng bộ đám mây (Cloud Sync):** Tự động sao lưu tiến độ học tập (Level từ vựng) và bộ từ vựng cá nhân lên Server (Firebase) để không mất dữ liệu khi đổi máy.
- **Quản lý hồ sơ (User Profile):** Hiển thị và chỉnh sửa thông tin cá nhân (Tên, Ảnh đại diện).
1. **Quản lý Nội dung (Content Management Module)**
- **Thư viện chủ đề (Topic Library):**
    - Hiển thị danh sách các gói từ vựng có sẵn (VD: TOEIC, IELTS, Giao tiếp cơ bản...).
    - Cho phép người dùng chọn/tải các gói này về học.
- **Quản lý Từ vựng cá nhân (My Vocabulary - CRUD):**
    - **Thêm từ (Create):** Người dùng tự nhập từ mới, nghĩa tiếng Việt, và câu ví dụ.
    - **Xem danh sách (Read):** Xem lại tất cả các từ đã thêm, hỗ trợ tìm kiếm/lọc.
    - **Sửa từ (Update):** Chỉnh sửa nội dung nếu nhập sai.
    - **Xóa từ (Delete):** Xóa bỏ các từ không còn muốn học.
- **Tra từ nhanh (Mini Dictionary):** Tra cứu nghĩa của một từ bất kỳ có trong cơ sở dữ liệu ứng dụng.

1. **Học tập Chủ động (Active Learning Module)**
- **Học Flashcard (In-app Study):**
    - Giao diện lật thẻ (Mặt trước: Tiếng Anh - Mặt sau: Nghĩa & Ví dụ).
    - Tương tác Vuốt (Swipe) hoặc bấm nút để đánh giá mức độ thuộc bài.
- **Phát âm chuẩn (Text-to-Speech):** Tích hợp nút loa để nghe giọng đọc bản xứ (Anh/Mỹ) cho từng từ vựng.

1. **Học tập Thụ động (Passive Learning Module)**

*Tính năng cốt lõi (Key Feature).*

- **Màn hình khóa thông minh (Lock Screen Overlay):**
    - Tự động hiển thị thẻ từ vựng đè lên màn hình ngay khi người dùng bật sáng điện thoại (SCREEN\_ON).
    - Cho phép tương tác nhanh mà không cần mở khóa:
        - Nút **"Đã nhớ"**: Đánh dấu đã thuộc.
        - Nút **"Chưa nhớ"**: Đánh dấu cần ôn lại.
        - Nút **"Bỏ qua"**: Tắt lớp phủ để dùng điện thoại.
- **Thuật toán Lặp lại ngắt quãng (SRS Logic):**
    - Hệ thống tự động tính toán "điểm rơi của trí nhớ" để hiển thị từ vựng vào đúng lúc người dùng sắp quên (thay vì hiển thị ngẫu nhiên).
    - Từ khó (bấm "Chưa nhớ") sẽ xuất hiện tần suất dày đặc hơn từ dễ.
- **Ví dụ ngữ cảnh thông minh (AI Contextual Examples):**
    - Mô tả: Sử dụng Generative AI (Gemini/OpenAI) để sinh ra các câu ví dụ mới lạ, độc nhất cho mỗi lần từ vựng xuất hiện, tránh nhàm chán so với câu ví dụ tĩnh.

**5. Tiện ích & Cài đặt (Settings & Utilities)**

*Tăng trải nghiệm người dùng (UX).*

- **Nhắc nhở học tập (Daily Reminder):** Gửi thông báo (Push Notification) nhắc người dùng vào app học vào khung giờ cố định.
- **Cấu hình hiển thị:**
    - Bật/Tắt tính năng Màn hình khóa.
    - Tùy chọn chủ đề từ vựng muốn hiển thị ra màn hình khóa.

