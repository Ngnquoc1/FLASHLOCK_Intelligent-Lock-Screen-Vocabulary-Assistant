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
                    syncUserProfile(authResult.getUser(), "password");
                    logAuthEvent(authResult.getUser(), "login_email", "success", "");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    logAuthEvent(null, "login_email", "failed", resolveErrorMessage(e));
                    callback.onError(resolveErrorMessage(e));
                });
    }

    public void registerWithEmail(String email, String password, AuthResultCallback callback) {
        String normalizedEmail = normalizeEmail(email);
        firebaseAuth.createUserWithEmailAndPassword(normalizedEmail, password)
                .addOnSuccessListener(authResult -> {
                    syncUserProfile(authResult.getUser(), "password");
                    logAuthEvent(authResult.getUser(), "register_email", "success", "");
                    callback.onSuccess();
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

    private void syncUserProfile(FirebaseUser user, String provider) {
        AuthUserProfile profile = AuthUserProfile.fromFirebaseUser(user, provider);
        if (profile == null) {
            return;
        }

        String uid = profile.getUid();
        Map<String, Object> payload = profile.toFirestoreMap();

        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        payload.put("createdAt", FieldValue.serverTimestamp());
                    }
                    firestore.collection("users").document(uid)
                            .set(payload, SetOptions.merge());
                })
                .addOnFailureListener(e -> firestore.collection("users").document(uid)
                        .set(payload, SetOptions.merge()));
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
