package com.nhom18.flashlock.data.repository;

public interface AuthRepository {
    void signInWithEmail(String email, String password, AuthResultCallback callback);
    void registerWithEmail(String email, String password, AuthResultCallback callback);
    void sendPasswordResetEmail(String email, AuthResultCallback callback);
    void signInWithGoogleIdToken(String idToken, AuthResultCallback callback);
}
