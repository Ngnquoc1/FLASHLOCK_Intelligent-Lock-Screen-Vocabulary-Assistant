package com.nhom18.flashlock.ui.login;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.nhom18.flashlock.data.repository.AuthRepository;
import com.nhom18.flashlock.data.repository.AuthResultCallback;
import com.nhom18.flashlock.ui.auth.AuthUiState;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class LoginViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void login_missingEmail_returnsEmailRequiredError() {
        FakeAuthRepository fakeRepo = new FakeAuthRepository(FakeAuthRepository.Mode.SUCCESS, null);
        LoginViewModel viewModel = new LoginViewModel(fakeRepo);

        viewModel.login("", "123456");

        AuthUiState state = viewModel.getAuthUiState().getValue();
        Assert.assertNotNull(state);
        Assert.assertEquals(AuthUiState.Status.ERROR, state.getStatus());
        Assert.assertEquals("LOGIN_EMAIL_REQUIRED", state.getMessage());
    }

    @Test
    public void login_emailNotVerified_returnsNotVerifiedError() {
        FakeAuthRepository fakeRepo = new FakeAuthRepository(FakeAuthRepository.Mode.FAIL, "AUTH_EMAIL_NOT_VERIFIED");
        LoginViewModel viewModel = new LoginViewModel(fakeRepo);

        viewModel.login("u@a.com", "123456");

        AuthUiState state = viewModel.getAuthUiState().getValue();
        Assert.assertNotNull(state);
        Assert.assertEquals(AuthUiState.Status.ERROR, state.getStatus());
        Assert.assertEquals("AUTH_EMAIL_NOT_VERIFIED", state.getMessage());
    }

    private static class FakeAuthRepository implements AuthRepository {
        enum Mode { SUCCESS, FAIL }

        private final Mode mode;
        private final String error;

        FakeAuthRepository(Mode mode, String error) {
            this.mode = mode;
            this.error = error;
        }

        @Override
        public void signInWithEmail(String email, String password, AuthResultCallback callback) {
            if (mode == Mode.SUCCESS) callback.onSuccess();
            else callback.onError(error == null ? "AUTH_UNKNOWN_ERROR" : error);
        }

        @Override
        public void registerWithEmail(String name, String email, String password, AuthResultCallback callback) {
            if (mode == Mode.SUCCESS) callback.onSuccess();
            else callback.onError(error == null ? "AUTH_UNKNOWN_ERROR" : error);
        }

        @Override
        public void sendPasswordResetEmail(String email, AuthResultCallback callback) {
            if (mode == Mode.SUCCESS) callback.onSuccess();
            else callback.onError(error == null ? "AUTH_UNKNOWN_ERROR" : error);
        }

        @Override
        public void signInWithGoogleIdToken(String idToken, AuthResultCallback callback) {
            if (mode == Mode.SUCCESS) callback.onSuccess();
            else callback.onError(error == null ? "AUTH_UNKNOWN_ERROR" : error);
        }
    }
}

