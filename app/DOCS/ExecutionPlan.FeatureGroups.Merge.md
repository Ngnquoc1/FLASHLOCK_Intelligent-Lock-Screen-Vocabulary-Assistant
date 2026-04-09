# FlashLock - Ke hoach thuc thi theo nhom feature + Database + Merge nhanh

## 1) Muc tieu tai lieu
Tai lieu nay dung de thong nhat cach trien khai app theo tung nhom feature, cach thiet ke database Firebase cho moi nhom, va quy trinh merge cac nhanh de khong vo luong.

Phu hop cho bối canh hien tai:
- Nhanh dang lam: `feature/authentication`
- Nhanh UI khac tren remote: `origin/feature/homebatch` (du kien chua man hinh cho + navbar)
- Nhanh on dinh: `main`

---

## 2) Phan nhom feature va thu tu thuc hien

### Nhom A - Core App Shell (Splash + Navbar + Main Navigation)
**Nhanh de trien khai**: `feature/homebatch` (hoac tach nho them nhu `feature/splash`, `feature/navbar` neu can)

**Pham vi**
- Splash/Loading
- Main container + bottom navbar
- Dieu huong den login hoac vao app chinh theo trang thai dang nhap

**Database lien quan**
- Khong can write phuc tap
- Chi can doc trang thai auth (`FirebaseAuth.getCurrentUser()`)

**Definition of Done**
- App mo len vao Splash
- Neu chua dang nhap -> vao Login
- Neu da dang nhap hop le -> vao Main + navbar
- Khong crash khi rotate/man hinh nho

---

### Nhom B - Authentication (Email/Password + Reset + Verification)
**Nhanh de trien khai**: `feature/authentication`

**Pham vi**
- Login, Register, Reset Access, Reset Confirmation, Set New Password
- Email verification (bat buoc truoc khi vao app chinh neu policy yeu cau)
- Dong bo profile co ban len Firestore sau dang ky

**Database lien quan**
1. **Firebase Authentication** (he thong auth goc)
   - Quan ly account, password hash, token, verify email
2. **Cloud Firestore**
   - Collection: `users`
   - Document id: `uid` cua Firebase Auth
   - Muc tieu: luu profile bo sung (khong thay the Auth)

**Cau truc goi y `users/{uid}`**
```json
{
  "uid": "firebaseUid",
  "displayName": "Nguyen Van A",
  "email": "a@example.com",
  "photoUrl": null,
  "isEmailVerified": false,
  "provider": "password",
  "createdAt": "serverTimestamp",
  "updatedAt": "serverTimestamp",
  "lastLoginAt": "serverTimestamp",
  "status": "active"
}
```

**Definition of Done**
- Register tao account tren Firebase Auth thanh cong
- Tao/merge du lieu `users/{uid}` thanh cong
- Login thanh cong + cap nhat `lastLoginAt`
- Reset password qua email hoat dong
- Email verification hoat dong theo policy

---

### Nhom C - User Profile
**Nhanh de trien khai de xuat**: `feature/profile`

**Pham vi**
- Xem/sua thong tin ho so (ten, avatar, setting co ban)

**Database lien quan**
- Firestore: `users/{uid}`
- Firebase Storage: `avatars/{uid}/...`

**Definition of Done**
- Cap nhat profile xong hien thi ngay tren UI
- Rule ngan user sua profile cua nguoi khac

---

### Nhom D - Hoc lieu/Noi dung chinh (Words/Decks/Progress)
**Nhanh de trien khai de xuat**: `feature/content-core`

**Pham vi**
- CRUD hoc lieu ca nhan
- Theo doi tien do

**Database lien quan (goi y)**
- `users/{uid}/decks/{deckId}`
- `users/{uid}/words/{wordId}`
- `users/{uid}/progress/{progressId}`

**Definition of Done**
- Tao/sua/xoa/loc du lieu nhanh, dung user
- Dong bo offline/online co kiem soat conflict co ban

