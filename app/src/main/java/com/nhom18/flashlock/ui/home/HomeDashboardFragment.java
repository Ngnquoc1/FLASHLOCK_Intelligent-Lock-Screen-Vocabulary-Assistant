package com.nhom18.flashlock.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.nhom18.flashlock.R;
import com.nhom18.flashlock.databinding.FragmentHomeDashboardBinding;
import com.nhom18.flashlock.data.model.Topic;
import com.nhom18.flashlock.data.model.Word;
import com.nhom18.flashlock.ui.study.StudyFlashcardActivity;
import com.nhom18.flashlock.ui.lockscreen.LockScreenConfigActivity;

import java.util.Locale;

public class HomeDashboardFragment extends Fragment {

    private FragmentHomeDashboardBinding binding;
    private HomeDashboardViewModel viewModel;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private SearchSuggestionAdapter searchAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(HomeDashboardViewModel.class);

        setupTts();
        setupSearchAdapter();
        setupObservers();
        setupClickListeners();

        viewModel.loadDashboardData();
    }

    private void setupTts() {
        tts = new TextToSpeech(requireContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });
    }

    private void speakWordOfTheDay() {
        if (binding == null) return;
        if (!ttsReady) {
            Toast.makeText(getContext(), R.string.home_wod_tts_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        Word w = viewModel.getWordOfTheDay().getValue();
        if (w == null || w.getTerm() == null || w.getTerm().trim().isEmpty()) return;
        tts.speak(w.getTerm(), TextToSpeech.QUEUE_FLUSH, null, "home_wod");
    }

    private void setupSearchAdapter() {
        searchAdapter = new SearchSuggestionAdapter();
        binding.rvSearchResults.setAdapter(searchAdapter);

        searchAdapter.setOnItemClickListener(new SearchSuggestionAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(SearchSuggestionAdapter.SearchItem item) {
                binding.etSearch.setText("");
                binding.etSearch.clearFocus();
                binding.cardSearchResults.setVisibility(View.GONE);

                DictionaryDetailBottomSheet sheet = DictionaryDetailBottomSheet.newInstance(item.id);
                sheet.show(getChildFragmentManager(), "DictionaryDetail");
            }

            @Override
            public void onAddClick(SearchSuggestionAdapter.SearchItem item) {
                if (item.type == SearchSuggestionAdapter.SearchItem.TYPE_API) {
                    Word quickWord = new Word();
                    quickWord.setTerm(item.id);
                    quickWord.setDefinition(item.definition != null ? item.definition : "");
                    quickWord.setStatus(Word.STATUS_NEW);
                    quickWord.setNextReviewAt(com.google.firebase.Timestamp.now());

                    viewModel.addCustomWordToMyWords(quickWord);

                    binding.etSearch.setText("");
                    binding.etSearch.clearFocus();
                    binding.cardSearchResults.setVisibility(View.GONE);
                }
            }
        });
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

        viewModel.getStreakCount().observe(getViewLifecycleOwner(), streak -> {
            if (binding != null) {
                binding.tvStreakCount.setText(streak + " Streak");
            }
        });

        viewModel.getLatestTopicProgress().observe(getViewLifecycleOwner(), progress -> {
            String topicId = progress != null ? progress.getTopicId() : null;
            String name = (topicId == null || Topic.MY_WORDS_TOPIC_ID.equals(topicId))
                    ? getString(R.string.home_my_words)
                    : getString(R.string.home_latest_topic);
            binding.tvFlashcardSubtitle.setText(getString(R.string.home_continue_format, name));
        });

        viewModel.getWordOfTheDay().observe(getViewLifecycleOwner(), word -> {
            if (word == null) return;
            binding.tvWordOfDayTitle.setText(word.getTerm());
            binding.tvWordOfDayPronunciation.setText(word.getPronunciation());
            binding.tvWordOfDayMeaning.setText(word.getDefinition());

            String type = word.getWordType();
            if (!TextUtils.isEmpty(type)) {
                binding.tvWordOfDayType.setText(type);
                binding.tvWordOfDayType.setVisibility(View.VISIBLE);
            } else {
                binding.tvWordOfDayType.setVisibility(View.GONE);
            }

            String example = word.getExample();
            if (!TextUtils.isEmpty(example)) {
                binding.tvWordOfDayExample.setText(getString(R.string.study_example_format, example, word.getTerm()));
                binding.tvWordOfDayExample.setVisibility(View.VISIBLE);
            } else {
                binding.tvWordOfDayExample.setVisibility(View.GONE);
            }
        });

        viewModel.getWordInMyWords().observe(getViewLifecycleOwner(), inMyWords -> {
            binding.btnWordAddMyWords.setVisibility(
                    Boolean.TRUE.equals(inMyWords) ? View.GONE : View.VISIBLE);
        });

        viewModel.getInfoMessage().observe(getViewLifecycleOwner(), code -> {
            if (code == null) return;
            int msgRes;
            if ("ADDED_TO_MY_WORDS".equals(code)) {
                msgRes = R.string.home_wod_added_toast;
            } else if ("ADD_TO_MY_WORDS_FAILED".equals(code)) {
                msgRes = R.string.home_wod_add_failed;
            } else {
                return;
            }
            Toast.makeText(getContext(), msgRes, Toast.LENGTH_SHORT).show();
            viewModel.clearInfoMessage();
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                viewModel.clearError();
            }
        });

        viewModel.getSearchResults().observe(getViewLifecycleOwner(), results -> {
            if (results == null || results.isEmpty()) {
                binding.cardSearchResults.setVisibility(View.GONE);
            } else {
                binding.cardSearchResults.setVisibility(View.VISIBLE);
                searchAdapter.submitList(results);
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
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                String query = binding.etSearch.getText().toString().trim();
                if (!query.isEmpty()) {
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
            if (Topic.MY_WORDS_TOPIC_ID.equals(topicId)) {
                intent.putExtra(StudyFlashcardActivity.EXTRA_TOPIC_TITLE, getString(R.string.home_my_words));
            }
            startActivity(intent);
        });

        binding.cardLockScreenSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), LockScreenConfigActivity.class);
            startActivity(intent);
        });

        binding.btnWordVolume.setOnClickListener(v -> speakWordOfTheDay());
        binding.btnWordAddMyWords.setOnClickListener(v -> viewModel.addWordOfTheDayToMyWords());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
        super.onDestroy();
    }
}