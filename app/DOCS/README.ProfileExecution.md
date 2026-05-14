# README - Ke hoach code User Profile (tu Buoc 2 tro di)

Tai lieu nay mo ta chi tiet cach trien khai nhom chuc nang **User Profile** tren nhanh `feature/profile`.

> Ghi chu: **Buoc 1 (fetch + tao nhanh `feature/profile`) da hoan tat**.

---

## 0) Muc tieu va pham vi

### Muc tieu
- Tao va hoan thien luong User Profile theo MVVM + Firebase.
- User xem/sua duoc thong tin ho so cua chinh minh.
- Avatar upload len Firebase Storage va dong bo URL vao Firestore.
- Rules du chat de user khong sua du lieu cua user khac.

### Pham vi chuc nang profile
- Xem profile: displayName, email, avatar, settings co ban.
- Sua profile: displayName, settings.
- Doi avatar.
- Khong bao gom cloud function nang cao (resize image, moderation) trong dot nay.

### Cac tai lieu can doi chieu
- `app/DOCS/BackendFirebaseBlueprint.md`
- `app/DOCS/ExecutionPlan.FeatureGroups.Merge.md`
- `app/src/main/java/com/nhom18/flashlock/data/model/AuthUserProfile.java`

---

## Step 2 - Chot Data Contract cho `users/{uid}`

Muc tieu cua Step 2: xac dinh ro field nao cho phep app cap nhat, field nao la protected.

### 2.1 Contract de xuat

Document: `users/{uid}`

```json
{
  "uid": "firebaseUid",
  "email": "user@example.com",
  "displayName": "Nguyen Van A",
  "avatarUrl": "https://...",
  "avatarPath": "avatars/<uid>/avatar_1710000000.jpg",
  "provider": "password",
  "isEmailVerified": true,
  "status": "active",
  "createdAt": "serverTimestamp",
  "updatedAt": "serverTimestamp",
  "lastLoginAt": "serverTimestamp",
  "settings": {
    "lockScreenEnabled": true,
    "reminderHour": 20,
    "reminderMinute": 30
  }
}
```

### 2.2 Phan loai field

- Editable tu app Profile:
  - `displayName`
  - `avatarUrl`
  - `avatarPath`
  - `settings.lockScreenEnabled`
  - `settings.reminderHour`
  - `settings.reminderMinute`
  - `updatedAt`

- Protected (khong cho client Profile ghi de):
  - `uid`
  - `email`
  - `provider`
  - `createdAt`
  - `lastLoginAt`
  - `status`
  - `isEmailVerified`

### 2.3 Luu y dong bo voi Auth
- `AuthUserProfile.toFirestoreMap()` can giu dong bo voi contract moi: khong con `phone`, su dung `status` thay cho `isActive`.
- Neu da co document cu co `phone`/`isActive`, giu backward-compatible trong giai doan chuyen doi va xoa dan qua migration task rieng.

Deliverable Step 2:
- Team thong nhat contract field-level.
- Ghi ro contract vao code model Profile va rules.

---

## Step 3 - Thao tac thu cong tren Firebase Console

Muc tieu cua Step 3: dam bao backend service san sang truoc khi code UI/profile.

### 3.1 Firebase Authentication
1. Mo Firebase Console -> project FlashLock.
2. Vao **Authentication** -> **Sign-in method**.
3. Xac nhan da bat:
   - Email/Password
   - Google (neu app dang dung)
4. Kiem tra tab **Users** da co account test.

### 3.2 Firestore Database
1. Vao **Firestore Database**.
2. Neu chua tao DB: bam **Create database**.
3. Chon **Production mode**.
4. Chon region on dinh (nen cung region voi Storage).
5. Tao tay 1 document test `users/{uid}` (co the bo qua neu app se tu tao khi login/register).

### 3.3 Firebase Storage
1. Vao **Storage** -> **Get started** neu chua khoi tao.
2. Chon cung region voi Firestore.
3. Kiem tra bucket da active.

### 3.4 Firebase CLI deploy rules (thuc hien o local)

```powershell
firebase login
firebase use <your-project-id>
firebase deploy --only firestore:rules,storage
```

Deliverable Step 3:
- Auth/Firestore/Storage deu da san sang.
- Rules deploy thanh cong.

---

## Step 4 - Hardening `firestore.rules` va `storage.rules`

Muc tieu: owner-only + whitelist field update.

### 4.1 Rule Firestore can co
- Chi user dang nhap moi doc/ghi.
- Chi owner (`request.auth.uid == uid`) moi truy cap `users/{uid}` cua minh.
- `update` chi cho phep danh sach field editable.
- Validate range cho `settings.reminderHour` (0..23) va `settings.reminderMinute` (0..59).

