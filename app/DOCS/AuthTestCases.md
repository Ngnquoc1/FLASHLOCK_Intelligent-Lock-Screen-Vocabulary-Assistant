# Auth Test Cases

## Email Verification Flow

1. Register new account with valid `name/email/password`.
   - Expected: app shows success message asking user to verify email.
   - Expected: user is signed out and redirected to Login screen.

2. Attempt login before clicking verification link.
   - Expected: login is blocked with message `Tai khoan chua xac thuc email...`.
   - Expected: user is not navigated to `MainActivity`.

3. Open verification email and click the verification link.
   - Expected: Firebase Authentication marks `emailVerified = true` for that user.

4. Login again after verification.
   - Expected: login succeeds and app navigates to `MainActivity`.
   - Expected: Firestore `users/{uid}` is upserted with profile fields and timestamps.

5. Register when email service is unavailable (simulate network issue).
   - Expected: registration returns `AUTH_VERIFY_EMAIL_SEND_FAILED` if verify mail cannot be sent.

## Regression Checks

1. Google Sign-In still works and bypasses email/password verification gate.
2. Password reset via email still sends reset link successfully.
3. Existing error mappings (`AUTH_INVALID_EMAIL`, `AUTH_WEAK_PASSWORD`, etc.) still display correct messages.

