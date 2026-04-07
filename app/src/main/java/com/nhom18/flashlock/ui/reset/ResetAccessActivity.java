package com.nhom18.flashlock.ui.reset;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.nhom18.flashlock.R;
import com.nhom18.flashlock.databinding.ActivityResetAccessBinding;
import com.nhom18.flashlock.ui.auth.AuthUiState;
import com.nhom18.flashlock.ui.login.LoginActivity;

public class ResetAccessActivity extends AppCompatActivity {

    private ActivityResetAccessBinding binding;
    private ResetAccessViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResetAccessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ResetAccessViewModel.class);

        setupObservers();
        setupListeners();
    }

    private void setupObservers() {
        viewModel.getAuthUiState().observe(this, state -> {
            if (state == null) {
                return;
            }

            boolean isLoading = state.getStatus() == AuthUiState.Status.LOADING;
            binding.btnSendReset.setEnabled(!isLoading);

            if (state.getStatus() == AuthUiState.Status.SUCCESS) {
                Toast.makeText(this, getString(R.string.auth_reset_link_sent), Toast.LENGTH_SHORT).show();
                String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString() : "";
                Intent intent = new Intent(this, ResetConfirmActivity.class);
                intent.putExtra("EMAIL_EXTRA", email);
                startActivity(intent);
                finish();
            } else if (state.getStatus() == AuthUiState.Status.ERROR) {
                Toast.makeText(this, resolveAuthErrorMessage(state.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        binding.btnSendReset.setOnClickListener(v -> {
            String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString() : "";
            viewModel.sendResetLink(email);
        });

        binding.ivBack.setOnClickListener(v -> navigateToLogin());
        binding.tvBackToLogin.setOnClickListener(v -> navigateToLogin());
        binding.ivLoginIcon.setOnClickListener(v -> navigateToLogin());

        binding.ivHelp.setOnClickListener(v -> {
            // Show help dialog or screen.
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private String resolveAuthErrorMessage(String errorCodeOrMessage) {
        if ("RESET_EMAIL_REQUIRED".equals(errorCodeOrMessage)) return getString(R.string.auth_error_login_email_required);
        if ("RESET_EMAIL_INVALID".equals(errorCodeOrMessage)) return getString(R.string.auth_error_invalid_email);

        if ("AUTH_USER_NOT_FOUND".equals(errorCodeOrMessage)) return getString(R.string.auth_error_user_not_found);
        if ("AUTH_NETWORK_ERROR".equals(errorCodeOrMessage)) return getString(R.string.auth_error_network);
        if ("AUTH_TOO_MANY_REQUESTS".equals(errorCodeOrMessage)) return getString(R.string.auth_error_too_many_requests);

        return getString(R.string.auth_action_failed, errorCodeOrMessage);
    }
}