### 4.2 Rule Storage can co
- Path avatar theo owner:
  - `avatars/{uid}/...`
- Chi owner duoc upload/read (tuy policy).
- Gioi han file:
  - `contentType` la image
  - size <= 5MB (goi y)

### 4.3 Checklist verify rules
- User A khong doc/sua document cua user B.
- User A khong upload vao `avatars/B/...`.
- User A khong update field protected.

Deliverable Step 4:
- Rules pass test manual co canh bao loi dung ky vong.

---

## Step 5 - Trien khai Data Layer (MVVM)

Muc tieu: tach ro data source va repository de ViewModel khong phu thuoc Firebase API truc tiep.

### 5.1 File can tao/sua

Tao moi (goi y):
- `app/src/main/java/com/nhom18/flashlock/data/model/UserProfile.java`
- `app/src/main/java/com/nhom18/flashlock/data/repository/ProfileRepository.java`
- `app/src/main/java/com/nhom18/flashlock/data/repository/FirebaseProfileRepository.java`
- `app/src/main/java/com/nhom18/flashlock/data/remote/FirebaseProfileDataSource.java`

Co the sua them:
- `app/src/main/java/com/nhom18/flashlock/data/model/AuthUserProfile.java` (neu can dong bo schema)

### 5.2 Contract Repository de xuat
- `getCurrentUserProfile()`
- `observeCurrentUserProfile()` (optional LiveData/Flow)
- `updateProfile(displayName, settings)`
- `uploadAvatar(imageUri)`
- `updateAvatarInfo(avatarUrl, avatarPath)`

### 5.3 Nguyen tac
- Repository tra ve object ket qua co status/thong diep loi.
- Tat ca timestamp dung `FieldValue.serverTimestamp()`.
- Khong hardcode `uid` tren UI; lay tu `FirebaseAuth.getCurrentUser()`.

Deliverable Step 5:
- Data layer compile pass.
- Co API day du cho ViewModel.

---

## Step 6 - Trien khai `ProfileViewModel` + UI State

Muc tieu: quan ly state man hinh profile ro rang, de test va de bao tri.

### 6.1 File can tao
- `app/src/main/java/com/nhom18/flashlock/ui/profile/ProfileUiState.java`
- `app/src/main/java/com/nhom18/flashlock/ui/profile/ProfileViewModel.java`

### 6.2 State toi thieu
- `IDLE`
- `LOADING`
- `CONTENT` (co data profile)
- `SAVING`
- `SUCCESS`
- `ERROR`

### 6.3 Su kien xu ly
- `loadProfile()`
- `onSaveProfile(displayName, settings...)`
- `onAvatarPicked(uri)`
- `onRetry()`

Deliverable Step 6:
- UI co the observe state de render loading/error/success.

---

## Step 7 - Trien khai UI Profile + noi flow vao Main/Nav

Muc tieu: user vao tab Profile, xem/sua du lieu that.

### 7.1 File UI can tao/sua
Tao moi (goi y):
- `app/src/main/res/layout/fragment_profile.xml` (hoac `activity_profile.xml`)
- `app/src/main/java/com/nhom18/flashlock/ui/profile/ProfileFragment.java`

Sua:
- `app/src/main/java/com/nhom18/flashlock/ui/main/MainActivity.java` (noi tab/fragment)
- `app/src/main/res/values/strings.xml` (bo sung text)
- `app/src/main/AndroidManifest.xml` (neu dung Activity rieng)

### 7.2 UI thanh phan toi thieu
- Avatar ImageView + nut doi anh
- Display name input
- Email text read-only
- Settings (switch + gio/phut)
- Nut Save
- Snackbar/Toast thong bao ket qua

### 7.3 UX quy tac
- Disable nut Save khi dang `SAVING`.
- Validate displayName khong rong.
- Neu save thanh cong: cap nhat UI ngay, hien thong bao thanh cong.

Deliverable Step 7:
- Tab Profile hoat dong full flow voi data that.

---

## Step 8 - Avatar Upload Flow chi tiet

Muc tieu: upload avatar on dinh, tranh leak file va loi race condition.

### 8.1 Luong chuan
1. User chon anh tu gallery.
2. Validate local:
   - dinh dang image
   - size hop le
3. Upload file len Storage path:
   - `avatars/{uid}/avatar_<timestamp>.jpg`
4. Lay `downloadUrl`.
5. Update Firestore `users/{uid}`:
   - `avatarUrl`
   - `avatarPath`
   - `updatedAt`
