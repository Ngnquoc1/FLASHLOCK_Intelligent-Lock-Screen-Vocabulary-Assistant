package com.nhom18.flashlock.data.repository;

import android.net.Uri;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.nhom18.flashlock.data.model.UserProfile;
import com.nhom18.flashlock.data.remote.FirebaseProfileDataSource;
import java.util.Date; // Nhớ import Date

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

        String path = "avatars/" + uid + "/avatar_" + System.currentTimeMillis() + ".jpg";

        return dataSource.uploadAvatar(path, imageUri).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }

            String url = task.getResult().toString();

            return dataSource.updateAvatarInfo(uid, url, path).continueWithTask(updateTask -> {
                if (!updateTask.isSuccessful()) {
                    dataSource.deleteFile(path);
                    throw updateTask.getException();
                }
                return Tasks.forResult(url);
            });
        });
    }

    @Override
    public Task<Void> updateUserStreak(int newStreak, Date lastCompletedDate) {
        String uid = dataSource.getCurrentUid();
        if (uid == null) {
            return Tasks.forException(new Exception("User not logged in"));
        }

        return dataSource.updateUserStreak(uid, newStreak, lastCompletedDate);
    }
}