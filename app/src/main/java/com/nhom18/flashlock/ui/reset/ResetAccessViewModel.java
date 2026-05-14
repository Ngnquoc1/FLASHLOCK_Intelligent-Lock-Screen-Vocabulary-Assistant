package com.nhom18.flashlock.ui.reset;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.nhom18.flashlock.data.remote.FirebaseAuthDataSource;
import com.nhom18.flashlock.data.repository.AuthRepository;
import com.nhom18.flashlock.data.repository.AuthResultCallback;
import com.nhom18.flashlock.data.repository.FirebaseAuthRepository;
import com.nhom18.flashlock.ui.auth.AuthUiState;

import java.util.regex.Pattern;

public class ResetAccessViewModel extends ViewModel {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );

    private final AuthRepository authRepository;
    private final MutableLiveData<AuthUiState> authUiState = new MutableLiveData<>(AuthUiState.idle());

    public ResetAccessViewModel() {
        this(new FirebaseAuthRepository(new FirebaseAuthDataSource()));
    }

    ResetAccessViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<AuthUiState> getAuthUiState() {
        return authUiState;
    }

    public void sendResetLink(String email) {
        String emailValue = email == null ? "" : email.trim();

        if (emailValue.isEmpty()) {
            authUiState.setValue(AuthUiState.error("RESET_EMAIL_REQUIRED"));
            return;
        }
        if (!EMAIL_PATTERN.matcher(emailValue).matches()) {
            authUiState.setValue(AuthUiState.error("RESET_EMAIL_INVALID"));
            return;
        }

        authUiState.setValue(AuthUiState.loading());
        authRepository.sendPasswordResetEmail(emailValue, new AuthResultCallback() {
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