6. Reload UI avatar.

### 8.2 Edge cases phai xu ly
- User huy chon anh.
- Mat mang giua qua trinh upload.
- Upload thanh cong nhung Firestore update fail.
- User bam upload lien tuc nhieu lan.

### 8.3 Chien luoc rollback mini
- Neu upload xong ma update Firestore fail: xoa file vua upload hoac danh dau retry.
- Neu update Firestore xong nhung UI load fail: giu du lieu backend, UI retry image loading.

Deliverable Step 8:
- Avatar flow hoat dong va co thong bao loi ro rang.

---

## Step 9 - Test checklist (manual + technical)

### 9.1 Test manual bat buoc
- [ ] Login user A -> Profile hien dung email/displayName.
- [ ] User A sua ten -> Save -> vao lai app van con.
- [ ] User A doi avatar -> restart app van hien dung.
- [ ] Logout -> login user B -> khong thay data user A.
- [ ] Loi mang khi Save/Upload -> app khong crash, hien thong diep de hieu.

### 9.2 Test security
- [ ] Thu sua `users/{uid-khac}` bi deny.
- [ ] Thu upload vao `avatars/{uid-khac}/...` bi deny.
- [ ] Thu ghi de field protected (`createdAt`, `email`) bi deny.

### 9.3 Build/test command goi y

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

Deliverable Step 9:
- Checklist pass va co evidence test ngan gon trong PR description.

---

## Step 10 - Risk, rollback, va cach xu ly su co

### 10.1 Rui ro ky thuat
- Lech schema giua auth sync va profile update.
- Rules qua chat lam app luon bi permission denied.
- Rules qua long gay lo hong du lieu.
- Anh qua lon lam upload timeout.

### 10.2 Ke hoach rollback
- Rule rollback: restore version rule truoc do tu git + deploy lai.
- Code rollback: `git revert <commit-profile>` neu loi nghiem trong.
- Feature flag (neu co): tam an Profile edit chi cho read-only.

### 10.3 Monitoring toi thieu
- Ghi log loi upload/save (Logcat + analytics neu da co).
- Theo doi ty le fail save profile.

---

## Step 11 - Definition of Done (DoD) cho nhom C

Nhanh `feature/profile` duoc xem la hoan tat khi:
- [ ] User xem/sua profile cua minh thanh cong.
- [ ] Avatar upload + luu URL Firestore thanh cong.
- [ ] Firestore/Storage rules chan user truy cap trai phep.
- [ ] Build debug pass.
- [ ] Co checklist test manual da tick day du.
- [ ] Co commit log ro rang + mo ta PR day du.

---

## Step 12 - Ke hoach commit theo moc (de merge de dang)

### Moc 1 - Contract + Rules
- Commit 1: chot model field + cap nhat rules.
- Commit 2: deploy rules + note verify.

### Moc 2 - Data layer
- Commit 3: tao model/repository/data source profile.

### Moc 3 - ViewModel + UI
- Commit 4: tao `ProfileViewModel` + `ProfileUiState`.
- Commit 5: tao man hinh Profile + wiring Main/Nav.

### Moc 4 - Avatar + hardening
- Commit 6: upload avatar flow + error handling.
- Commit 7: polish UI + message + cleanup.

### Moc 5 - Test + merge
- Commit 8: cap nhat test/checklist docs.
- Tao PR `feature/profile` -> nhanh tich hop/nhanh dich.

---

## Step 13 - Mau ten commit de su dung nhanh

```text
feat(profile): add firestore profile repository and model
feat(profile): implement profile viewmodel and ui state
feat(profile): add profile screen and navigation wiring
feat(profile): implement avatar upload to firebase storage
chore(rules): harden firestore and storage rules for profile
test(profile): add manual checklist and validation notes
```

---

## Appendix - Manual Firebase thao tac nhanh cho ban

### A. Tao user test
- Vao Firebase Console -> Authentication -> Users -> Add user.
- Tao 2 user (A/B) de test ownership.

### B. Kiem tra data Firestore
- Vao Firestore -> `users` -> chon doc theo `uid`.
- Sau khi save profile, kiem tra `displayName`, `avatarUrl`, `updatedAt`.

### C. Kiem tra Storage
- Vao Storage -> folder `avatars/<uid>`.
- Dam bao file upload nam dung path user hien tai.

---

## Trang thai hien tai
- [x] Step 1 da xong (tao nhanh `feature/profile`)
- [ ] Step 2 den Step 13 dang cho trien khai theo tai lieu nay

