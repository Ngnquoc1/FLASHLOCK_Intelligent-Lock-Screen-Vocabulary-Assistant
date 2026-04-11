package com.nhom18.flashlock.data.repository;

import android.net.Uri;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.nhom18.flashlock.data.model.UserProfile;
import com.nhom18.flashlock.data.remote.FirebaseProfileDataSource;

public class FirebaseProfileRepository implements ProfileRepository {
    private final FirebaseProfileDataSource dataSource;

    public FirebaseProfileRepository(FirebaseProfileDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Task<UserProfile> getCurrentUserProfile() {
        String uid = dataSource.getCurrentUid();
        if (uid == null) {
            return Tasks.forException(new Exception("User not logged in"));
        }

        return dataSource.getUserProfile(uid).continueWith(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return task.getResult().toObject(UserProfile.class);
        });
    }

    @Override
    public Task<Void> updateProfile(String displayName, UserProfile.Settings settings) {
        String uid = dataSource.getCurrentUid();
        if (uid == null) {
            return Tasks.forException(new Exception("User not logged in"));
        }
        return dataSource.updateProfile(uid, displayName, settings);
    }

    @Override
    public Task<String> uploadAvatar(Uri imageUri) {
        String uid = dataSource.getCurrentUid();
        if (uid == null) {
            return Tasks.forException(new Exception("User not logged in"));
        }

        return dataSource.uploadAvatar(uid, imageUri).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            Uri downloadUri = task.getResult();
            String url = downloadUri.toString();
            String path = "avatars/" + uid + "/" + imageUri.getLastPathSegment();
            
            // Sau khi upload Storage xong, cap nhat URL vao Firestore
            return dataSource.updateAvatarInfo(uid, url, path).continueWith(updateTask -> {
                if (!updateTask.isSuccessful()) {
                    throw updateTask.getException();
                }
                return url;
            });
        });
    }
}
