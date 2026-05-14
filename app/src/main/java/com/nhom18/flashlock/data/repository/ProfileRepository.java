package com.nhom18.flashlock.data.repository;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import com.google.android.gms.tasks.Task;
import com.nhom18.flashlock.data.model.UserProfile;

public interface ProfileRepository {
    Task<UserProfile> getCurrentUserProfile();
    Task<Void> updateProfile(String displayName, UserProfile.Settings settings);
    Task<String> uploadAvatar(Uri imageUri);
}
