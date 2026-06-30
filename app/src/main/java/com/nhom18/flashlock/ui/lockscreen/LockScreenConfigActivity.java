package com.nhom18.flashlock.ui.lockscreen;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.nhom18.flashlock.service.LockScreenStudyService;

import com.google.android.material.slider.Slider;
import com.nhom18.flashlock.R;
import com.nhom18.flashlock.data.model.Topic;
import com.nhom18.flashlock.data.model.UserProfile;
import com.nhom18.flashlock.data.remote.FirebaseProfileDataSource;
import com.nhom18.flashlock.data.remote.FirebaseSavedTopicDataSource;
import com.nhom18.flashlock.data.repository.FirebaseProfileRepository;
import com.nhom18.flashlock.data.repository.FirebaseSavedTopicRepository;
import com.nhom18.flashlock.databinding.ActivityLockScreenConfigBinding;
import com.nhom18.flashlock.ui.profile.LockScreenTopicAdapter;
import com.nhom18.flashlock.utils.PermissionsHelper;

import java.util.ArrayList;
import java.util.List;

public class LockScreenConfigActivity extends AppCompatActivity {

    private ActivityLockScreenConfigBinding binding;
    private LockScreenConfigViewModel viewModel;
    private LockScreenTopicAdapter adapter;
    private UserProfile currentProfile;

    private List<String> selectedTopicIds = new ArrayList<>();
    // Phân biệt set programmatic (load từ Firestore) vs user tap, tránh vòng lặp save.
    private boolean isProgrammaticToggle = false;

