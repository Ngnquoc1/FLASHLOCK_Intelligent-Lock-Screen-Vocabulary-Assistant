package com.nhom18.flashlock.data.remote;

import android.net.Uri;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.nhom18.flashlock.data.model.UserProfile;
import java.util.HashMap;
import java.util.Map;

public class FirebaseProfileDataSource {
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final FirebaseAuth auth;

    public FirebaseProfileDataSource() {
        this.db = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public String getCurrentUid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    public Task<DocumentSnapshot> getUserProfile(String uid) {
        return db.collection("users").document(uid).get();
    }

    public Task<Void> updateProfile(String uid, String displayName, UserProfile.Settings settings) {
        Map<String, Object> updates = new HashMap<>();
        if (displayName != null) updates.put("displayName", displayName);
        if (settings != null) updates.put("settings", settings);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        return db.collection("users").document(uid).update(updates);
    }

    public Task<Uri> uploadAvatar(String uid, Uri imageUri) {
        String path = "avatars/" + uid + "/avatar_" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child(path);

        return ref.putFile(imageUri).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return ref.getDownloadUrl();
        });
    }

    public Task<Void> updateAvatarInfo(String uid, String url, String path) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("avatarUrl", url);
        updates.put("avatarPath", path);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        return db.collection("users").document(uid).update(updates);
    }
}
