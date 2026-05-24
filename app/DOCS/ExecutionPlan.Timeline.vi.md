# FlashLock - Lộ trình thực thi chi tiết theo từng tuần (4 Tuần)

Tài liệu này là bản dịch Tiếng Việt của khối "Week-by-week timeline" từ kế hoạch `ExecutionPlan.End2End.4Weeks.md`.

## Bảng tóm tắt kế hoạch 4 tuần

| Tuần | Mục tiêu | Hạng mục kỹ thuật chính | Đầu ra mong đợi | Trạng thái |
| --- | --- | --- | --- | --- |
| Tuần 1/4 | Ưu tiên Profile + Avatar nền tảng | Chốt contract `users/{uid}` (không `phone`), tạo `ProfileRepository`/`FirebaseProfileDataSource`, tạo `ProfileViewModel`/`ProfileUiState`, wiring `ProfileFragment`, upload avatar `avatars/{uid}/...`, cập nhật rules profile/avatar | Profile có thể load/update thông tin user và upload avatar end-to-end; rules owner-only được xác minh | Not_Done |
| Tuần 2/4 | Hoàn thiện Content core | Triển khai `WordRepository`, CRUD `users/{uid}/my_words/{wordId}`, đọc `topics/*`, bổ sung query/index cần thiết, test repository + ViewModel | User CRUD được từ vựng cá nhân và xem được topic catalog khi đăng nhập | Not_Done |
| Tuần 3/4 | Learning + Sync | Ghi `study_events`, scaffold lock-screen quick actions, triển khai `SyncRepository`, ghi `sync_logs` có `traceId`, retry khi lỗi mạng | Study events và sync logs đầy đủ, có khả năng truy vết và phục hồi cơ bản | Not_Done |
| Tuần 4/4 | Hardening backend + Release gate | Full Cloud Functions (`onStudyEventWrite`, `callGenerateExample`, `onAuthCreate`, `sendDailyReminder`), App Check, FCM, emulator test rules, regression E2E | RC merge-ready: chức năng core hoạt động end-to-end, rules/telemetry/bảo mật đạt tiêu chí | Not_Done |

*Ghi chú scope: Đã loại khỏi phạm vi hiện tại `Phone` và `SMS OTP`; Auth sử dụng Email/Password + Google + email reset/verification.*

---

## Tuần 1/4 (Hiện tại) - Nền tảng Profile + Avatar (Ưu tiên cao nhất)

**Mục tiêu**: Hoàn thiện khung module Profile và luồng upload avatar ổn định end-to-end.

### Checklist theo ngày (Tuần 1)

- [ ] **Ngày 1**: Chốt hợp đồng dữ liệu (contract) và policy bảo mật
  - Chốt các trường dữ liệu cho `users/{uid}` (không dùng `phone`).
  - Tách biệt rõ các trường cho phép người dùng sửa và trường hệ thống tự cập nhật.
  - Cập nhật `firestore.rules` để validate file Profile (người dùng chỉ được sửa một số trường cho phép).

- [ ] **Ngày 2**: Triển khai tầng Data
  - Thêm interface `ProfileRepository`.
  - Triển khai `FirebaseProfileDataSource` cho các tác vụ lấy/cập nhật dữ liệu từ Firebase.
  - Cấu hình `FirebaseProfileRepository`.

- [ ] **Ngày 3**: Triển khai ViewModel + Trạng thái UI (UI state)
  - Khởi tạo `ProfileUiState` (`IDLE/LOADING/CONTENT/SAVING/SUCCESS/ERROR`).
  - Viết `ProfileViewModel` để xử lý load và update profile.

- [ ] **Ngày 4**: Triển khai Profile UI và luồng điều hướng
  - Khởi tạo layout `fragment_profile.xml`.
  - Cài đặt `ProfileFragment` sử dụng View Binding.
  - Nối tab Profile vào `MainActivity` navbar.

- [ ] **Ngày 5**: Triển khai Upload Avatar
  - Chọn ảnh, validate định dạng/dung lượng, upload lên cấu trúc thư mục `avatars/{uid}/...`.
  - Lưu cấu hình `avatarUrl`, `avatarPath` và cập nhật field `updatedAt` lên Firestore.
  - Xử lý các tình huống hủy bỏ, kết nối mạng lỗi, và chính sách retry tự động.

- [ ] **Ngày 6**: Phân quyền, bảo mật và xác thực
  - Siết tài nguyên cho `storage.rules` (chỉ cấp path thuộc về người đăng nhập, giới hạn size file và định dạng ảnh).
  - Kiểm tra thủ công: Đảm bảo tài khoản A không có quyền can thiệp data của tài khoản B.

