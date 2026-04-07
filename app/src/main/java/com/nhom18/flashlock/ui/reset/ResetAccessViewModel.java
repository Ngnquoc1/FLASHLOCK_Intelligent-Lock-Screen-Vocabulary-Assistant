package com.nhom18.flashlock.ui.reset;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ResetAccessViewModel extends ViewModel {

    private final MutableLiveData<Boolean> resetStatus = new MutableLiveData<>();

    public LiveData<Boolean> getResetStatus() {
        return resetStatus;
    }

    public void sendResetLink(String email) {
        // TODO: Implement Firebase Auth password reset logic here
        if (email != null && !email.isEmpty()) {
            resetStatus.setValue(true);
        } else {
            resetStatus.setValue(false);
        }
    }
}

