package com.nhom18.flashlock.ui.register;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class RegisterViewModel extends ViewModel {

    private final MutableLiveData<Boolean> registerStatus = new MutableLiveData<>();

    public LiveData<Boolean> getRegisterStatus() {
        return registerStatus;
    }

    public void register(String name, String email, String phone, String password) {
        // TODO: Implement Firebase Auth logic here later
        // Example logic abstraction
        if (name != null && !name.isEmpty() &&
            email != null && !email.isEmpty() &&
            password != null && !password.isEmpty()) {
            registerStatus.setValue(true);
        } else {
            registerStatus.setValue(false);
        }
    }
}

