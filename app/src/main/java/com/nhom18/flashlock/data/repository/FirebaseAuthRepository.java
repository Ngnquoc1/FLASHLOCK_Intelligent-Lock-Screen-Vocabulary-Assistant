package com.nhom18.flashlock.data.repository;

import com.nhom18.flashlock.data.remote.FirebaseAuthDataSource;

public class FirebaseAuthRepository implements AuthRepository {

    private final FirebaseAuthDataSource authDataSource;

    public FirebaseAuthRepository(FirebaseAuthDataSource authDataSource) {
        this.authDataSource = authDataSource;
    }

    @Override
    public void signInWithEmail(String email, String password, AuthResultCallback callback) {
        authDataSource.signInWithEmail(email, password, callback);
    }

    @Override
    public void registerWithEmail(String name, String email, String password, AuthResultCallback callback) {
        authDataSource.registerWithEmail(name, email, password, callback);
    }

    @Override
    public void sendPasswordResetEmail(String email, AuthResultCallback callback) {
        authDataSource.sendPasswordResetEmail(email, callback);
    }

    @Override
    public void signInWithGoogleIdToken(String idToken, AuthResultCallback callback) {
        authDataSource.signInWithGoogleIdToken(idToken, callback);
    }
}
