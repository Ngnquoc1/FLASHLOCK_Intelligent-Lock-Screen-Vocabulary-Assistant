package com.nhom18.flashlock.ui.vocabulary;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nhom18.flashlock.R;
import com.nhom18.flashlock.data.model.Word;
import com.nhom18.flashlock.data.model.Topic;
import com.nhom18.flashlock.ui.vocabulary.AddWordBottomSheet.AddWordInput;

import java.util.ArrayList;
import java.util.List;

import androidx.core.content.ContextCompat;

public class VocabularyFragment extends Fragment {

    private boolean isVocabularyTab = true;
    private boolean isServerSearchActive = false;
    private String statusFilter = null;
    private String searchQuery = "";
    private List<Word> cachedWords = new ArrayList<>();
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private VocabularyViewModel viewModel;
    private VocabularyAdapter vocabularyAdapter;
    private TopicAdapter topicAdapter;

    private TextView tvEmptyState;
    private TextView tvErrorState;
    private View loadingView;

    private List<Topic> cachedTopics = new ArrayList<>();

    public static VocabularyFragment newInstance() {
        return new VocabularyFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vocabulary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvTabVocab = view.findViewById(R.id.tv_tab_vocabulary);
        TextView tvTabTopics = view.findViewById(R.id.tv_tab_topics);
        TextView tvTitle = view.findViewById(R.id.tv_title);
        TextView tvSubtitle = view.findViewById(R.id.tv_subtitle);
        View filterScroll = view.findViewById(R.id.filter_scroll);
        View exploreFooter = view.findViewById(R.id.explore_footer);
        View fabAdd = view.findViewById(R.id.fab_add);
        View btnExploreLibrary = view.findViewById(R.id.btn_explore_library);

        TextView chipAll = view.findViewById(R.id.chip_all_words);
        TextView chipNew = view.findViewById(R.id.chip_new_words);
        TextView chipLearning = view.findViewById(R.id.chip_learning_words);
        TextView chipMastered = view.findViewById(R.id.chip_mastered_words);

        TextView etSearch = view.findViewById(R.id.et_search);

        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        tvErrorState = view.findViewById(R.id.tv_error_state);
        loadingView = view.findViewById(R.id.progress_loading);

        RecyclerView recyclerView = view.findViewById(R.id.rv_content);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        tvTabVocab.setOnClickListener(v ->
                switchToVocabularyTab(tvTabVocab, tvTabTopics, tvTitle, tvSubtitle, filterScroll, exploreFooter, fabAdd, recyclerView));
        tvTabTopics.setOnClickListener(v ->
                switchToTopicsTab(tvTabTopics, tvTabVocab, tvTitle, tvSubtitle, filterScroll, exploreFooter, fabAdd, recyclerView));

        vocabularyAdapter = new VocabularyAdapter(new VocabularyAdapter.WordActionListener() {
            @Override
            public void onDelete(Word word) {
                showDeleteConfirm(word);
            }

            @Override
            public void onEdit(Word word) {
                showEditWordBottomSheet(word);
            }
        });
        topicAdapter = new TopicAdapter();

        viewModel = new ViewModelProvider(this).get(VocabularyViewModel.class);
        viewModel.getWords().observe(getViewLifecycleOwner(), words -> {
            cachedWords = words != null ? words : new ArrayList<>();
            applyFilter();
        });
        viewModel.getTopics().observe(getViewLifecycleOwner(), topics -> {
            cachedTopics = topics != null ? topics : new ArrayList<>();
            topicAdapter.submitList(cachedTopics);
            updateTopicsEmptyState();
        });
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            loadingView.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE);
        });
        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            String resolved = resolveErrorMessage(message);
            if (resolved != null && !resolved.trim().isEmpty()) {
                tvErrorState.setText(resolved);
                tvErrorState.setVisibility(View.VISIBLE);
                Toast.makeText(requireContext(), resolved, Toast.LENGTH_SHORT).show();
            } else {
                tvErrorState.setVisibility(View.GONE);
            }
        });

        chipAll.setOnClickListener(v -> {
            statusFilter = null;
            updateFilterChips(chipAll, chipNew, chipLearning, chipMastered);
            applyFilter();
        });
        chipNew.setOnClickListener(v -> {
            statusFilter = Word.STATUS_NEW;
            updateFilterChips(chipAll, chipNew, chipLearning, chipMastered);
            applyFilter();
        });
        chipLearning.setOnClickListener(v -> {
            statusFilter = Word.STATUS_LEARNING;
            updateFilterChips(chipAll, chipNew, chipLearning, chipMastered);
            applyFilter();
        });
        chipMastered.setOnClickListener(v -> {
            statusFilter = Word.STATUS_MASTERED;
            updateFilterChips(chipAll, chipNew, chipLearning, chipMastered);
            applyFilter();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s != null ? s.toString().trim() : "";
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> {
                    if (searchQuery.length() >= 2) {
                        isServerSearchActive = true;
                        viewModel.searchVocabulary(searchQuery);
                    } else {
                        isServerSearchActive = false;
                        if (searchQuery.isEmpty()) {
                            viewModel.loadVocabulary();
                        }
                        applyFilter();
                    }
                };
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No-op
            }
        });

        btnExploreLibrary.setOnClickListener(v -> {
            View navLibrary = requireActivity().findViewById(R.id.nav_library_item);
            if (navLibrary != null) {
                navLibrary.performClick();
            }
        });

        fabAdd.setOnClickListener(v -> showAddWordBottomSheet());

        switchToVocabularyTab(tvTabVocab, tvTabTopics, tvTitle, tvSubtitle, filterScroll, exploreFooter, fabAdd, recyclerView);
    }

    private void switchToVocabularyTab(TextView selected, TextView unselected, TextView tvTitle, TextView tvSubtitle,
                                       View filterScroll, View exploreFooter, View fabAdd, RecyclerView recyclerView) {
        if (!isVocabularyTab) {
            isVocabularyTab = true;
        }
        updateTabs(selected, unselected);
        tvTitle.setText(getString(R.string.vocab_title_my));
        tvSubtitle.setText(getString(R.string.vocab_subtitle_my));
        filterScroll.setVisibility(View.VISIBLE);
        exploreFooter.setVisibility(View.GONE);
        fabAdd.setVisibility(View.VISIBLE);
        recyclerView.setAdapter(vocabularyAdapter);
        viewModel.loadVocabulary();
    }

    private void switchToTopicsTab(TextView selected, TextView unselected, TextView tvTitle, TextView tvSubtitle,
                                   View filterScroll, View exploreFooter, View fabAdd, RecyclerView recyclerView) {
        if (isVocabularyTab) {
            isVocabularyTab = false;
        }
        updateTabs(selected, unselected);
        tvTitle.setText(getString(R.string.vocab_title_topics));
        tvSubtitle.setText(getString(R.string.vocab_subtitle_topics));
        filterScroll.setVisibility(View.GONE);
        exploreFooter.setVisibility(View.VISIBLE);
        fabAdd.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);
        tvEmptyState.setText(R.string.vocab_empty_topics);
        updateTopicsEmptyState();
        recyclerView.setAdapter(topicAdapter);
        viewModel.loadTopics();
    }

    private void applyFilter() {
        List<Word> filtered = WordFilter.apply(cachedWords, statusFilter, searchQuery, !isServerSearchActive);
        vocabularyAdapter.submitList(filtered);
        if (isVocabularyTab) {
            tvEmptyState.setText(R.string.vocab_empty_state);
            tvEmptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void updateTopicsEmptyState() {
        if (isVocabularyTab) {
            return;
        }
        boolean isEmpty = cachedTopics.isEmpty();
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        tvEmptyState.setText(R.string.vocab_empty_topics);
        View exploreFooter = requireView().findViewById(R.id.explore_footer);
        if (exploreFooter != null) {
            exploreFooter.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
    }

    private String resolveErrorMessage(String code) {
        if (code == null) {
            return null;
        }
        switch (code) {
            case "WORD_REQUIRED":
                return getString(R.string.vocab_error_word_required);
            case "WORD_TERM_REQUIRED":
                return getString(R.string.vocab_error_term_required);
            case "WORD_DEFINITION_REQUIRED":
                return getString(R.string.vocab_error_definition_required);
            case "WORD_ID_REQUIRED":
                return getString(R.string.vocab_error_word_id_required);
            case "ADD_WORD_FAILED":
                return getString(R.string.vocab_error_add_failed);
            case "UPDATE_WORD_FAILED":
                return getString(R.string.vocab_error_update_failed);
            case "DELETE_WORD_FAILED":
                return getString(R.string.vocab_error_delete_failed);
            case "LOAD_WORDS_FAILED":
                return getString(R.string.vocab_error_load_failed);
            case "SEARCH_WORDS_FAILED":
                return getString(R.string.vocab_error_search_failed);
            case "LOAD_TOPICS_FAILED":
                return getString(R.string.vocab_error_load_topics_failed);
            default:
                return code;
        }
    }

    private void updateFilterChips(TextView chipAll, TextView chipNew, TextView chipLearning, TextView chipMastered) {
        setChipSelected(chipAll, statusFilter == null);
        setChipSelected(chipNew, Word.STATUS_NEW.equals(statusFilter));
        setChipSelected(chipLearning, Word.STATUS_LEARNING.equals(statusFilter));
        setChipSelected(chipMastered, Word.STATUS_MASTERED.equals(statusFilter));
    }

    private void setChipSelected(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        chip.setTextColor(ContextCompat.getColor(requireContext(), selected ? R.color.on_primary_container : R.color.white));
    }

    private void updateTabs(TextView selected, TextView unselected) {
        selected.setBackgroundResource(R.drawable.bg_tab_selected);
        selected.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        selected.setAlpha(1.0f);

        unselected.setBackground(null);
        unselected.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface));
        unselected.setAlpha(0.6f);
    }

    private void showAddWordBottomSheet() {
        AddWordBottomSheet bottomSheet = new AddWordBottomSheet();
        bottomSheet.setListener(this::handleAddWordSubmit);
        bottomSheet.show(getChildFragmentManager(), "AddWordBottomSheet");
    }

    private void showEditWordBottomSheet(Word word) {
        AddWordBottomSheet bottomSheet = new AddWordBottomSheet();
        bottomSheet.setEditingWord(word);
        bottomSheet.setListener(this::handleAddWordSubmit);
        bottomSheet.show(getChildFragmentManager(), "EditWordBottomSheet");
    }

    private void handleAddWordSubmit(AddWordInput input) {
        Word word = new Word();
        word.setWordId(input.getWordId());
        word.setTerm(input.getTerm());
        word.setDefinition(input.getDefinition());
        word.setPronunciation(input.getPronunciation());
        word.setExample(input.getExample());
        word.setStatus(input.getStatus());
        word.setWordType(input.getWordType());

        if (input.getWordId() != null && !input.getWordId().trim().isEmpty()) {
            viewModel.updateWord(word);
        } else {
            viewModel.addWord(word);
        }
    }

    private void showDeleteConfirm(Word word) {
        if (word == null) {
            return;
        }
        String term = word.getTerm() != null ? word.getTerm() : "";
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.vocab_delete_title)
                .setMessage(getString(R.string.vocab_delete_message, term))
                .setPositiveButton(R.string.vocab_delete_confirm, (dialog, which) -> viewModel.deleteWord(word.getWordId()))
                .setNegativeButton(R.string.vocab_delete_cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}
