package com.nhom18.flashlock.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.nhom18.flashlock.R;
import com.nhom18.flashlock.databinding.ActivityLoginBinding;
import com.nhom18.flashlock.ui.auth.AuthUiState;
import com.nhom18.flashlock.ui.main.MainActivity;
import com.nhom18.flashlock.ui.register.RegisterActivity;
import com.nhom18.flashlock.ui.reset.ResetAccessActivity;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private LoginViewModel viewModel;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        setupGoogleSignIn();
        setupObservers();
        setupListeners();
    }

    private void setupGoogleSignIn() {
        String webClientId = getString(R.string.google_web_client_id);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        Toast.makeText(this, getString(R.string.auth_error_google_cancelled), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account == null) {
                            Toast.makeText(this, getString(R.string.auth_error_google_token_invalid), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        viewModel.loginWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        Toast.makeText(this, getString(R.string.auth_error_google_cancelled), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupObservers() {
        viewModel.getAuthUiState().observe(this, state -> {
            if (state == null) {
                return;
            }

            boolean isLoading = state.getStatus() == AuthUiState.Status.LOADING;
            binding.btnLogin.setEnabled(!isLoading);
            binding.btnGoogle.setEnabled(!isLoading);

            if (state.getStatus() == AuthUiState.Status.SUCCESS) {
                Toast.makeText(this, getString(R.string.auth_login_success), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else if (state.getStatus() == AuthUiState.Status.ERROR) {
                Toast.makeText(this, resolveAuthErrorMessage(state.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString() : "";
            String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString() : "";
            viewModel.login(email, password);
        });

        binding.tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ResetAccessActivity.class))
        );

        binding.btnGoogle.setOnClickListener(v -> startGoogleSignIn());

        binding.llNavRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );

        binding.tvRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );
    }

    private void startGoogleSignIn() {
        String webClientId = getString(R.string.google_web_client_id);
        if (webClientId.contains("REPLACE_WITH_FIREBASE")) {
            Toast.makeText(this, getString(R.string.auth_error_google_config_missing), Toast.LENGTH_SHORT).show();
            return;
        }

        googleSignInClient.signOut().addOnCompleteListener(task ->
                googleSignInLauncher.launch(googleSignInClient.getSignInIntent())
        );
    }

    private String resolveAuthErrorMessage(String errorCodeOrMessage) {
        if ("LOGIN_EMAIL_REQUIRED".equals(errorCodeOrMessage)) return getString(R.string.auth_error_login_email_required);
        if ("LOGIN_PASSWORD_REQUIRED".equals(errorCodeOrMessage)) return getString(R.string.auth_error_login_password_required);

        if ("AUTH_INVALID_EMAIL".equals(errorCodeOrMessage)) return getString(R.string.auth_error_invalid_email);
        if ("AUTH_USER_NOT_FOUND".equals(errorCodeOrMessage)) return getString(R.string.auth_error_user_not_found);
        if ("AUTH_WRONG_PASSWORD".equals(errorCodeOrMessage)) return getString(R.string.auth_error_wrong_password);
        if ("AUTH_USER_DISABLED".equals(errorCodeOrMessage)) return getString(R.string.auth_error_user_disabled);
        if ("AUTH_EMAIL_NOT_VERIFIED".equals(errorCodeOrMessage)) return getString(R.string.auth_error_email_not_verified);
        if ("AUTH_NETWORK_ERROR".equals(errorCodeOrMessage)) return getString(R.string.auth_error_network);
        if ("AUTH_TOO_MANY_REQUESTS".equals(errorCodeOrMessage)) return getString(R.string.auth_error_too_many_requests);
        if ("AUTH_GOOGLE_TOKEN_INVALID".equals(errorCodeOrMessage)) return getString(R.string.auth_error_google_token_invalid);

        return getString(R.string.auth_action_failed, errorCodeOrMessage);
    }
}