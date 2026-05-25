package com.nhom18.flashlock.ui.library;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom18.flashlock.R;
import com.nhom18.flashlock.data.model.Word;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LibraryTopicWordsActivity extends AppCompatActivity {
    public static final String EXTRA_TOPIC_ID = "extra_topic_id";
    public static final String EXTRA_TOPIC_TITLE = "extra_topic_title";
    public static final String EXTRA_TOPIC_CATEGORY = "extra_topic_category";
    public static final String EXTRA_TOPIC_LANGUAGE = "extra_topic_language";
    public static final String EXTRA_TOPIC_WORD_COUNT = "extra_topic_word_count";

    private final List<Word> cachedWords = new ArrayList<>();
    private boolean isLoading = false;
    private boolean hasError = false;

    private TextView tvTopicTitle;
    private TextView tvTopicMeta;
    private TextView tvEmptyState;
    private View loadingView;

    private LibraryTopicWordsViewModel viewModel;
    private LibraryTopicWordAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library_topic_words);

        ImageView btnBack = findViewById(R.id.iv_topic_words_back);
        tvTopicTitle = findViewById(R.id.tv_topic_words_topic_title);
        tvTopicMeta = findViewById(R.id.tv_topic_words_meta);
        tvEmptyState = findViewById(R.id.tv_topic_words_empty);
        loadingView = findViewById(R.id.progress_topic_words_loading);

        btnBack.setOnClickListener(v -> finish());

        Intent intent = getIntent();
        String topicId = intent != null ? intent.getStringExtra(EXTRA_TOPIC_ID) : null;
        if (topicId == null || topicId.trim().isEmpty()) {
            Toast.makeText(this, R.string.library_open_topic_missing_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String title = intent.getStringExtra(EXTRA_TOPIC_TITLE);
        tvTopicTitle.setText(!isBlank(title) ? title : getString(R.string.topic_words_fallback_title));
        bindMeta(intent.getStringExtra(EXTRA_TOPIC_CATEGORY),
                intent.getStringExtra(EXTRA_TOPIC_LANGUAGE),
                intent.getLongExtra(EXTRA_TOPIC_WORD_COUNT, 0L));

        RecyclerView recyclerView = findViewById(R.id.rv_topic_words);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LibraryTopicWordAdapter();
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(LibraryTopicWordsViewModel.class);
        viewModel.getWords().observe(this, words -> {
            cachedWords.clear();
            if (words != null) {
                cachedWords.addAll(words);
            }
            hasError = false;
            adapter.submitList(new ArrayList<>(cachedWords));
            updateEmptyState();
        });
        viewModel.getLoading().observe(this, loading -> {
            isLoading = Boolean.TRUE.equals(loading);
            loadingView.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            updateEmptyState();
        });
        viewModel.getError().observe(this, code -> {
            if (code == null) {
                return;
            }
            hasError = true;
            String message = resolveErrorMessage(code);
            tvEmptyState.setText(message);
            tvEmptyState.setVisibility(View.VISIBLE);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });

        viewModel.loadTopicWords(topicId);
    }

    private void bindMeta(String category, String language, long wordCount) {
        List<String> parts = new ArrayList<>();
        if (!isBlank(category)) {
            parts.add(category.trim());
        }
        if (!isBlank(language)) {
            parts.add(language.trim().toUpperCase(Locale.US));
        }
        if (wordCount > 0) {
            parts.add(getString(R.string.topic_words_count_format, wordCount));
        }
        if (parts.isEmpty()) {
            tvTopicMeta.setVisibility(View.GONE);
        } else {
            tvTopicMeta.setVisibility(View.VISIBLE);
            tvTopicMeta.setText(TextUtils.join(" - ", parts));
        }
    }

    private void updateEmptyState() {
        if (isLoading) {
            tvEmptyState.setVisibility(View.GONE);
            return;
        }
        if (hasError) {
            tvEmptyState.setVisibility(View.VISIBLE);
            return;
        }
        boolean isEmpty = cachedWords.isEmpty();
        tvEmptyState.setText(R.string.topic_words_empty);
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private String resolveErrorMessage(String code) {
        if ("TOPIC_ID_REQUIRED".equals(code)) {
            return getString(R.string.library_open_topic_missing_id);
        }
        if ("LOAD_TOPIC_WORDS_FAILED".equals(code)) {
            return getString(R.string.topic_words_load_failed);
        }
        return code;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

