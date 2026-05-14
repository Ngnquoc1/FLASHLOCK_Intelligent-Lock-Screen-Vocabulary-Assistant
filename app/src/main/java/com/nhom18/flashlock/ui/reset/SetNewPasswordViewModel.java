package com.nhom18.flashlock.ui.reset;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SetNewPasswordViewModel extends ViewModel {

    private final MutableLiveData<Boolean> updateStatus = new MutableLiveData<>();
    private final MutableLiveData<Integer> passwordStrength = new MutableLiveData<>(0);

    public LiveData<Boolean> getUpdateStatus() {
        return updateStatus;
    }

    public LiveData<Integer> getPasswordStrength() {
        return passwordStrength;
    }

    public void evaluatePassword(String password) {
        // TODO: Implement actual logic to check password rules
        // Dummy logic
        if (password == null || password.isEmpty()) {
            passwordStrength.setValue(0);
        } else if (password.length() > 8) {
            passwordStrength.setValue(65);
        } else {
            passwordStrength.setValue(30);
        }
    }

    public void updatePassword(String newPassword, String confirmPassword) {
        if (newPassword != null && !newPassword.isEmpty() && newPassword.equals(confirmPassword)) {
            updateStatus.setValue(true);
        } else {
            updateStatus.setValue(false);
        }
    }
}

