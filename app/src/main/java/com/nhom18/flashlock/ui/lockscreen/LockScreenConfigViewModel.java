package com.nhom18.flashlock.ui.lockscreen;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.nhom18.flashlock.data.model.Topic;
import com.nhom18.flashlock.data.model.UserProfile;
import com.nhom18.flashlock.data.repository.ProfileRepository;
import com.nhom18.flashlock.data.repository.SavedTopicRepository;

import java.util.ArrayList;
import java.util.List;

public class LockScreenConfigViewModel extends ViewModel {

    private final ProfileRepository profileRepository;
    private final SavedTopicRepository savedTopicRepository;

    private final MutableLiveData<UserProfile> profile = new MutableLiveData<>(null);
    private final MutableLiveData<List<Topic>> topics = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>(null);

    public LockScreenConfigViewModel(ProfileRepository profileRepository,
                                    SavedTopicRepository savedTopicRepository) {
        this.profileRepository = profileRepository;
        this.savedTopicRepository = savedTopicRepository;
    }

    public LiveData<UserProfile> getProfile() {
        return profile;
    }

    public LiveData<List<Topic>> getTopics() {
        return topics;
    }

    public LiveData<Boolean> getSaving() {
        return saving;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void load() {
        profileRepository.getCurrentUserProfile().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                profile.postValue(task.getResult());
            } else {
                error.postValue(task.getException() != null ? task.getException().getMessage() : "LOAD_PROFILE_FAILED");
            }
        });

        savedTopicRepository.getSavedTopics().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                topics.postValue(task.getResult());
            }
        });
    }

    public void saveSettings(String displayName, UserProfile.Settings settings) {
        saving.setValue(true);
        profileRepository.updateProfile(displayName, settings).addOnCompleteListener(task -> {
            saving.postValue(false);
            if (!task.isSuccessful()) {
                error.postValue(task.getException() != null ? task.getException().getMessage() : "SAVE_SETTINGS_FAILED");
            }
        });
    }
}
