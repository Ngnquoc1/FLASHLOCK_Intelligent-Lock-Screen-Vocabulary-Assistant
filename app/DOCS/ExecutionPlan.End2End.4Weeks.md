# FlashLock End-to-End Technical Execution Plan (4 Weeks)

Last updated: 2026-04-14
Scope source: `app/DOCS/SPEC.md`, `app/DOCS/BackendFirebaseBlueprint.md`, `app/DOCS/Progress_Report_Full.txt`

## 1) Scope lock and assumptions

- Timeline: 4 weeks from now (current week = Week 1/4).
- Priority: Profile + Avatar first.
- Auth scope lock:
  - Keep: Email/Password, Google Sign-In, email reset, email verification.
  - Remove from current scope: Phone auth and SMS OTP.
- Architecture lock: MVVM + Repository + Firebase (Auth/Firestore/Storage/Functions/FCM/App Check).

## 2) Current implementation status (traceable)

### 2.1 Done (already implemented)

- [x] Core app shell: Splash auth gate + Main container + bottom navbar.
- [x] Auth UI set: Login/Register/ResetAccess/ResetConfirm/SetNewPassword.
- [x] Auth data layer: `AuthRepository`, `FirebaseAuthRepository`, `FirebaseAuthDataSource`.
- [x] Firebase Auth integration: Email/Password + Google + reset email.
- [x] Email verification gate before entering main app.
- [x] Firestore sync baseline after auth (`users/{uid}` + `auth_logs`).
- [x] Firestore/Storage owner-only baseline rules.
- [x] Unit test baseline for auth ViewModel (Login/Register).

Primary evidence:
- `app/DOCS/Progress_Report_Full.txt`
- `app/src/main/java/com/nhom18/flashlock/data/remote/FirebaseAuthDataSource.java`
- `firestore.rules`, `storage.rules`

### 2.2 Not_Done (required by SPEC/Blueprint, not complete yet)

- [ ] Full User Profile module (UI + ViewModel + Repository + DataSource).
- [ ] Avatar production flow (upload, rollback, error states, retry policy).
- [ ] Content module: `my_words` CRUD + topic catalog read.
- [ ] Active learning module (flashcard sessions, remember/forgot/skipped events).
- [ ] Passive learning module (lock screen flow + quick actions).
- [ ] Full Cloud Functions set (`onStudyEventWrite`, `callGenerateExample`, `onAuthCreate`, `sendDailyReminder`).
- [ ] Sync module (`SyncRepository`, `sync_logs`, retry + telemetry `traceId`).
- [ ] Emulator validation for rules and protected fields.
- [ ] App Check hardening + FCM reminder production wiring.
- [ ] End-to-end regression and release hardening.

### 2.3 Out_of_Scope (confirmed removed)

- [x] Phone field in register/profile contract.
- [x] SMS OTP reset/auth flow.

## 3) Technical backlog mapping (feature -> backend)

### 3.1 Profile

- Firestore document: `users/{uid}`
- Storage path: `avatars/{uid}/avatar_<timestamp>.jpg`
- Android contracts to implement:
  - `ProfileRepository`
  - `FirebaseProfileDataSource`
  - `ProfileViewModel`
  - `ProfileUiState`
  - `ProfileFragment` + binding/layout

### 3.2 Content (Vocabulary)

- Firestore:
  - `users/{uid}/my_words/{wordId}`
  - `topics/{topicId}` and `topics/{topicId}/words/{wordId}`
- Android contracts:
  - `WordRepository`
  - `FirebaseWordDataSource`
  - UI screens/fragments for list + CRUD + detail

### 3.3 Learning and Sync

- Firestore:
  - `users/{uid}/study_events/{eventId}`
  - `users/{uid}/sync_logs/{logId}`
- Cloud Functions:
  - `onStudyEventWrite`
  - `callGenerateExample`
  - `onAuthCreate`
  - `sendDailyReminder`
- Android contracts:
  - `SyncRepository`
  - event writer in learning flow

## 4) Week-by-week timeline (4 weeks)

## Week 1/4 (current) - Profile + Avatar foundation (highest priority)

Goal: deliver complete Profile module skeleton and stable avatar upload path.

### Day-by-day checklist (Week 1)

- [ ] Day 1: finalize profile contract and rule policy
  - Align `users/{uid}` fields with no `phone`.
  - Split editable vs protected fields.
  - Update `firestore.rules` for profile field whitelist and validation.

- [ ] Day 2: implement data layer
  - Add `ProfileRepository` interface.
  - Add `FirebaseProfileDataSource`.
  - Add `FirebaseProfileRepository`.

- [ ] Day 3: implement ViewModel + UI state
  - Add `ProfileUiState` (`IDLE/LOADING/CONTENT/SAVING/SUCCESS/ERROR`).
  - Add `ProfileViewModel` load/update methods.

- [ ] Day 4: implement Profile UI and nav wiring
  - Add `fragment_profile.xml`.
  - Add `ProfileFragment` with View Binding.
  - Wire Profile tab in `MainActivity`.

