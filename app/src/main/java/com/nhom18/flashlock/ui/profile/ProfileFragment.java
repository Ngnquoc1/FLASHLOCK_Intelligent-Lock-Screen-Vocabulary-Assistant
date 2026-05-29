package com.nhom18.flashlock.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nhom18.flashlock.R;
import com.nhom18.flashlock.data.model.UserProfile;
import com.nhom18.flashlock.data.remote.FirebaseProfileDataSource;
import com.nhom18.flashlock.data.repository.FirebaseProfileRepository;
import com.nhom18.flashlock.databinding.FragmentProfileBinding;
import com.nhom18.flashlock.ui.login.LoginActivity;
import com.nhom18.flashlock.ui.lockscreen.LockScreenConfigActivity;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;

    private int currentHour = 20;
    private int currentMinute = 30;
    private UserProfile currentProfile;

    private final androidx.activity.result.ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    if (isFileSizeValid(uri)) {

                        viewModel.setPendingAvatarUri(uri);

                        // Chỉ cập nhật preview ảnh chính trong fragment
                        com.bumptech.glide.Glide.with(this)
                                .load(uri)
                                .transform(
                                        new com.bumptech.glide.load.resource.bitmap.CenterCrop(),
                                        new com.bumptech.glide.load.resource.bitmap.RoundedCorners(32)
                                )
                                .into(binding.ivAvatar);

                        // Top Bar Avatar sẽ được MainActivity tự động cập nhật sau khi upload thành công
                    } else {
                        Toast.makeText(getContext(), "Vui lòng chọn ảnh dưới 5MB.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseProfileDataSource dataSource = new FirebaseProfileDataSource();
        FirebaseProfileRepository repository = new FirebaseProfileRepository(dataSource);
        viewModel = new ProfileViewModel(repository);

        setupObservers();
        setupListeners();

        viewModel.loadProfile();
    }

    private void hideKeyboard() {
        if (getActivity() != null) {
            View view = getActivity().getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        }
        if (binding != null) {
            binding.etDisplayName.clearFocus();
            binding.etDailyGoalValue.clearFocus();
        }
    }

    private boolean isFileSizeValid(android.net.Uri uri) {
        try {
            android.database.Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                if (sizeIndex != -1) {
                    long size = cursor.getLong(sizeIndex);
                    cursor.close();
                    return size <= 5 * 1024 * 1024;
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private void setupObservers() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            boolean isSaving = (state.getStatus() == ProfileUiState.Status.SAVING);
            binding.btnSave.setEnabled(!isSaving);
            binding.btnSave.setText(isSaving ? R.string.profile_msg_saving : R.string.profile_btn_save);
            binding.btnEditAvatar.setEnabled(!isSaving);

            switch (state.getStatus()) {
                case CONTENT:
                case SUCCESS:
                    if (state.getUserProfile() != null) {
                        currentProfile = state.getUserProfile();
                        if (!binding.etDisplayName.hasFocus()) {
                            binding.etDisplayName.setText(state.getUserProfile().getDisplayName());
                        }
                        binding.tvEmail.setText(state.getUserProfile().getEmail());

                        String avatarUrl = state.getUserProfile().getAvatarUrl();
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            com.bumptech.glide.Glide.with(this)
                                    .load(avatarUrl)
                                    .transform(
                                            new com.bumptech.glide.load.resource.bitmap.CenterCrop(),
                                            new com.bumptech.glide.load.resource.bitmap.RoundedCorners(32)
                                    )
                                    .placeholder(R.drawable.ic_nav_profile)
                                    .into(binding.ivAvatar);
                        } else {
                            binding.ivAvatar.setImageResource(R.drawable.ic_nav_profile);
                        }

                        if (state.getUserProfile().getSettings() != null) {
                            currentHour = state.getUserProfile().getSettings().getReminderHour();
                            currentMinute = state.getUserProfile().getSettings().getReminderMinute();
                            binding.swReminder.setChecked(state.getUserProfile().getSettings().isLockScreenEnabled());
                            updateTimeUI();

                            // Load Daily Goal lên UI
                            if (!binding.etDailyGoalValue.hasFocus()) {
                                binding.etDailyGoalValue.setText(String.valueOf(state.getUserProfile().getSettings().getDailyGoal()));
                            }
                        }
                    }

                    if (state.getStatus() == ProfileUiState.Status.SUCCESS) {
                        Toast.makeText(getContext(), R.string.profile_msg_save_success, Toast.LENGTH_SHORT).show();
                    }
                    break;

                case ERROR:
                    Toast.makeText(getContext(), state.getErrorMessage(), Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        viewModel.getNavigateToLogin().observe(getViewLifecycleOwner(), navigate -> {
            if (navigate) {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });
    }

    private void setupListeners() {
        binding.etDisplayName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                hideKeyboard();
                return true;
            }
            return false;
        });

        binding.etDailyGoalValue.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                hideKeyboard();
                return true;
            }
            return false;
        });

        binding.btnGoalDown.setOnClickListener(v -> {
            try {
                int currentGoal = Integer.parseInt(binding.etDailyGoalValue.getText().toString().trim());
                if (currentGoal > 1) {
                    binding.etDailyGoalValue.setText(String.valueOf(currentGoal - 1));
                }
            } catch (NumberFormatException e) {
                binding.etDailyGoalValue.setText("1");
            }
        });

        binding.btnGoalUp.setOnClickListener(v -> {
            try {
                int currentGoal = Integer.parseInt(binding.etDailyGoalValue.getText().toString().trim());
                binding.etDailyGoalValue.setText(String.valueOf(currentGoal + 1));
            } catch (NumberFormatException e) {
                binding.etDailyGoalValue.setText("5");
            }
        });

        binding.btnSave.setOnClickListener(v -> {
            hideKeyboard();
            String newName = binding.etDisplayName.getText().toString().trim();
            if (TextUtils.isEmpty(newName)) {
                binding.etDisplayName.setError(getString(R.string.profile_error_name_empty));
                binding.etDisplayName.requestFocus();
                return;
            }

            int newGoal = 5;
            try {
                newGoal = Integer.parseInt(binding.etDailyGoalValue.getText().toString().trim());
                if (newGoal <= 0) newGoal = 1;
            } catch (NumberFormatException e) {
                newGoal = 5;
                binding.etDailyGoalValue.setText(String.valueOf(newGoal));
            }

            UserProfile.Settings currentSettings = currentProfile != null && currentProfile.getSettings() != null
                    ? currentProfile.getSettings() : new UserProfile.Settings();

            currentSettings.setReminderHour(currentHour);
            currentSettings.setReminderMinute(currentMinute);
            currentSettings.setLockScreenEnabled(binding.swReminder.isChecked());
            currentSettings.setDailyGoal(newGoal);

            viewModel.onSaveProfile(newName, currentSettings);
        });

        binding.btnEditAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        binding.btnLogout.setOnClickListener(v -> {
            Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.button_click);
            v.startAnimation(anim);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}
                @Override
                public void onAnimationEnd(Animation animation) {
                    viewModel.onLogout();
                }
                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        });

        binding.btnSyncNow.setOnClickListener(v -> Toast.makeText(getContext(), "Đang đồng bộ dữ liệu...", Toast.LENGTH_SHORT).show());
        binding.btnHourUp.setOnClickListener(v -> { currentHour = (currentHour + 1) % 24; updateTimeUI(); });
        binding.btnHourDown.setOnClickListener(v -> { currentHour = (currentHour - 1 + 24) % 24; updateTimeUI(); });
        binding.btnMinuteUp.setOnClickListener(v -> { currentMinute = (currentMinute + 1) % 60; updateTimeUI(); });
        binding.btnMinuteDown.setOnClickListener(v -> { currentMinute = (currentMinute - 1 + 60) % 60; updateTimeUI(); });

        binding.btnLockScreenSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LockScreenConfigActivity.class);
            startActivity(intent);
        });
    }

    private void updateTimeUI() {
        binding.tvHour.setText(String.format("%02d", currentHour));
        binding.tvMinute.setText(String.format("%02d", currentMinute));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}