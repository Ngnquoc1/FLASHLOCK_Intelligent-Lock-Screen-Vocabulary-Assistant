package com.nhom18.flashlock.ui.register;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.nhom18.flashlock.data.repository.AuthRepository;
import com.nhom18.flashlock.data.repository.AuthResultCallback;
import com.nhom18.flashlock.ui.auth.AuthUiState;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
public class RegisterViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    @Test
    public void register_missingPhone_returnsPhoneRequiredError() {
        FakeAuthRepository fakeRepo = new FakeAuthRepository(FakeAuthRepository.Mode.SUCCESS, null);
        RegisterViewModel viewModel = new RegisterViewModel(fakeRepo);
        viewModel.register("User", "u@a.com", "", "123456");
        AuthUiState state = viewModel.getAuthUiState().getValue();
        Assert.assertNotNull(state);
        Assert.assertEquals(AuthUiState.Status.ERROR, state.getStatus());
        Assert.assertEquals("REGISTER_PHONE_REQUIRED", state.getMessage());
    }
    @Test
    public void register_success_returnsSuccessState() {
        FakeAuthRepository fakeRepo = new FakeAuthRepository(FakeAuthRepository.Mode.SUCCESS, null);
        RegisterViewModel viewModel = new RegisterViewModel(fakeRepo);
        viewModel.register("User", "u@a.com", "0909000000", "123456");
        AuthUiState state = viewModel.getAuthUiState().getValue();
        Assert.assertNotNull(state);
        Assert.assertEquals(AuthUiState.Status.SUCCESS, state.getStatus());
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
        public void registerWithEmail(String email, String password, AuthResultCallback callback) {
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
