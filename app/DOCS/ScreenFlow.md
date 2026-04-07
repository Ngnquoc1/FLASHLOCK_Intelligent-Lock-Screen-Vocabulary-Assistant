# ScreenFlow - FlashLock

## 1) Global navigation

Ứng dụng sử dụng Bottom Navigation khi vào không gian chính với 4 tab:

- Home: tổng hợp tiến độ và truy cập nhanh phiên học.
- Library: duyệt và tải gói từ vựng.
- My Words: quản lý từ vựng cá nhân.
- Profile: hồ sơ, cài đặt, đồng bộ, đăng xuất.

## 2) Cụm màn hình xác thực (Auth Flow)

### 2.1 Danh sách màn hình

- Splash
- Onboarding
- Login
- Register
- Reset Access
- Reset Confirmation
- Set New Password

### 2.2 Luồng chính

- App launch -> Splash -> kiểm tra phiên đăng nhập.
- Chưa có phiên -> Onboarding -> Login.
- Có phiên -> Home.
- Login <-> Register.
- Login -> Reset Access -> Reset Confirmation -> Set New Password -> Login.

### 2.3 Trạng thái cần xử lý

- Loading khi gọi Auth API.
- Success: điều hướng theo flow.
- Error: hiển thị thông báo thân thiện, không mất dữ liệu form.

## 3) Cụm màn hình không gian chính (Main App)

### 3.1 Home

- Header chào người dùng và avatar.
- Khu vực mini dictionary.
- Widget tiến độ học và streak.
- CTA "Học Flashcard ngay".
- Toggle bật/tắt lock screen overlay.

### 3.2 Library

- Danh sách chủ đề theo nhóm.
- Màn chi tiết chủ đề và thao tác tải/bắt đầu học.

### 3.3 My Words

- Danh sách từ, tìm kiếm và lọc trạng thái.
- Swipe để sửa/xóa.
- FAB mở form thêm/sửa từ.

### 3.4 Profile

- Hồ sơ cá nhân.
- Trạng thái đồng bộ.
- Cài đặt nhắc học và lock screen.
- Đăng xuất có confirm.

## 4) Cụm học tập

### 4.1 In-app Study

- Flashcard lật 2 mặt.
- Nút/gesture đánh giá nhớ-quên.
- TTS phát âm.

### 4.2 Lock Screen Overlay

- Hiển thị thẻ từ trên màn hình khóa khi nhận sự kiện bật sáng màn hình.
- 3 hành động: Chưa nhớ, Đã nhớ, Bỏ qua.
- Trả quyền điều khiển ngay cho người dùng khi chọn Bỏ qua.

## 5) Tài liệu liên quan

- Định nghĩa tính năng: `app/DOCS/SPEC.md`
- Kế hoạch thực thi auth theo bước: `app/DOCS/AuthExecution.byStep.md`
