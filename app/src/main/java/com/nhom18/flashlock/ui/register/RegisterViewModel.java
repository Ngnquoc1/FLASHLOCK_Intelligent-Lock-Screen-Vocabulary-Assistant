package com.nhom18.flashlock.ui.register;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.nhom18.flashlock.data.remote.FirebaseAuthDataSource;
import com.nhom18.flashlock.data.repository.AuthRepository;
import com.nhom18.flashlock.data.repository.AuthResultCallback;
import com.nhom18.flashlock.data.repository.FirebaseAuthRepository;
import com.nhom18.flashlock.ui.auth.AuthUiState;

public class RegisterViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final MutableLiveData<AuthUiState> authUiState = new MutableLiveData<>(AuthUiState.idle());

    public RegisterViewModel() {
        this(new FirebaseAuthRepository(new FirebaseAuthDataSource()));
    }

    RegisterViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<AuthUiState> getAuthUiState() {
        return authUiState;
    }

    public void register(String name, String email, String password) {
        String nameValue = name == null ? "" : name.trim();
        String emailValue = email == null ? "" : email.trim();
        String passwordValue = password == null ? "" : password.trim();

        if (nameValue.isEmpty()) {
            authUiState.setValue(AuthUiState.error("REGISTER_NAME_REQUIRED"));
            return;
        }
        if (emailValue.isEmpty()) {
            authUiState.setValue(AuthUiState.error("REGISTER_EMAIL_REQUIRED"));
            return;
        }
        if (passwordValue.isEmpty()) {
            authUiState.setValue(AuthUiState.error("REGISTER_PASSWORD_REQUIRED"));
            return;
        }

        authUiState.setValue(AuthUiState.loading());
        authRepository.registerWithEmail(nameValue, emailValue, passwordValue, new AuthResultCallback() {
            @Override
            public void onSuccess() {
                authUiState.postValue(AuthUiState.success());
            }

            @Override
            public void onError(String message) {
                authUiState.postValue(AuthUiState.error(message));
            }
        });
    }
}