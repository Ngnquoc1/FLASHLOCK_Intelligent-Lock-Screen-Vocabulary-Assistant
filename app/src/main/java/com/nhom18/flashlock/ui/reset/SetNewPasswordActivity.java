package com.nhom18.flashlock.ui.reset;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.nhom18.flashlock.databinding.ActivitySetNewPasswordBinding;
import com.nhom18.flashlock.ui.login.LoginActivity;

public class SetNewPasswordActivity extends AppCompatActivity {

    private ActivitySetNewPasswordBinding binding;
    private SetNewPasswordViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetNewPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(SetNewPasswordViewModel.class);

        setupObservers();
        setupListeners();
    }

    private void setupObservers() {
        viewModel.getUpdateStatus().observe(this, isSuccess -> {
            if (isSuccess != null && isSuccess) {
                // Return completely back to login upon success
                navigateToLogin();
            } else {
                // Show mismatch or error
            }
        });

        viewModel.getPasswordStrength().observe(this, strengthValue -> {
            binding.pbStrength.setProgress(strengthValue);
            binding.tvStrengthPercent.setText(strengthValue + "%");

            if(strengthValue < 30) {
                binding.tvStrengthLabel.setText("Weak");
            } else if (strengthValue < 80) {
                binding.tvStrengthLabel.setText("Medium");
            } else {
                binding.tvStrengthLabel.setText("Strong");
            }
        });
    }

    private void setupListeners() {
        binding.ivBack.setOnClickListener(v -> finish());

        binding.btnUpdatePassword.setOnClickListener(v -> {
            String newPwd = binding.etNewPassword.getText() != null ? binding.etNewPassword.getText().toString() : "";
            String confirmPwd = binding.etConfirmPassword.getText() != null ? binding.etConfirmPassword.getText().toString() : "";
            viewModel.updatePassword(newPwd, confirmPwd);
        });

        binding.tvContactSupport.setOnClickListener(v -> {
            // Logic to open support link/dialog
        });

        binding.etNewPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.evaluatePassword(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}