- [ ] Day 5: implement avatar upload
  - Pick image, validate type/size, upload to `avatars/{uid}/...`.
  - Save `avatarUrl`, `avatarPath`, `updatedAt` to Firestore.
  - Handle cancellation, network failure, and retry.

- [ ] Day 6: security and verification
  - Harden `storage.rules` (owner path + image + size limit).
  - Manual security checks (A cannot read/write B data).

- [ ] Day 7: stabilization and docs
  - Add/update tests and checklist.
  - Update `JOURNAL.jsonl` and progress docs.

Week 1 DoD:
- Profile screen can load/update own profile.
- Avatar upload works end-to-end with proper error handling.
- Rules protect owner-only access.

## Week 2/4 - Content core (My Vocabulary + Topic library)

Goal: implement user vocabulary and topic read flow.

- [ ] Implement `WordRepository` + datasource methods:
  - create/update/delete/get/list my words
  - observe due words (basic query)
- [ ] Build UI for My Vocabulary CRUD.
- [ ] Build read-only topic catalog (`topics/*`) for authenticated users.
- [ ] Add Firestore composite indexes if required by query.
- [ ] Add rule checks for:
  - owner-only `users/{uid}/my_words/*`
  - authenticated read/admin write for `topics/*`
- [ ] Add tests for repository + ViewModel paths.

Week 2 DoD:
- User can CRUD own words and cannot touch other users' words.
- Topic catalog is readable in app for signed-in users.

## Week 3/4 - Learning flow + Sync logging

Goal: complete active/passive learning data write paths and sync observability.

- [ ] Active learning:
  - flashcard session state
  - submit `remembered/forgot/skipped` events
- [ ] Passive learning baseline:
  - lock-screen trigger pipeline (or service scaffold if UI pending)
  - quick actions map to study events
- [ ] Sync module:
  - implement `SyncRepository`
  - write `sync_logs` with `traceId`, result, direction
  - retry strategy for failed writes
- [ ] Update analytics/log conventions for critical flows.

Week 3 DoD:
- Study events and sync logs are written with traceability.
- App behavior remains user-scoped and recoverable on network issues.

## Week 4/4 - Full backend hardening and release gate

Goal: complete full function scope and production hardening.

- [ ] Cloud Functions (full scope)
  - `onStudyEventWrite`: recompute SRS + next review server-side.
  - `callGenerateExample`: proxy AI generation with abuse checks.
  - `onAuthCreate`: bootstrap `users/{uid}` defaults.
  - `sendDailyReminder`: schedule reminders through FCM.
- [ ] App Check production setup (Play Integrity).
- [ ] FCM integration for reminder notifications.
- [ ] Rules emulator tests and protected field tests.
- [ ] End-to-end regression:
  - auth + profile + avatar + content + learning + reminders.
- [ ] Release readiness:
  - telemetry checks (`traceId`), retry checks, docs and rollout notes.

Week 4 DoD:
- All core flows pass E2E test matrix.
- Functions + App Check + FCM are active and validated.
- Release candidate branch is merge-ready.

## 5) Implementation order by technical layers

1. Data model and contracts (`users`, `my_words`, `study_events`, `sync_logs`).
2. Security rules (`firestore.rules`, `storage.rules`) with deny-by-default.
3. Repository and remote data source layer.
4. ViewModel and UI state.
5. UI wiring and navigation.
6. Functions/FCM/App Check.
7. Tests, telemetry, and release gate.

## 6) Required manual Firebase operations

- Enable and verify services:
  - Authentication (Email/Password + Google)
  - Firestore Database (production mode)
  - Storage bucket
  - Cloud Functions (billing/project setup if needed)
  - Cloud Messaging
  - App Check (Play Integrity)
- Deploy rules/functions by environment.
- Create test users A/B for ownership security tests.

## 7) Risk controls and mitigations

- Schema drift risk:
  - Keep one source of truth in model contract docs.
- Rule over-restriction risk:
  - Validate with emulator before production deploy.
- Rule under-restriction risk:
  - Add negative tests for cross-user write/read.
- Upload failure risk:
  - Add retry and rollback policy for avatar flow.
- Function abuse risk:
  - Callable auth check, quota, and request validation.

## 8) Reporting and tracking format

- Update `JOURNAL.jsonl` at each completed milestone.
- Update `app/DOCS/Progress_Report_Full.txt` at end of each week.
- Keep `Done/Not_Done` checklist synchronized with this plan.

## 9) Weekly status template

Use this weekly format in team updates:

- Week: W#/4
- Completed:
  - [ ] item 1
  - [ ] item 2
- In progress:
  - [ ] item
- Blockers:
  - blocker + owner + ETA
- Next week commit goals:
  - commit title patterns and target modules

## 10) Final definition of success (end of Week 4)

- Auth + Profile + Avatar + Content + Learning + Reminder flows run end-to-end.
- Data is user-scoped by uid and protected by rules.
- Cloud Functions operate for SRS, AI example, bootstrap, and reminders.
- App Check and FCM are active in production configuration.
- Regression and security checklist are all passed.