---

## 3) Kien truc database Firebase tong quan

### 3.1 Dich vu dung
- Firebase Authentication: danh tinh va xac thuc
- Cloud Firestore: profile + du lieu nghiep vu
- Firebase Storage: media (avatar, file)
- (Tuy chon) Cloud Functions: logic server nhay cam, audit

### 3.2 Rule dinh huong
- Deny by default
- User chi doc/ghi tai nguyen co `uid` trung `request.auth.uid`
- Kiem tra field cho phep update (khong cho ghi de len field nhay cam)

### 3.3 Chi so/cost
- Bat index khi query phuc hop
- Tranh viet log qua nhieu collection neu khong can
- Co retention policy cho log

---

## 4) Ke hoach merge nhanh (chi tiet, an toan)

## 4.1 Nguyen tac
- Moi nhom feature merge theo thu tu: **A -> B -> C -> D**
- Moi PR nho, de review, co test case dinh kem
- Khong merge truc tiep vao `main` neu chua pass build/test

### 4.2 Thu tu merge de xuat voi nhanh hien co
1. Dong bo nhanh remote UI shell ve local:
   - Tao local tracking cho `feature/homebatch`
2. Rebase hoac merge `main` vao tung nhanh feature de giam conflict som
3. Merge `feature/homebatch` vao `feature/authentication` (hoac tao integration branch)
4. Fix conflict, chay test Auth + Navigation
5. Tao PR len `main`

### 4.3 Lua chon chien luoc
- **Lua chon 1 (de quan ly)**: Tao nhanh tich hop `integration/auth-shell`
  - Merge `feature/homebatch` + `feature/authentication` vao nhanh nay
  - Test xong moi merge vao `main`
- **Lua chon 2 (nhanh hon)**: Merge truc tiep `feature/homebatch` -> `feature/authentication`
  - On dinh xong moi PR `feature/authentication` -> `main`

### 4.4 Checklist truoc moi lan merge
- [ ] Build debug thanh cong
- [ ] Unit test auth pass
- [ ] Login/Register/Reset flow pass manual
- [ ] Navigation Splash -> Login/Main dung
- [ ] Khong con API key placeholder o vi tri critical
- [ ] Rule Firestore/Storage khong bi long

---

## 5) Ke hoach theo sprint (goi y 4 sprint)

### Sprint 1 (1 tuan): Chot shell + auth baseline
- Hop nhat nhanh A + B
- Chot luong navigation
- Chot schema `users/{uid}`

### Sprint 2 (1 tuan): Auth hardening
- Email verification, reset flow, error handling day du
- Ghi/doi chieu log auth can thiet

### Sprint 3 (1-2 tuan): Profile + storage avatar
- CRUD profile
- Storage upload avatar + rule

### Sprint 4 (1-2 tuan): Content core + release prep
- Du lieu hoc chinh
- Test E2E, toi uu rule/index, chuan bi release

---

## 6) Vai tro va trach nhiem (de phoi hop nhanh)
- Nhom UI: Layout, navigation, state render
- Nhom Auth/Backend: Firebase Auth + Firestore sync + rules
- Nhom QA: test case regression cho flow auth/navigation
- PM/Tech lead: gate merge, chot DoD moi sprint

---

## 7) Quy uoc branch/PR de de theo doi
- Nhanh feature: `feature/<ten-nhom>`
- Nhanh fix nong: `hotfix/<ten-loi>`
- Nhanh tich hop: `integration/<pham-vi>`
- Ten PR mau:
  - `[Auth] Implement email verification gate`
  - `[Shell] Integrate splash + bottom navbar`

---

## 8) Moc uu tien ngay luc nay
1. Hop nhat duoc `feature/homebatch` voi `feature/authentication` ma khong vo luong auth
2. Chot luong: Splash -> Auth/Main
3. Chot schema `users/{uid}` va rule co ban
4. Khoa baseline test manual auth + navigation

