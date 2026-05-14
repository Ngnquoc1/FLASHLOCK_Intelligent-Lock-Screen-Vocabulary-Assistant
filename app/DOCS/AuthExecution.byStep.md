# Auth Execution by Step - FlashLock

## Mục tiêu

Triển khai tuần tự nhóm Authentication theo thứ tự phụ thuộc kỹ thuật.

## Trạng thái

- `[x]` Done
- `[~]` Partial
- `[ ]` Not Started

## Step 1 - Chuẩn hóa Auth UI State

- [x] Tạo model state chung cho auth (`idle/loading/success/error`).
- [x] Áp dụng cho `LoginViewModel`, `RegisterViewModel`, `ResetAccessViewModel`.
- [x] Activity chỉ observe state và render UI.

## Step 2 - Email/Password Login

- [x] Đã có gọi Firebase Auth.
- [x] Thêm validate input tối thiểu (email/password rỗng).
- [x] Xử lý trạng thái loading và disable nút khi submit.
- [x] Điều hướng sau login thành công.

## Step 3 - Register

- [x] Đã có gọi Firebase Auth create user.
- [x] Chuẩn hóa validate name/email/password.
- [x] Đồng bộ lỗi auth sang thông báo UI.
- [x] Điều hướng sau register theo flow đã chốt.

## Step 4 - Reset Access

- [x] Đã có gửi reset email.
- [x] Validate format email trước submit.
- [x] Chuẩn hóa thông điệp thành công/thất bại.
- [x] Kiểm thử flow `ResetAccess -> ResetConfirm -> Login`.

## Step 5 - Google Sign-In

- [x] Tích hợp Google Sign-In client trong `LoginActivity`.
- [x] Exchange Google credential với Firebase Auth.
- [x] Xử lý trạng thái lỗi mạng/token hết hạn.

## Step 6 - Auth Tests

- [x] Unit test ViewModel state transitions.
- [x] Test case navigation auth flow (xem `app/DOCS/AuthTestCases.md`).
- [x] Regression checklist thủ công cho auth (xem `app/DOCS/AuthTestCases.md`).

## Step 7 - Auth Firestore Hardening

- [x] Đồng bộ `users/{uid}` sau login/register/google.
- [x] Thêm model `AuthUserProfile` để chuẩn hóa payload Firestore.
- [x] Ghi sự kiện auth vào `users/{uid}/auth_logs`.
- [x] Cập nhật Firestore rules cho `auth_logs` owner-only.

## Tài liệu tham chiếu

- `app/DOCS/SPEC.md`
- `app/DOCS/ScreenFlow.md`
- `app/DOCS/AuthTestCases.md`
