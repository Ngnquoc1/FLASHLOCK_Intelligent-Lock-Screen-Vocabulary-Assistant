# FlashLock AI Agent Guide

**Quick Summary**: FlashLock is an Android vocabulary learning app combining active learning (flashcards) with passive learning (lock screen overlays). It uses Firebase backend, MVVM architecture, ViewBinding, and repository pattern for data access.

---

## 1. Architecture Overview

FlashLock follows **MVVM + Repository pattern**:
- **UI Layer**: Activities/Fragments observe ViewModels via callbacks and state objects
- **ViewModel Layer**: Holds UI state (see `AuthUiState` pattern with Status enum: IDLE/LOADING/SUCCESS/ERROR)
- **Repository Layer**: Abstracts data sources, implements business logic
- **Data Source Layer**: `Remote` (Firebase) and `Local` (future) data sources

**Key principle**: Repository interface is stable; implementation can swap Firebase with other providers.

Example flow: `LoginActivity` → `LoginViewModel.login()` → `AuthRepository.signInWithEmail()` → `FirebaseAuthDataSource.signInWithEmail()`

---

## 2. Critical Workflows

### Build & Compile
```powershell
./gradlew :app:compileDebugJavaWithJavac     # Quick compile check
./gradlew :app:assembleDebug                 # Build debug APK
./gradlew clean; ./gradlew build             # Full rebuild
```
**Note**: Requires JDK 11+, AGP 9.0+, Gradle 8.x. Sync Gradle in IDE if adding deps.

### Testing
```powershell
./gradlew :app:testDebugUnitTest             # Run unit tests
```
Tests live in `app/src/test/java/`. Instrumented tests in `app/src/androidTest/java/`.

### Gradle Dependency Catalog
Version centralization in `gradle/libs.versions.toml`. Add deps there first, then reference via `libs.xxx` in `build.gradle.kts`.

### Firebase Configuration
- `google-services.json` must exist in `app/` (from Firebase Console)
- Rules defined in `firestore.rules` and `storage.rules`
- Deploy via: `firebase deploy --only firestore:rules,storage`

---

## 3. Firebase Integration Patterns

### Auth Data Source Pattern (`FirebaseAuthDataSource`)
```java
// Follows callback-based async model (not LiveData/coroutines—yet)
firebaseAuth.signInWithEmailAndPassword(email, password)
    .addOnSuccessListener(authResult -> {
        syncUserProfile(user, "password");
        callback.onSuccess();
    })
    .addOnFailureListener(e -> 
        callback.onError(resolveErrorMessage(e))
    );
```

**Error mapping**: Use `resolveErrorMessage(exception)` to convert Firebase exceptions into standardized error codes (e.g., `AUTH_EMAIL_NOT_VERIFIED`, `AUTH_INVALID_CREDENTIALS`).

**Firestore sync**: After auth success, always call `syncUserProfile(user, method)` to bootstrap `users/{uid}` document with profile and settings.

### Firestore Data Model
- **users/{uid}** - user profile (displayName, email, avatarUrl, settings, createdAt, updatedAt, lastSyncAt)
- **users/{uid}/my_words/{wordId}** - personal vocabulary (term, definition, srsLevel, nextReviewAt, status: new/learning/mastered)
- **users/{uid}/auth_logs/{eventId}** - audit trail (event type, timestamp, status)
- **topics/{topicId}/words/{wordId}** - published library (admin-writable only)

**Ownership enforcement** in `firestore.rules`: User docs readable/writable only by owner (`request.auth.uid == uid`).

---

## 4. Authentication Flow (Complete Spec)

### Screens & Navigation
- **Splash** → checks cached session, routes to Onboarding or Home
- **Onboarding** → directs to Login
- **Login** → Email/Password or Google Sign-In
- **Register** → new account creation with email verification
- **Reset Access** → email verification entry
- **Reset Confirmation** → wait for reset link
- **Set New Password** → password change after link verification

### Email Verification Requirement
- Registration sends `user.sendEmailVerification()` then signs out user
- User checks email, clicks verification link in Firebase Console test env
- On next login attempt, check `user.isEmailVerified()` before allowing access

### Google Sign-In Integration
- `LoginActivity` initializes `GoogleSignInClient` with web client ID from `google-services.json`
- Exchange Google credential: `GoogleAuthProvider.getCredential(idToken, null)` → `firebaseAuth.signInWithCredential()`
- Same `syncUserProfile()` call after successful credential exchange

### ViewModel State Pattern
All auth ViewModels expose `AuthUiState` with factory methods:
- `AuthUiState.idle()` - initial state
- `AuthUiState.loading()` - API call in progress, disable UI
- `AuthUiState.success()` - action completed, trigger navigation
- `AuthUiState.error(message)` - show error snackbar, preserve form data

Activity observes state via `ViewModel.getUiState()` (setter callback or mutable field) and renders accordingly.

---

## 5. Data Layer Conventions

### Repository Interface Contracts
Interfaces in `com.nhom18.flashlock.data.repository`:
- `AuthRepository` - sign in, register, password reset, Google sign-in
- `AuthResultCallback` - two-method interface (`onSuccess()`, `onError(message)`)

**Key pattern**: Repositories accept `AuthResultCallback` for async results; no coroutines yet.

### Remote Data Source
`FirebaseAuthDataSource` and `FirebaseProfileDataSource` are singletons (getInstance() pattern).

