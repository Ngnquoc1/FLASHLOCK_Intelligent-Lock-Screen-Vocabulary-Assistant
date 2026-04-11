package com.nhom18.flashlock.data.remote;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.nhom18.flashlock.data.model.AuthUserProfile;
import com.nhom18.flashlock.data.repository.AuthResultCallback;

import java.util.HashMap;
import java.util.Map;

public class FirebaseAuthDataSource {

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;

    public FirebaseAuthDataSource() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void signInWithEmail(String email, String password, AuthResultCallback callback) {
        String normalizedEmail = normalizeEmail(email);
        firebaseAuth.signInWithEmailAndPassword(normalizedEmail, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user == null || !user.isEmailVerified()) {
                        logAuthEvent(user, "login_email", "failed", "AUTH_EMAIL_NOT_VERIFIED");
                        firebaseAuth.signOut();
                        callback.onError("AUTH_EMAIL_NOT_VERIFIED");
                        return;
                    }

                    syncUserProfile(user, "password");
                    logAuthEvent(user, "login_email", "success", "");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    logAuthEvent(null, "login_email", "failed", resolveErrorMessage(e));
                    callback.onError(resolveErrorMessage(e));
                });
    }

    public void registerWithEmail(String name, String email, String password, AuthResultCallback callback) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedName = normalizeValue(name);

        firebaseAuth.createUserWithEmailAndPassword(normalizedEmail, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    syncUserProfile(user, "password", normalizedName);
                    logAuthEvent(user, "register_email", "success", "");

                    if (user == null) {
                        callback.onError("AUTH_VERIFY_EMAIL_SEND_FAILED");
                        return;
                    }

                    user.sendEmailVerification()
                            .addOnSuccessListener(unused -> {
                                firebaseAuth.signOut();
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                logAuthEvent(user, "verify_email", "failed", "AUTH_VERIFY_EMAIL_SEND_FAILED");
                                callback.onError("AUTH_VERIFY_EMAIL_SEND_FAILED");
                            });
                })
                .addOnFailureListener(e -> {
                    logAuthEvent(null, "register_email", "failed", resolveErrorMessage(e));
                    callback.onError(resolveErrorMessage(e));
                });
    }

    public void sendPasswordResetEmail(String email, AuthResultCallback callback) {
        String normalizedEmail = normalizeEmail(email);
        firebaseAuth.sendPasswordResetEmail(normalizedEmail)
                .addOnSuccessListener(unused -> {
                    FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                    logAuthEvent(currentUser, "reset_password", "success", "");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    logAuthEvent(firebaseAuth.getCurrentUser(), "reset_password", "failed", resolveErrorMessage(e));
                    callback.onError(resolveErrorMessage(e));
                });
    }

    public void signInWithGoogleIdToken(String idToken, AuthResultCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    syncUserProfile(authResult.getUser(), "google");
                    logAuthEvent(authResult.getUser(), "login_google", "success", "");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthException) {
                        String code = ((FirebaseAuthException) e).getErrorCode();
                        if ("ERROR_INVALID_CREDENTIAL".equals(code)) {
                            logAuthEvent(firebaseAuth.getCurrentUser(), "login_google", "failed", "AUTH_GOOGLE_TOKEN_INVALID");
                            callback.onError("AUTH_GOOGLE_TOKEN_INVALID");
                            return;
                        }
                    }
                    String reason = resolveErrorMessage(e);
                    logAuthEvent(firebaseAuth.getCurrentUser(), "login_google", "failed", reason);
                    callback.onError(reason);
                });
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim();
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private void syncUserProfile(FirebaseUser user, String provider) {
        syncUserProfile(user, provider, "");
    }

    private void syncUserProfile(FirebaseUser user, String provider, String name) {
        AuthUserProfile profile = AuthUserProfile.fromFirebaseUser(user, provider);
        if (profile == null) return;

        String uid = profile.getUid();
        Map<String, Object> payload = profile.toFirestoreMap();

        // 1. Luôn cập nhật thời gian đăng nhập cuối
        payload.put("lastLoginAt", FieldValue.serverTimestamp());

        // 2. Xử lý logic ghi đè displayName
        String normalizedName = normalizeValue(name);
        if (!normalizedName.isEmpty()) {
            // Nếu có tên truyền vào (tức là đang ĐĂNG KÝ mới), thì mới cho phép ghi tên này
            payload.put("displayName", normalizedName);
        } else {
            // Nếu đang ĐĂNG NHẬP (name rỗng), hãy XÓA displayName khỏi payload
            // để nó không ghi đè chuỗi rỗng lên tên cũ trong Firestore
            payload.remove("displayName");
        }

        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        payload.put("createdAt", FieldValue.serverTimestamp());
                        // Nếu chưa có document (User mới hoàn toàn), mới dùng .set()
                        firestore.collection("users").document(uid).set(payload, SetOptions.merge());
                    } else {
                        // Nếu đã có document (User cũ đăng nhập lại), chỉ dùng .update()
                        // để cập nhật lastLoginAt mà không đụng chạm đến displayName, avatar...
                        firestore.collection("users").document(uid).update(payload);
                    }
                })
                .addOnFailureListener(e -> {
                    // Fallback an toàn
                    firestore.collection("users").document(uid).set(payload, SetOptions.merge());
                });
    }

    private void logAuthEvent(FirebaseUser user, String event, String result, String reason) {
        if (user == null) {
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("event", event);
        payload.put("result", result);
        payload.put("reason", reason == null ? "" : reason);
        payload.put("createdAt", FieldValue.serverTimestamp());

        firestore.collection("users")
                .document(user.getUid())
                .collection("auth_logs")
                .add(payload);
    }

    private String resolveErrorMessage(Exception exception) {
        if (exception == null) {
            return "AUTH_UNKNOWN_ERROR";
        }

        if (exception instanceof FirebaseNetworkException) {
            return "AUTH_NETWORK_ERROR";
        }

        if (exception instanceof FirebaseTooManyRequestsException) {
            return "AUTH_TOO_MANY_REQUESTS";
        }

        if (exception instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) exception).getErrorCode();

            switch (code) {
                case "ERROR_INVALID_EMAIL":
                    return "AUTH_INVALID_EMAIL";
                case "ERROR_USER_NOT_FOUND":
                    return "AUTH_USER_NOT_FOUND";
                case "ERROR_WRONG_PASSWORD":
                case "ERROR_INVALID_CREDENTIAL": // For newer Firebase versions
                    return "AUTH_WRONG_PASSWORD";
                case "ERROR_INVALID_IDP_RESPONSE":
                case "ERROR_CREDENTIAL_ALREADY_IN_USE":
                    return "AUTH_GOOGLE_TOKEN_INVALID";
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    return "AUTH_EMAIL_ALREADY_IN_USE";
                case "ERROR_WEAK_PASSWORD":
                    return "AUTH_WEAK_PASSWORD";
                case "ERROR_USER_DISABLED":
                    return "AUTH_USER_DISABLED";
                default:
                    return "AUTH_UNKNOWN_ERROR";
            }
        }

        return "AUTH_UNKNOWN_ERROR";
    }
}