    // Bất kể user "Allow" hay "Deny" trong dialog hệ thống, callback chắc chắn chạy
    // → refresh status đáng tin hơn onResume (vốn có thể không fire trên một số dialog).
    private final ActivityResultLauncher<Intent> settingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> refreshPermissionStatus());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLockScreenConfigBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this, new LockScreenConfigViewModelFactory(
                new FirebaseProfileRepository(new FirebaseProfileDataSource()),
                new FirebaseSavedTopicRepository(new FirebaseSavedTopicDataSource())
        )).get(LockScreenConfigViewModel.class);

        setupRecycler();
        setupListeners();
        setupObservers();
        setupPermissionFixButtons();

        // Tạo channel ngay để user có thể chỉnh setting channel cụ thể trong system Settings
        // mà không phải chờ service chạy lần đầu.
        LockScreenStudyService.ensureChannel(this);

        viewModel.load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh trạng thái mỗi khi quay lại từ Settings.
        refreshPermissionStatus();
    }

    private void setupPermissionFixButtons() {
        binding.btnFixNotifications.setOnClickListener(v -> {
            if (!PermissionsHelper.hasNotificationPermission(this)) {
                PermissionsHelper.requestNotificationPermission(this);
            } else {
                settingsLauncher.launch(
                        PermissionsHelper.appNotificationSettings(this, null));
            }
        });
        binding.btnFixChannel.setOnClickListener(v ->
                settingsLauncher.launch(
                        PermissionsHelper.appNotificationSettings(
                                this, LockScreenStudyService.CHANNEL_ID)));
        binding.btnFixBattery.setOnClickListener(v -> {
            Intent i = PermissionsHelper.requestIgnoreBatteryOptimizationsIntent(this);
            if (i != null) settingsLauncher.launch(i);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionsHelper.REQ_POST_NOTIFICATIONS) {
            refreshPermissionStatus();
        }
    }

    private void refreshPermissionStatus() {
        // POST_NOTIFICATIONS + global enabled (gộp 1 ô)
        boolean notifOk = PermissionsHelper.hasNotificationPermission(this)
                && PermissionsHelper.areAppNotificationsEnabled(this);
        bindPermissionRow(binding.tvStatusNotifications, binding.btnFixNotifications, notifOk);

        // Channel lock-screen enabled (chưa tạo channel → coi như chưa OK).
        boolean channelOk = PermissionsHelper.isChannelEnabled(this, LockScreenStudyService.CHANNEL_ID);
        bindPermissionRow(binding.tvStatusChannel, binding.btnFixChannel, channelOk);

        // Battery unrestricted (cảnh báo, không bắt buộc).
        boolean batteryOk = PermissionsHelper.isIgnoringBatteryOptimizations(this);
        bindPermissionRow(binding.tvStatusBattery, binding.btnFixBattery, batteryOk);
    }

    private void bindPermissionRow(android.widget.TextView status,
                                   com.google.android.material.button.MaterialButton fixBtn,
                                   boolean ok) {
        status.setText(ok ? R.string.perm_status_ok : R.string.perm_status_missing);
        fixBtn.setVisibility(ok ? View.GONE : View.VISIBLE);
    }

    private void setupRecycler() {
        adapter = new LockScreenTopicAdapter(new ArrayList<>(), new ArrayList<>());
        adapter.setOnSelectionChangedListener(count -> updateSelectedCount());
        binding.recyclerTopics.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerTopics.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnApply.setOnClickListener(v -> saveConfig());
        binding.btnReset.setOnClickListener(v -> resetDefaults());

        // Switch auto-save ngay khi toggle (Material pattern) — không cần Apply.
        binding.swEnableLockScreen.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticToggle) return;
            applyEnableToggle(isChecked);
        });
    }

    private void applyEnableToggle(boolean isChecked) {
        if (currentProfile == null) return;
        UserProfile.Settings currentSettings = currentProfile.getSettings() != null
                ? currentProfile.getSettings() : new UserProfile.Settings();
        currentSettings.setLockScreenEnabled(isChecked);
        // Không động vào topicIds — Apply lo phần đó.
        String displayName = currentProfile.getDisplayName() != null ? currentProfile.getDisplayName() : "";
        viewModel.saveSettings(displayName, currentSettings);

        Intent serviceIntent = new Intent(this, LockScreenStudyService.class);
        if (isChecked) {
            androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent);
        } else {
            stopService(serviceIntent);
        }
    }

    private void setupObservers() {
        viewModel.getProfile().observe(this, profile -> {
            currentProfile = profile;
            if (profile == null || profile.getSettings() == null) {
                return;
            }
            UserProfile.Settings settings = profile.getSettings();
            // Set checked programmatic, không trigger listener.
            isProgrammaticToggle = true;
            binding.swEnableLockScreen.setChecked(settings.isLockScreenEnabled());
            isProgrammaticToggle = false;
            selectedTopicIds = settings.getLockScreenTopicIds() != null
                    ? settings.getLockScreenTopicIds() : new ArrayList<>();
            updateSelectedCount();
        });

        viewModel.getTopics().observe(this, topics -> {
            List<Topic> items = topics != null ? topics : new ArrayList<>();
            adapter = new LockScreenTopicAdapter(items, selectedTopicIds);
            adapter.setOnSelectionChangedListener(count -> updateSelectedCount());
            binding.recyclerTopics.setAdapter(adapter);
            updateSelectedCount();
        });

        viewModel.getSaving().observe(this, saving -> {
            boolean isSaving = Boolean.TRUE.equals(saving);
            binding.btnApply.setEnabled(!isSaving);
        });

        viewModel.getError().observe(this, message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSelectedCount() {
        int count = adapter != null ? adapter.getSelectedIds().size() : 0;
        if (count == 0) {
            binding.tvSelectedCount.setText(getString(R.string.lock_screen_topics_all));
            return;
        }
        binding.tvSelectedCount.setText(getString(R.string.lock_screen_topics_selected_format, count));
    }

    private void saveConfig() {
        if (currentProfile == null) {
            return;
        }
        // Switch enabled đã được auto-save ở listener. Apply chỉ lưu danh sách topic.
        UserProfile.Settings currentSettings = currentProfile.getSettings() != null
                ? currentProfile.getSettings() : new UserProfile.Settings();
        currentSettings.setLockScreenEnabled(binding.swEnableLockScreen.isChecked());
        currentSettings.setLockScreenTopicIds(new ArrayList<>(adapter.getSelectedIds()));

        String displayName = currentProfile.getDisplayName() != null ? currentProfile.getDisplayName() : "";
        viewModel.saveSettings(displayName, currentSettings);
        Toast.makeText(this, R.string.lock_screen_config_saved, Toast.LENGTH_SHORT).show();

        // Topic đổi → service cần refresh pool. Chỉ start nếu đang enabled.
        if (binding.swEnableLockScreen.isChecked()) {
            Intent refresh = new Intent(this, LockScreenStudyService.class);
            refresh.setAction(LockScreenStudyService.ACTION_REFRESH_WORDS);
            androidx.core.content.ContextCompat.startForegroundService(this, refresh);
        }
    }

    private void resetDefaults() {
        binding.swEnableLockScreen.setChecked(true);
        adapter = new LockScreenTopicAdapter(
                viewModel.getTopics().getValue() != null ? viewModel.getTopics().getValue() : new ArrayList<>(),
                new ArrayList<>()
        );
        adapter.setOnSelectionChangedListener(count -> updateSelectedCount());
        binding.recyclerTopics.setAdapter(adapter);
        updateSelectedCount();
    }

    private static class LockScreenConfigViewModelFactory implements ViewModelProvider.Factory {
        private final FirebaseProfileRepository profileRepository;
        private final FirebaseSavedTopicRepository savedTopicRepository;

        LockScreenConfigViewModelFactory(FirebaseProfileRepository profileRepository,
                                         FirebaseSavedTopicRepository savedTopicRepository) {
            this.profileRepository = profileRepository;
            this.savedTopicRepository = savedTopicRepository;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends androidx.lifecycle.ViewModel> T create(Class<T> modelClass) {
            return (T) new LockScreenConfigViewModel(profileRepository, savedTopicRepository);
        }
    }
}
