package com.nhom18.flashlock.ui.profile;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.nhom18.flashlock.data.model.UserProfile;
import com.nhom18.flashlock.data.repository.ProfileRepository;

public class ProfileViewModel extends ViewModel {
    private final ProfileRepository repository;
    private final MutableLiveData<ProfileUiState> uiState = new MutableLiveData<>(ProfileUiState.idle());
    private final MutableLiveData<Boolean> navigateToLogin = new MutableLiveData<>(false);

    public ProfileViewModel(ProfileRepository repository) {
        this.repository = repository;
    }

    public LiveData<ProfileUiState> getUiState() {
        return uiState;
    }

    public LiveData<Boolean> getNavigateToLogin() {
        return navigateToLogin;
    }

    public void loadProfile() {
        uiState.setValue(ProfileUiState.loading());
        repository.getCurrentUserProfile().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                uiState.setValue(ProfileUiState.content(task.getResult()));
            } else {
                uiState.setValue(ProfileUiState.error(task.getException() != null ? task.getException().getMessage() : "Unknown error"));
            }
        });
    }

    public void onSaveProfile(String displayName, UserProfile.Settings settings) {
        UserProfile currentProfile = uiState.getValue() != null ? uiState.getValue().getUserProfile() : null;
        uiState.setValue(ProfileUiState.saving(currentProfile));

        repository.updateProfile(displayName, settings).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                loadProfile();
            } else {
                uiState.setValue(ProfileUiState.error(task.getException() != null ? task.getException().getMessage() : "Update failed"));
            }
        });
    }

    public void onAvatarPicked(Uri uri) {
        UserProfile currentProfile = uiState.getValue() != null ? uiState.getValue().getUserProfile() : null;
        uiState.setValue(ProfileUiState.saving(currentProfile));

        repository.uploadAvatar(uri).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                loadProfile();
            } else {
                uiState.setValue(ProfileUiState.error(task.getException() != null ? task.getException().getMessage() : "Upload failed"));
            }
        });
    }

    public void onLogout() {
        FirebaseAuth.getInstance().signOut();
        navigateToLogin.setValue(true);
    }

    public void onRetry() {
        loadProfile();
    }
}
