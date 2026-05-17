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
                    FirebaseUser user = authResult.getUser();
                    if (user == null) {
                        callback.onError("AUTH_UNKNOWN_ERROR");
                        return;
                    }

                    boolean isNewUser = authResult.getAdditionalUserInfo() != null &&
                            authResult.getAdditionalUserInfo().isNewUser();

                    if (isNewUser) {
                        user.sendEmailVerification()
                                .addOnSuccessListener(unused -> {
                                    logAuthEvent(user, "register_google", "pending_verification", "");
                                    firebaseAuth.signOut();
                                    callback.onError("AUTH_EMAIL_NOT_VERIFIED");
                                })
                                .addOnFailureListener(e -> {
                                    callback.onError("AUTH_VERIFY_EMAIL_SEND_FAILED");
                                });
                    } else {
                        if (user.isEmailVerified()) {
                            syncUserProfile(user, "google");
                            logAuthEvent(user, "login_google", "success", "");
                            callback.onSuccess();
                        } else {
                            firebaseAuth.signOut();
                            callback.onError("AUTH_EMAIL_NOT_VERIFIED");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onError(resolveErrorMessage(e));
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
        AuthUserProfile authProfile = AuthUserProfile.fromFirebaseUser(user, provider);
        if (authProfile == null) return;

        String uid = authProfile.getUid();

        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        // USER MỚI
                        Map<String, Object> payload = authProfile.toFirestoreMap();
                        payload.put("createdAt", FieldValue.serverTimestamp());
                        payload.put("lastLoginAt", FieldValue.serverTimestamp());

                        String normalizedName = normalizeValue(name);
                        if (!provider.equals("google") && !normalizedName.isEmpty()) {
                            payload.put("displayName", normalizedName);
                        }

                        firestore.collection("users").document(uid).set(payload);
                    } else {
                        // USER CŨ QUAY LẠI
                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("lastLoginAt", FieldValue.serverTimestamp());

                        updateData.put("email", user.getEmail());

                        firestore.collection("users").document(uid).update(updateData);
                    }
                })
                .addOnFailureListener(e -> {

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
        if (exception == null) return "AUTH_UNKNOWN_ERROR";

        if (exception instanceof FirebaseNetworkException) return "AUTH_NETWORK_ERROR";
        if (exception instanceof FirebaseTooManyRequestsException) return "AUTH_TOO_MANY_REQUESTS";

        if (exception instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) exception).getErrorCode();

            switch (code) {
                case "ERROR_INVALID_EMAIL": return "AUTH_INVALID_EMAIL";
                case "ERROR_USER_NOT_FOUND": return "AUTH_USER_NOT_FOUND";
                case "ERROR_WRONG_PASSWORD":
                case "ERROR_INVALID_CREDENTIAL": return "AUTH_WRONG_PASSWORD";

                // Lỗi khi email đã liên kết với phương thức khác (Email vs Google)
                case "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL":
                case "ERROR_EMAIL_ALREADY_IN_USE": return "AUTH_EMAIL_ALREADY_IN_USE";

                case "ERROR_INVALID_IDP_RESPONSE":
                case "ERROR_CREDENTIAL_ALREADY_IN_USE": return "AUTH_GOOGLE_TOKEN_INVALID";

                case "ERROR_WEAK_PASSWORD": return "AUTH_WEAK_PASSWORD";
                case "ERROR_USER_DISABLED": return "AUTH_USER_DISABLED";
                default:
                    return "AUTH_UNKNOWN_ERROR: " + code;
            }
        }
        return "AUTH_ACTION_FAILED: " + exception.getMessage();
    }
}
