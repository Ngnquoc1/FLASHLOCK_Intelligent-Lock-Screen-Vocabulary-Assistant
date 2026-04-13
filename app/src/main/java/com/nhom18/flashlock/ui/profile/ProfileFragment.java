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
import com.nhom18.flashlock.data.model.UserProfile; // Nhớ import model UserProfile
import com.nhom18.flashlock.data.remote.FirebaseProfileDataSource;
import com.nhom18.flashlock.data.repository.FirebaseProfileRepository;
import com.nhom18.flashlock.databinding.FragmentProfileBinding;
import com.nhom18.flashlock.ui.login.LoginActivity;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;

    // Khai báo bộ phóng (Launcher) để mở trình chọn tệp tin từ hệ thống
    private final androidx.activity.result.ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    // Kiểm tra dung lượng ảnh trước khi upload
                    if (isFileSizeValid(uri)) {
                        viewModel.onAvatarPicked(uri);
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
        }
    }

    // Hàm check dung lượng file (Giới hạn 5MB)
    private boolean isFileSizeValid(android.net.Uri uri) {
        try {
            android.database.Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                long size = cursor.getLong(sizeIndex);
                cursor.close();
                return size <= 5 * 1024 * 1024; // 5MB
            }
        } catch (Exception e) { e.printStackTrace(); }
        return true;
    }

    private void setupObservers() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {

            // Xử lý bật/tắt nút Lưu dựa trên trạng thái (Khóa nút khi đang lưu)
            boolean isSaving = (state.getStatus() == ProfileUiState.Status.SAVING);
            binding.btnSave.setEnabled(!isSaving);
            binding.btnSave.setText(isSaving ? R.string.profile_msg_saving : R.string.profile_btn_save);

            // Khóa luôn nút chọn ảnh để chống user bấm liên tục
            binding.btnEditAvatar.setEnabled(!isSaving);

            switch (state.getStatus()) {
                case CONTENT:
                case SUCCESS:
                    if (state.getUserProfile() != null) {
                        // Chỉ set lại text nếu User không đang gõ (tránh việc con trỏ bị giật về đầu)
                        if (!binding.etDisplayName.hasFocus()) {
                            binding.etDisplayName.setText(state.getUserProfile().getDisplayName());
                        }

                        // Hiển thị Email
                        binding.tvEmail.setText(state.getUserProfile().getEmail());

                        String avatarUrl = state.getUserProfile().getAvatarUrl();
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            com.bumptech.glide.Glide.with(this)
                                    .load(avatarUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_nav_profile)
                                    .into(binding.ivAvatar);
                        } else {
                            // Nếu không có URL, hiển thị ảnh mặc định
                            binding.ivAvatar.setImageResource(R.drawable.ic_nav_profile);
                        }

                        // TODO: Nếu model Settings của bạn đã hoàn thiện, hãy gỡ comment bên dưới để set dữ liệu
                        // binding.swReminder.setChecked(state.getUserProfile().getSettings().isReminderEnabled());
                        // binding.tvHour.setText(String.format("%02d", state.getUserProfile().getSettings().getReminderHour()));
                        // binding.tvMinute.setText(String.format("%02d", state.getUserProfile().getSettings().getReminderMinute()));
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

        // Bắt sự kiện khi nhấn nút OK/Done trên bàn phím ảo
        binding.etDisplayName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                hideKeyboard();
                return true;
            }
            return false;
        });

        // Xử lý nút Lưu Profile
        binding.btnSave.setOnClickListener(v -> {
            hideKeyboard();

            String newName = binding.etDisplayName.getText().toString().trim();

            if (TextUtils.isEmpty(newName)) {
                binding.etDisplayName.setError(getString(R.string.profile_error_name_empty));
                binding.etDisplayName.requestFocus();
                return;
            }

            // Gói các Settings hiện tại trên UI vào Object
            UserProfile.Settings currentSettings = new UserProfile.Settings();

            // TODO: Bổ sung logic lấy data từ UI truyền vào Settings ở đây
            // currentSettings.setReminderEnabled(binding.swReminder.isChecked());
            // currentSettings.setReminderHour(Integer.parseInt(binding.tvHour.getText().toString()));
            // currentSettings.setReminderMinute(Integer.parseInt(binding.tvMinute.getText().toString()));

            // Gọi ViewModel để lưu (Lên Firebase)
            viewModel.onSaveProfile(newName, currentSettings);
        });

        // Xử lý nút chọn ảnh đại diện
        binding.btnEditAvatar.setOnClickListener(v -> {
            // Kích hoạt Launcher để mở thư viện ảnh, lọc chỉ hiện các file hình ảnh
            pickImageLauncher.launch("image/*");
        });

        // Xử lý nút Đăng xuất
        binding.btnLogout.setOnClickListener(v -> {
            // Chạy animation click
            Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.button_click);
            v.startAnimation(anim);

            // Đợi animation chạy xong rồi mới thực hiện logout
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

        binding.btnSyncNow.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Đang đồng bộ dữ liệu...", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}