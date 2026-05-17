package com.nhom18.flashlock.data.model;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class AuthUserProfile {

    private final String uid;
    private final String email;
    private final String displayName;
    private final String avatarUrl;
    private final String provider;

    public AuthUserProfile(String uid, String email, String displayName, String avatarUrl, String provider) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.provider = provider;
    }

    public static AuthUserProfile fromFirebaseUser(FirebaseUser user, String provider) {
        if (user == null) {
            return null;
        }

        String photo = (user.getPhotoUrl() != null) ? user.getPhotoUrl().toString() : "";

        return new AuthUserProfile(
                user.getUid(),
                user.getEmail() == null ? "" : user.getEmail(),
                user.getDisplayName() == null ? "" : user.getDisplayName(),
                photo,
                provider
        );
    }

    public String getUid() {
        return uid;
    }

    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("displayName", displayName);
        payload.put("avatarUrl", avatarUrl);
        payload.put("provider", provider);
        payload.put("status", "active");
        payload.put("updatedAt", FieldValue.serverTimestamp());
        payload.put("lastLoginAt", FieldValue.serverTimestamp());
        return payload;
    }
}