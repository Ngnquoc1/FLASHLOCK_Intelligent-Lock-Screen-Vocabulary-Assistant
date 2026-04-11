package com.nhom18.flashlock.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nhom18.flashlock.R;
import com.nhom18.flashlock.data.remote.FirebaseProfileDataSource;
import com.nhom18.flashlock.data.repository.FirebaseProfileRepository;
import com.nhom18.flashlock.databinding.FragmentProfileBinding;
import com.nhom18.flashlock.ui.login.LoginActivity;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;

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

    private void setupObservers() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state.getStatus() == ProfileUiState.Status.CONTENT && state.getUserProfile() != null) {
                binding.tvDisplayName.setText(state.getUserProfile().getDisplayName());
            } else if (state.getStatus() == ProfileUiState.Status.ERROR) {
                Toast.makeText(getContext(), state.getErrorMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getNavigateToLogin().observe(getViewLifecycleOwner(), navigate -> {
            if (navigate) {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
            }
        });
    }

    private void setupListeners() {
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
