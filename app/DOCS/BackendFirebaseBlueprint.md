# FlashLock Firebase Backend Blueprint

## 1. Scope and Principles

- Platform: Firebase Auth, Firestore, Storage, Cloud Functions, FCM, App Check.
- Architecture: MVVM + Repository (UI -> ViewModel -> Repository -> Remote Data Source).
- Security model: deny-by-default, per-user ownership, server-only writes for sensitive fields.
- Sync strategy: server timestamp as source of truth, offline-first on client.

## 2. Firebase Services Mapping

- `FirebaseAuth`: Email/Password, Google Sign-In, password reset.
- `CloudFirestore`: user profile, vocabulary, learning progress, sync metadata.
- `FirebaseStorage`: avatar image and optional media assets.
- `CloudFunctions`: SRS scheduling, AI example generation proxy, sensitive write validation.
- `FCM`: reminders and re-engagement notifications.
- `AppCheck`: Play Integrity provider to reduce abusive traffic.

## 3. Firestore Data Model

### 3.1 Collections

- `users/{uid}`
  - `displayName: string`
  - `email: string`
  - `avatarUrl: string`
  - `createdAt: timestamp`
  - `updatedAt: timestamp`
  - `lastSyncAt: timestamp`
  - `settings: map { lockScreenEnabled, reminderHour, reminderMinute }`

- `users/{uid}/my_words/{wordId}`
  - `term: string`
  - `definition: string`
  - `example: string`
  - `topicId: string|null`
  - `srsLevel: number`
  - `nextReviewAt: timestamp`
  - `lastReviewedAt: timestamp|null`
  - `status: string` (`new`, `learning`, `mastered`)
  - `source: string` (`manual`, `library`, `ai`)
  - `createdAt: timestamp`
  - `updatedAt: timestamp`

- `topics/{topicId}`
  - `name: string`
  - `description: string`
  - `icon: string`
  - `tags: array<string>`
  - `wordCount: number`
  - `isPublished: boolean`
  - `updatedAt: timestamp`

- `topics/{topicId}/words/{wordId}`
  - `term: string`
  - `definition: string`
  - `example: string`
  - `phonetic: string`
  - `audioUrl: string|null`
  - `difficulty: number`

- `users/{uid}/study_events/{eventId}`
  - `wordId: string`
  - `eventType: string` (`heard`, `remembered`, `forgot`, `skipped`)
  - `sessionId: string`
  - `createdAt: timestamp`

- `users/{uid}/sync_logs/{logId}`
  - `direction: string` (`push`, `pull`, `merge`)
  - `entity: string`
  - `result: string` (`success`, `conflict`, `failed`)
  - `createdAt: timestamp`
  - `traceId: string`

## 4. Security Rules Policy

- User documents only readable/writable by the document owner (`request.auth.uid == uid`).
- `topics/*` readable by authenticated users, writable only by admin claim.
- Deny client updates to protected fields (`createdAt`, `serverComputedScore`).
- Storage path ownership policy: `avatars/{uid}/...` only writable by owner.

## 5. Cloud Functions Responsibilities

- `onStudyEventWrite`: recompute SRS level and `nextReviewAt` on server.
- `callGenerateExample`: callable function proxying Gemini API with abuse checks.
- `onAuthCreate`: bootstrap `users/{uid}` document.
- `sendDailyReminder`: scheduled push notification job.

## 6. Android Data Layer Contracts

- `AuthRepository`: sign in, register, reset password.
- `WordRepository`: CRUD my words, observe due words.
- `SyncRepository`: trigger sync, read last sync state.
- `ProfileRepository`: update profile and avatar.

## 7. Rollout Milestones

1. M1: Firebase dependencies + auth repository integration.
2. M2: Firestore profile/word schema + rules baseline.
3. M3: SRS Cloud Functions and event-driven updates.
4. M4: App Check + monitoring + production hardening.

## 8. Definition of Done

- All auth screens use repository-backed Firebase calls.
- Firestore and Storage rules validated in emulator tests.
- Critical flows have telemetry (`traceId`) and retry strategy.
- JOURNAL includes completion logs for each milestone.
