package com.nhom18.flashlock.ui.register;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.nhom18.flashlock.R;
import com.nhom18.flashlock.databinding.ActivityRegisterBinding;
import com.nhom18.flashlock.ui.auth.AuthUiState;
import com.nhom18.flashlock.ui.login.LoginActivity;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private RegisterViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        setupObservers();
        setupListeners();
    }

    private void setupObservers() {
        viewModel.getAuthUiState().observe(this, state -> {
            if (state == null) {
                return;
            }

            boolean isLoading = state.getStatus() == AuthUiState.Status.LOADING;
            binding.btnRegister.setEnabled(!isLoading);

            if (state.getStatus() == AuthUiState.Status.SUCCESS) {
                Toast.makeText(this, getString(R.string.auth_register_success), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            } else if (state.getStatus() == AuthUiState.Status.ERROR) {
                Toast.makeText(this, resolveAuthErrorMessage(state.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        binding.btnRegister.setOnClickListener(v -> {
            String name = binding.etFullName.getText() != null ? binding.etFullName.getText().toString() : "";
            String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString() : "";
            String phone = binding.etPhone.getText() != null ? binding.etPhone.getText().toString() : "";
            String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString() : "";

            viewModel.register(name, email, phone, password);
        });

        binding.tvLogin.setOnClickListener(v -> navigateToLogin());

        binding.llNavLogin.setOnClickListener(v -> navigateToLogin());

        binding.tvHelp.setOnClickListener(v -> {
            // Show help.
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private String resolveAuthErrorMessage(String errorCodeOrMessage) {
        if ("REGISTER_NAME_REQUIRED".equals(errorCodeOrMessage)) return getString(R.string.auth_error_register_name_required);
        if ("REGISTER_EMAIL_REQUIRED".equals(errorCodeOrMessage)) return getString(R.string.auth_error_register_email_required);
        if ("REGISTER_PHONE_REQUIRED".equals(errorCodeOrMessage)) return getString(R.string.auth_error_register_phone_required);
        if ("REGISTER_PASSWORD_REQUIRED".equals(errorCodeOrMessage)) return getString(R.string.auth_error_register_password_required);

        if ("AUTH_INVALID_EMAIL".equals(errorCodeOrMessage)) return getString(R.string.auth_error_invalid_email);
        if ("AUTH_EMAIL_ALREADY_IN_USE".equals(errorCodeOrMessage)) return getString(R.string.auth_error_email_already_in_use);
        if ("AUTH_WEAK_PASSWORD".equals(errorCodeOrMessage)) return getString(R.string.auth_error_weak_password);
        if ("AUTH_NETWORK_ERROR".equals(errorCodeOrMessage)) return getString(R.string.auth_error_network);
        if ("AUTH_TOO_MANY_REQUESTS".equals(errorCodeOrMessage)) return getString(R.string.auth_error_too_many_requests);

        return getString(R.string.auth_action_failed, errorCodeOrMessage);
    }
}