package com.nhom18.flashlock.ui.vocabulary;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom18.flashlock.R;
import com.nhom18.flashlock.data.model.Word;

import java.util.ArrayList;
import java.util.List;

public class VocabularyFragment extends Fragment {

    private boolean isVocabularyTab = true;
    private String statusFilter = null;
    private List<Word> cachedWords = new ArrayList<>();

    private VocabularyViewModel viewModel;
    private VocabularyAdapter vocabularyAdapter;
    private TopicAdapter topicAdapter;

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

        TextView chipAll = view.findViewById(R.id.chip_all_words);
        TextView chipNew = view.findViewById(R.id.chip_new_words);
        TextView chipLearning = view.findViewById(R.id.chip_learning_words);

        RecyclerView recyclerView = view.findViewById(R.id.rv_content);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        vocabularyAdapter = new VocabularyAdapter(new VocabularyAdapter.WordActionListener() {
            @Override
            public void onDelete(Word word) {
                viewModel.deleteWord(word.getWordId());
            }

            @Override
            public void onEdit(Word word) {
                // Edit flow can be added later.
            }
        });
        topicAdapter = new TopicAdapter();

        viewModel = new ViewModelProvider(this).get(VocabularyViewModel.class);
        viewModel.getWords().observe(getViewLifecycleOwner(), words -> {
            cachedWords = words != null ? words : new ArrayList<>();
            applyFilter();
        });
        viewModel.getTopics().observe(getViewLifecycleOwner(), topics -> topicAdapter.submitList(topics));
        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        chipAll.setOnClickListener(v -> {
            statusFilter = null;
            applyFilter();
        });
        chipNew.setOnClickListener(v -> {
            statusFilter = Word.STATUS_NEW;
            applyFilter();
        });
        chipLearning.setOnClickListener(v -> {
            statusFilter = Word.STATUS_LEARNING;
            applyFilter();
        });

        tvTabVocab.setOnClickListener(v -> switchToVocabularyTab(tvTabVocab, tvTabTopics, tvTitle, tvSubtitle, filterScroll, exploreFooter, fabAdd, recyclerView));
        tvTabTopics.setOnClickListener(v -> switchToTopicsTab(tvTabTopics, tvTabVocab, tvTitle, tvSubtitle, filterScroll, exploreFooter, fabAdd, recyclerView));

        fabAdd.setOnClickListener(v -> showAddWordDialog());

        switchToVocabularyTab(tvTabVocab, tvTabTopics, tvTitle, tvSubtitle, filterScroll, exploreFooter, fabAdd, recyclerView);
    }

    private void switchToVocabularyTab(TextView selected, TextView unselected, TextView tvTitle, TextView tvSubtitle,
                                       View filterScroll, View exploreFooter, View fabAdd, RecyclerView recyclerView) {
        if (!isVocabularyTab) {
            isVocabularyTab = true;
        }
        updateTabs(selected, unselected);
        tvTitle.setText("My Vocabulary");
        tvSubtitle.setText("Manage your saved words");
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
        tvTitle.setText("Saved Topics");
        tvSubtitle.setText("Manage your specialized word collections");
        filterScroll.setVisibility(View.GONE);
        exploreFooter.setVisibility(View.VISIBLE);
        fabAdd.setVisibility(View.GONE);
        recyclerView.setAdapter(topicAdapter);
        viewModel.loadTopics();
    }

    private void applyFilter() {
        if (statusFilter == null) {
            vocabularyAdapter.submitList(cachedWords);
            return;
        }
        List<Word> filtered = new ArrayList<>();
        for (Word word : cachedWords) {
            if (statusFilter.equals(word.getStatus())) {
                filtered.add(word);
            }
        }
        vocabularyAdapter.submitList(filtered);
    }

    private void updateTabs(TextView selected, TextView unselected) {
        selected.setBackgroundResource(R.drawable.bg_tab_selected);
        selected.setTextColor(getResources().getColor(R.color.white));
        selected.setAlpha(1.0f);

        unselected.setBackground(null);
        unselected.setTextColor(getResources().getColor(R.color.on_surface));
        unselected.setAlpha(0.6f);
    }

    private void showAddWordDialog() {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);

        EditText termInput = new EditText(requireContext());
        termInput.setHint("Term");
        container.addView(termInput);

        EditText definitionInput = new EditText(requireContext());
        definitionInput.setHint("Definition");
        container.addView(definitionInput);

        new AlertDialog.Builder(requireContext())
                .setTitle("Add word")
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> viewModel.addWord(
                        termInput.getText().toString(),
                        definitionInput.getText().toString()))
                .setNegativeButton("Cancel", null)
                .show();
    }
}
