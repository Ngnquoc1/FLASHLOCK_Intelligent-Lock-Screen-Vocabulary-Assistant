package com.nhom18.flashlock.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.nhom18.flashlock.databinding.FragmentHomeDashboardBinding;
import com.nhom18.flashlock.data.model.Topic;
import com.nhom18.flashlock.ui.study.StudyFlashcardActivity;
import com.nhom18.flashlock.ui.lockscreen.LockScreenConfigActivity;

public class HomeDashboardFragment extends Fragment {

    private FragmentHomeDashboardBinding binding;
    private HomeDashboardViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HomeDashboardViewModel.class);

        setupObservers();
        setupClickListeners();

        viewModel.loadDashboardData();
    }

    private void setupObservers() {
        viewModel.getDailyCount().observe(getViewLifecycleOwner(), count -> {
            binding.tvProgressCount.setText(String.valueOf(count));
            updateProgressSubtitle();
        });

        viewModel.getDailyGoal().observe(getViewLifecycleOwner(), goal -> {
            binding.tvProgressGoal.setText("GOAL " + goal);
            updateProgressSubtitle();
        });

        viewModel.getProgressPercentage().observe(getViewLifecycleOwner(), percentage ->
                binding.progressCircular.setProgress(percentage, true)
        );

        viewModel.getLatestTopicProgress().observe(getViewLifecycleOwner(), progress -> {
            if (progress != null) {
                binding.tvFlashcardSubtitle.setText("Continue learning: " + progress.getTopicId());
            } else {
                binding.tvFlashcardSubtitle.setText("Continue learning: My Words");
            }
        });

        viewModel.getWordOfTheDay().observe(getViewLifecycleOwner(), word -> {
            if (word != null) {
                binding.tvWordOfDayTitle.setText(word.getTerm());
                binding.tvWordOfDayPronunciation.setText(word.getPronunciation());
                binding.tvWordOfDayMeaning.setText(word.getDefinition());
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProgressSubtitle() {
        Integer count = viewModel.getDailyCount().getValue();
        Integer goal = viewModel.getDailyGoal().getValue();
        if (count != null && goal != null) {
            int remaining = Math.max(0, goal - count);
            if (remaining > 0) {
                binding.tvProgressSubtitle.setText("You are only " + remaining + " words away from completion!");
            } else {
                binding.tvProgressSubtitle.setText("Goal completed! Great job!");
            }
        }
    }

    private void setupClickListeners() {
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                String query = binding.etSearch.getText().toString().trim();
                if (!query.isEmpty()) {
                    Toast.makeText(getContext(), "Searching for: " + query, Toast.LENGTH_SHORT).show();
                    binding.etSearch.clearFocus();
                }
                return true;
            }
            return false;
        });

        binding.cardFlashcardLearn.setOnClickListener(v -> {
            String topicId = Topic.MY_WORDS_TOPIC_ID;
            if (viewModel.getLatestTopicProgress().getValue() != null) {
                topicId = viewModel.getLatestTopicProgress().getValue().getTopicId();
            }

            Intent intent = new Intent(getContext(), StudyFlashcardActivity.class);
            intent.putExtra(StudyFlashcardActivity.EXTRA_TOPIC_ID, topicId);
            startActivity(intent);
        });

        binding.cardLockScreenSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), LockScreenConfigActivity.class);
            startActivity(intent);
        });

        binding.btnWordVolume.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Speaking...", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}