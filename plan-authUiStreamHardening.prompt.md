## Plan: Auth UI Stream Hardening

This draft plan tightens the current auth UI stream by standardizing icons to Material assets, keeping MVVM boundaries clean, and verifying navigation/back-stack behavior across all five auth screens. It focuses on targeted updates in layouts, activities/viewmodels, and manifest/strings while preserving the existing structure and documenting each completed milestone in `JOURNAL.jsonl` for traceability.

### Steps
1. [ ] Inventory auth assets/usages in [layout auth files](app/src/main/res/layout) and map each icon to a Material equivalent (`ic_login`, `ic_register`, `ic_question`, `ic_email`, `ic_email_outline`).
2. [ ] Normalize icon resources in [drawable](app/src/main/res/drawable), replacing PNG auth icons with vectors and keeping `24dp` viewport/size consistency (`ic_email_outline` sizing guard).
3. [ ] Apply icon references and sizing rules in [Reset Access layout](app/src/main/res/layout/activity_reset_access.xml) and related auth layouts (`TextInputLayout` `startIconDrawable`, nav/support icons).
4. [ ] Verify and align screen flow logic in [LoginActivity](app/src/main/java/com/nhom18/flashlock/ui/login/LoginActivity.java), [RegisterActivity](app/src/main/java/com/nhom18/flashlock/ui/register/RegisterActivity.java), [ResetAccessActivity](app/src/main/java/com/nhom18/flashlock/ui/reset/ResetAccessActivity.java), [ResetConfirmActivity](app/src/main/java/com/nhom18/flashlock/ui/reset/ResetConfirmActivity.java), and [SetNewPasswordActivity](app/src/main/java/com/nhom18/flashlock/ui/reset/SetNewPasswordActivity.java) for Login <-> Register <-> Reset Access <-> Reset Confirmation <-> Set New Password.
5. [ ] Enforce MVVM cleanliness by keeping UI-only code in activities and state/validation in `ViewModel` classes under [ui packages](app/src/main/java/com/nhom18/flashlock/ui), then append structured progress entries to [JOURNAL.jsonl](JOURNAL.jsonl) using the existing `{id, task, status, note}` schema.

### Further Considerations
1. Should auth stay split as current packages or move to unified `ui/auth/*`? Option A keep current / Option B reorganize now / Option C defer until feature freeze.
2. For reset-confirm resend action, keep demo jump to `SetNewPasswordActivity` or switch to real resend trigger placeholder? Option A keep demo / Option B soft placeholder / Option C full behavior ticket.
3. For JOURNAL entry granularity, log per screen or one auth-stream entry? Option A per screen / Option B single consolidated / Option C hybrid (icons + flow).
