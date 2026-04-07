package com.nhom18.flashlock.ui.reset;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.nhom18.flashlock.databinding.ActivityResetConfirmBinding;
import com.nhom18.flashlock.ui.login.LoginActivity;

public class ResetConfirmActivity extends AppCompatActivity {

    private ActivityResetConfirmBinding binding;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResetConfirmBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get email from intent if passed
        userEmail = getIntent().getStringExtra("EMAIL_EXTRA");

        setupListeners();
    }

    private void setupListeners() {
        binding.ivBack.setOnClickListener(v -> navigateToLogin());

        binding.btnBackToLogin.setOnClickListener(v -> navigateToLogin());

        binding.llNavLogin.setOnClickListener(v -> navigateToLogin());

        binding.tvResendEmail.setOnClickListener(v -> {
            // Simulator: Trigger the deep link flow instead for demo
            Intent intent = new Intent(this, SetNewPasswordActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
