package com.nhom18.flashlock.ui.login;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class LoginViewModel extends ViewModel {

    private final MutableLiveData<Boolean> loginStatus = new MutableLiveData<>();

    public LiveData<Boolean> getLoginStatus() {
        return loginStatus;
    }

    public void login(String email, String password) {
        // TODO: Implement Firebase Auth logic here later
        // Example logic abstraction
        if (email != null && !email.isEmpty() && password != null && !password.isEmpty()) {
            loginStatus.setValue(true);
        } else {
            loginStatus.setValue(false);
        }
    }

    public void loginWithGoogle() {
        // TODO: Implement Firebase Google Sign In logic here later
    }
}
