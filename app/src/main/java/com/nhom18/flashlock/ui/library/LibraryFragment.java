package com.nhom18.flashlock.ui.library;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;

import com.nhom18.flashlock.R;
import com.nhom18.flashlock.data.model.Topic;
import com.nhom18.flashlock.ui.main.MainActivity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LibraryFragment extends Fragment {

    private static final String CATEGORY_ALL = "all";
    private static final String TAG = "LibraryFragment";

    private LibraryViewModel viewModel;
    private LibraryTopicAdapter adapter;
    private RecyclerView recyclerView;
    private List<Topic> cachedTopics = new ArrayList<>();
    private String activeCategory = CATEGORY_ALL;

    private TextView tvEmptyState;
    private View loadingView;
    private LinearLayout chipContainer;
    private final List<TextView> chipViews = new ArrayList<>();
    private boolean isLoading = false;
    private boolean isNavigatingToStudy = false;

    public static LibraryFragment newInstance() {
        return new LibraryFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chipContainer = view.findViewById(R.id.chip_library_container);
        tvEmptyState = view.findViewById(R.id.tv_library_empty_state);
        loadingView = view.findViewById(R.id.library_loading_container);

        recyclerView = view.findViewById(R.id.rv_library_topics);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(false);
        recyclerView.setItemAnimator(null);

        adapter = new LibraryTopicAdapter(new LibraryTopicAdapter.TopicActionListener() {
            @Override
            public void onStudyNow(Topic topic) {
                isNavigatingToStudy = true;
                viewModel.saveTopic(topic);
            }

            @Override
            public void onSaveTopic(Topic topic) {
                isNavigatingToStudy = false;
                viewModel.saveTopic(topic);
            }

            @Override
            public void onOpenTopic(Topic topic) {
                openTopicWords(topic);
            }
        });
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(LibraryViewModel.class);
        
        viewModel.getTopics().observe(getViewLifecycleOwner(), topics -> {
            cachedTopics = topics != null ? new ArrayList<>(topics) : new ArrayList<>();
            updateCategoryChips(cachedTopics);
            applyFilter();
        });

        viewModel.getSavedTopicIds().observe(getViewLifecycleOwner(), ids -> {
            adapter.setSavedTopicIds(ids);
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoadingValue -> {
            isLoading = Boolean.TRUE.equals(isLoadingValue);
            loadingView.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
            applyFilter();
        });

        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message != null && isAdded()) {
                tvEmptyState.setText(R.string.library_error_load);
                tvEmptyState.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getActionError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && isAdded()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                isNavigatingToStudy = false;
                viewModel.clearActionError();
            }
        });

        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message == null || !isAdded()) return;
            
            if ("LIBRARY_TOPIC_SAVED".equals(message)) {
                Toast.makeText(getContext(), R.string.library_save_success, Toast.LENGTH_SHORT).show();
                if (isNavigatingToStudy && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openVocabularyTopics();
                }
            }
            isNavigatingToStudy = false;
            viewModel.clearMessage();
        });

        viewModel.loadLibrary();
    }

    private void updateCategoryChips(List<Topic> topics) {
        if (!isAdded()) return;
        android.util.Log.d(TAG, "updateCategoryChips: topics=" + (topics != null ? topics.size() : 0));

        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(CATEGORY_ALL, getString(R.string.library_chip_all));

        for (Topic topic : topics) {
            if (topic == null) continue;
            String categoryValue = topic.getCategory();
            if (categoryValue == null || categoryValue.trim().isEmpty()) {
                categoryValue = topic.getLanguage();
            }
            if (categoryValue == null) continue;
            for (String token : extractCategoryTokens(categoryValue)) {
                String key = normalizeCategory(token);
                if (!key.isEmpty() && !labels.containsKey(key)) {
                    labels.put(key, token);
                }
            }
        }

        if (!labels.containsKey(activeCategory)) {
            activeCategory = CATEGORY_ALL;
        }

        chipContainer.removeAllViews();
        chipViews.clear();
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            TextView chip = buildChip(entry.getKey(), entry.getValue());
            chipContainer.addView(chip);
            chipViews.add(chip);
        }
        updateChipStyles();
    }

    private TextView buildChip(String key, String label) {
        Context context = getContext();
        if (context == null) return new TextView(requireContext());

        TextView chip = new TextView(context);
        chip.setTag(key);
        chip.setText(label);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        chip.setPadding(dpToPx(18), dpToPx(8), dpToPx(18), dpToPx(8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(dpToPx(10));
        chip.setLayoutParams(params);
        chip.setOnClickListener(v -> {
            activeCategory = key;
            updateChipStyles();
            applyFilter();
        });
        return chip;
    }

    private void updateChipStyles() {
        Context context = getContext();
        if (context == null) return;

        int selectedColor = ContextCompat.getColor(context, R.color.on_primary_container);
        int normalColor = ContextCompat.getColor(context, R.color.white);
        for (TextView chip : chipViews) {
            String key = chip.getTag() != null ? chip.getTag().toString() : CATEGORY_ALL;
            boolean selected = key.equals(activeCategory);
            chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chip.setTextColor(selected ? selectedColor : normalColor);
        }
    }

    private void applyFilter() {
        if (isLoading) {
            android.util.Log.d(TAG, "applyFilter: loading");
            tvEmptyState.setText(R.string.library_loading);
            tvEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        List<Topic> filtered;
        if (CATEGORY_ALL.equals(activeCategory)) {
            filtered = new ArrayList<>(cachedTopics);
        } else {
            filtered = new ArrayList<>();
            for (Topic topic : cachedTopics) {
                if (matchesCategory(topic, activeCategory)) {
                    filtered.add(topic);
                }
            }
        }
        java.util.Set<String> distinctIds = new java.util.HashSet<>();
        for (Topic topic : filtered) {
            if (topic == null) continue;
            String docId = topic.getDocumentId();
            String key = docId != null ? docId : topic.getTopicId();
            if (key != null) {
                distinctIds.add(key);
            }
        }
        android.util.Log.d(TAG, "applyFilter: category=" + activeCategory + " cached=" + cachedTopics.size()
                + " filtered=" + filtered.size() + " distinctIds=" + distinctIds.size());
        final int expectedCount = filtered.size();
        adapter.submitList(filtered, () -> {
            if (recyclerView != null) recyclerView.requestLayout();
            updateEmptyState(expectedCount);
        });
        updateEmptyState(expectedCount);
    }

    private void updateEmptyState(int count) {
        android.util.Log.d(TAG, "updateEmptyState: count=" + count + " loading=" + isLoading);
        if (isLoading) {
            tvEmptyState.setText(R.string.library_loading);
            tvEmptyState.setVisibility(View.VISIBLE);
            return;
        }
        boolean showEmpty = count == 0;
        if (showEmpty) {
            tvEmptyState.setText(R.string.library_empty_state);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private boolean matchesCategory(Topic topic, String category) {
        if (topic == null) return false;
        if (CATEGORY_ALL.equals(category)) return true;

        String categoryValue = topic.getCategory();
        if (categoryValue == null || categoryValue.trim().isEmpty()) {
            categoryValue = topic.getLanguage();
        }
        if (categoryValue != null) {
            for (String token : extractCategoryTokens(categoryValue)) {
                if (normalizeCategory(token).equals(category)) return true;
            }
        }

        String languageValue = topic.getLanguage();
        if (languageValue != null) {
            for (String token : extractCategoryTokens(languageValue)) {
                if (normalizeCategory(token).equals(category)) return true;
            }
        }
        return false;
    }

    private List<String> extractCategoryTokens(String raw) {
        List<String> tokens = new ArrayList<>();
        if (raw == null) return tokens;
        String[] parts = raw.split("[,;/|]");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) tokens.add(trimmed);
        }
        return tokens;
    }

    private String normalizeCategory(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
    }

    private int dpToPx(int dp) {
        if (!isAdded()) return 0;
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void openTopicWords(Topic topic) {
        if (topic == null || topic.getTopicId() == null) {
            Toast.makeText(getContext(), R.string.library_open_topic_missing_id, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(requireContext(), LibraryTopicWordsActivity.class);
        intent.putExtra(LibraryTopicWordsActivity.EXTRA_TOPIC_ID, topic.getTopicId());
        intent.putExtra(LibraryTopicWordsActivity.EXTRA_TOPIC_TITLE, topic.getTitle());
        intent.putExtra(LibraryTopicWordsActivity.EXTRA_TOPIC_CATEGORY, topic.getCategory());
        intent.putExtra(LibraryTopicWordsActivity.EXTRA_TOPIC_LANGUAGE, topic.getLanguage());
        intent.putExtra(LibraryTopicWordsActivity.EXTRA_TOPIC_WORD_COUNT, topic.getWordCount());
        startActivity(intent);
    }
}
