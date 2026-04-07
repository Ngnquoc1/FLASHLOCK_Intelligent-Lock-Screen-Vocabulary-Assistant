package com.nhom18.flashlock.ui.reset;

import android.os.Bundle;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.nhom18.flashlock.databinding.ActivityResetAccessBinding;
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
        viewModel.getResetStatus().observe(this, isSuccess -> {
            if (isSuccess != null && isSuccess) {
                // Navigate to confirmation screen
                String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString() : "";
                Intent intent = new Intent(this, ResetConfirmActivity.class);
                intent.putExtra("EMAIL_EXTRA", email);
                startActivity(intent);
                finish();
            } else {
                // Show error message
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
            // Show help dialog or screen
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