**Error handling helpers**:
- `resolveErrorMessage(exception)` - maps Firebase exceptions to human-readable error codes
- `normalizeEmail()`, `normalizeValue()` - trim/lowercase for consistency

### User Profile Bootstrap
Every auth success triggers `syncUserProfile(user, method)` to create/update:
```
users/{uid} = {
  displayName, email, avatarUrl,
  settings: { lockScreenEnabled: true, reminderHour: 9, reminderMinute: 0 },
  createdAt, updatedAt, lastSyncAt
}
```

---

## 6. UI Layer Conventions

### Activity/Fragment Structure
- Activities handle navigation and Firebase initialization
- Fragments compose screen UI with ViewBinding
- Activities hold ViewModel references and observe state
- Fragments interact with parent Activity's ViewModel or own ViewModel

### ViewBinding Setup
In `build.gradle.kts`, `buildFeatures { viewBinding = true }` is already enabled.
- Generate binding: `ActivityXLayout.bind(root)` or `ActivityXLayout.inflate(inflater, ...)`
- Never use `findViewById()` directly

### Fragment Navigation
Uses Android XML navigation graph (if integrating with Navigation Component, define in `res/navigation/`).
Auth flow is Activity-based navigation currently (Splash → Onboarding → LoginActivity → RegisterActivity, etc.).

### Error Display Pattern
- Snackbar for transient errors from API calls
- Toast for system errors
- Preserve form data when error occurs (don't clear on failure)

---

## 7. Project Documentation & Key Files

| File | Purpose |
|------|---------|
| `app/DOCS/SPEC.md` | Feature spec (targets, scope, security requirements) |
| `app/DOCS/ScreenFlow.md` | Navigation graph, auth flow, UI layouts |
| `app/DOCS/AuthExecution.byStep.md` | Auth implementation checklist (7 steps, mostly complete) |
| `app/DOCS/BackendFirebaseBlueprint.md` | Firestore schema, security rules, Cloud Functions design |
| `app/DOCS/AuthTestCases.md` | Manual test cases for auth flow regression |
| `firestore.rules` | Firestore security rules (deny-by-default, owner ownership enforcement) |
| `gradle/libs.versions.toml` | Dependency versions (single source of truth) |

---

## 8. Common Tasks for Agents

### Adding a New Firestore Collection
1. Define schema in class (e.g., `TopicProgress.java`) with Firestore serialization
2. Create `FirebaseXDataSource` with read/write methods using `firestore.collection().document()`
3. Create interface `XRepository` in `data.repository` package
4. Implement repository using FirebaseXDataSource
5. Update `firestore.rules` with access control rules
6. Document in `BackendFirebaseBlueprint.md` under Collections section

### Adding a New Auth Method
1. Add method to `AuthRepository` interface
2. Implement in `FirebaseAuthDataSource` (handle auth success → syncUserProfile)
3. Create or extend ViewModel with login logic
4. Update Activity/Fragment UI to call ViewModel method
5. Observe `AuthUiState` and navigate on success
6. Add test case to `AuthTestCases.md`

### Modifying UI State Model
- Keep existing factory methods; add new static factory if adding state variant
- Never make `AuthUiState` constructor public (use factory methods for encapsulation)
- Update all ViewModels using `AuthUiState` if structure changes

---

## 9. Testing Quick Start

Run unit tests before pushing:
```powershell
./gradlew :app:testDebugUnitTest
```

Test patterns:
- ViewModel tests: mock Repository, verify state transitions
- Repository tests: mock FirebaseXDataSource, verify callback invocation
- See `AuthTestCases.md` for manual regression checklist

---

## 10. Environment & Tools

- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36 (Android 15)
- **Java**: 11+ (source/target compatibility)
- **Gradle**: 8.x wrapper (in repo)
- **Firebase BOM**: 34.11.0
- **IDE**: Android Studio (recommended) or IntelliJ with Android plugin

**Quick debug**:
- Emulator: Android Studio AVD Manager or `emulator -avd <name>`
- Logcat view UI in Android Studio or `adb logcat`
- Firebase Console for auth/Firestore inspection in real-time

---

## 11. Vietnamese Context

Project team uses Vietnamese (Tiếng Việt) in documentation and comments. Key terms:
- **Đăng nhập** = Login, **Đăng ký** = Register, **Đăng xuất** = Logout
- **Từ vựng** = Vocabulary, **Flashcard** = Thẻ từ
- **Màn hình khóa** = Lock screen, **Học thụ động** = Passive learning
- **SRS** = Spaced Repetition System for review scheduling
- When reading comments or docs, expect Vietnamese terminology

---

## 12. Known Patterns & Gotchas

1. **Email verification**: `isEmailVerified()` check is strict; dev testing may need to bypass in debug builds
2. **Firestore offline persistence**: Not yet implemented; all reads are online-only (add `enablePersistence()` when needed)
3. **No coroutines yet**: Async code uses callbacks; migration to coroutines is planned
4. **ViewBinding required**: Never use `findViewById()` for this project
5. **Auth logs**: Every auth event (login, register, error) is logged to `users/{uid}/auth_logs` for audit trail
6. **Timestamps**: Use Firebase server timestamp (`FieldValue.serverTimestamp()`) for sync conflicts

---

**Last Updated**: May 2026 | For latest changes, check `app/DOCS/` and `JOURNAL.jsonl`