- [ ] **Ngày 7**: Ổn định chạy thử và Tài liệu hóa
  - Chạy các test case bổ sung và checklist các tính năng.
  - Cập nhật nhật ký dự án (`JOURNAL.jsonl`) và các tài liệu liên quan.

**Tiêu chí hoàn thành (DoD) Tuần 1:**
- Profile screen tải và update thành công hồ sơ của user nội bộ.
- Profile Upload Avatar thực thi hoàn chỉnh không lỗi vặt, handle tốt exception/crashed.
- Các quy định Data Rules được pass trót lọt thông qua cơ chế owner-only access.

---

## Tuần 2/4 - Nội dung cốt lõi (Từ vựng cá nhân + Thư viện chủ đề)

**Mục tiêu**: Triển khai tính năng từ vựng của user và luồng đọc chủ đề.

- [ ] Triển khai `WordRepository` + datasource (CRUD từ vựng cá nhân, quan sát theo chu kỳ / due words).
- [ ] Build UI đối với hệ thao tác CRUD của thư viện My Vocabulary.
- [ ] Build UI thư mục tổng để tra từ catalog `topics/*` dành cho authenticated users (read-only).
- [ ] Đánh Index Composite cho Firestore tùy theo bộ Query truy vấn dữ liệu học.
- [ ] Quy định các bộ Rules bảo mật liên đới:
  - Chỉ cho phép owner ghi trong `users/{uid}/my_words/*`
  - Auth read/admin write cho đường dẫn `topics/*`
- [ ] Bổ sung Unit/Integration tests cho Repository và ViewModel.

**Tiêu chí hoàn thành (DoD) Tuần 2:**
- Chức năng thêm/sửa/xóa từ vựng cá nhân vận hành ổn định mà không dính chéo user khác.
- Thư việc topic catalog hiển thị đúng dữ liệu trên môi trường Firebase.

---

## Tuần 3/4 - Luồng học tập + Ghi log đồng bộ (Sync)

**Mục tiêu**: Hoàn thiện luồng kiểm duyệt flashcard learning và theo dõi quá trình đồng bộ đa luồng.

- [ ] Học chủ động: Tích hợp trạng thái thao tác session với các events (`remembered`, `forgot`, `skipped`).
- [ ] Bệ phóng Học thụ động: Cơ tạo baseline / Trigger màn hình chờ. Setup luồng quick actions.
- [ ] Hệ module Sync:
  - Hiện thực hóa lớp `SyncRepository`.
  - Ghi log liên tục thông quan `sync_logs` đánh dấu đầy đủ `traceId`, result và direction.
  - Chính sách Retry cho các failed local writes.
- [ ] Setup convention / analytics ghi events chuẩn chỉnh cho các flow quan trọng.

**Tiêu chí hoàn thành (DoD) Tuần 3:**
- Dữ liệu Event học tập và dấu vết logs đi đúng quy trình, xuất hóa tính truy xuất (traceability).
- Logic hoạt động offline/online app giữ đúng môi trường cô lập, khôi phục data tốt nếu network sụp dòng.

---

## Tuần 4/4 - Hoàn thiện bảo mật backend (Hardening) và Release Gate

**Mục tiêu**: Thiết lập Cloud Functions trọn gói và cấu hình quy chuẩn cho production.

- [ ] Bật toàn bộ công suất Cloud Functions đi kèm ứng dụng:
  - `onStudyEventWrite`: Recompute điểm srs / Next review level phía môi trường Backend.
  - `callGenerateExample`: Lệnh điều hướng prompt AI có cơ chế lướt/chặn spam Proxy.
  - `onAuthCreate`: Gắn kết chuẩn Bootstrap default lúc acc mới join `users/{uid}`.
  - `sendDailyReminder`: Đặt scheduler đi qua FCM.
- [ ] Thiết lập cổng App Check security dùng qua hệ Play Integrity.
- [ ] Gắn FCM nhắc lịch reminder push notification.
- [ ] Rà soát Rules thủ công chạy xuyên chuỗi Emulators đánh vào các protected document.
- [ ] Hợp bộ Regression End-to-end pass cho loạt nhóm chức năng Auth/Profile/Content/Reminders...
- [ ] Release Readiness: Kiểm soát TraceID telemetry, retry config và push code sẵn sàng hợp nhất.

**Tiêu chí hoàn thành (DoD) Tuần 4:**
- Toàn bộ matrix End-to-end phải được pass hoàn toàn.
- Functions + Play Integrity App Check + FCM đảm bảo hoạt động an toàn.
- Nhánh `main` RC đã sẵn sàng merge không gãy sụp chức năng liên kết.

