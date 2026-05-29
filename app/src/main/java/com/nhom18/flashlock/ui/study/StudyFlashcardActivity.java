package com.nhom18.flashlock.ui.study;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.nhom18.flashlock.R;
import com.nhom18.flashlock.data.model.Word;

import java.util.Locale;

public class StudyFlashcardActivity extends AppCompatActivity {
    public static final String EXTRA_TOPIC_ID = "extra_topic_id";
    public static final String EXTRA_TOPIC_TITLE = "extra_topic_title";
    public static final String EXTRA_TOPIC_CATEGORY = "extra_topic_category";

    private StudyViewModel viewModel;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean showingBack = false;
    private String currentTopicId;

    private View cardFront;
    private View cardBack;
    private View cardContainer;
    private View btnRemember;
    private View btnForget;
    private View studyStats;
    private View studyActions;
    private View emptyStateLayout;
    private boolean isAnimating = false;

    private TextView tvFrontLabel;
    private TextView tvFrontWord;
    private TextView tvFrontPronunciation;
    private TextView tvFrontType;
    private TextView tvBackMeaning;
    private TextView tvBackExample;

    private TextView tvCountNew;
    private TextView tvCountLearning;
    private TextView tvCountMastered;

    private static final long FLIP_DURATION_MS = 240;
    private static final long EXIT_DURATION_MS = 220;

    private boolean isCurrentNew = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_flashcard);

        initViews();
        setupTts();

        viewModel = new ViewModelProvider(this).get(StudyViewModel.class);
        viewModel.getState().observe(this, this::render);

        String topicId = getIntent().getStringExtra(EXTRA_TOPIC_ID);
        currentTopicId = topicId;
        String topicTitle = getIntent().getStringExtra(EXTRA_TOPIC_TITLE);
        String topicCategory = getIntent().getStringExtra(EXTRA_TOPIC_CATEGORY);

        bindTopBar(topicTitle, topicCategory);
        viewModel.loadTopic(topicId);
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.iv_study_back);
        TextView tvTitle = findViewById(R.id.tv_study_title);
        TextView tvSubtitle = findViewById(R.id.tv_study_subtitle);
        btnBack.setOnClickListener(v -> finish());

        cardFront = findViewById(R.id.card_front);
        cardBack = findViewById(R.id.card_back);
        cardContainer = findViewById(R.id.card_container);
        studyStats = findViewById(R.id.study_stats);
        studyActions = findViewById(R.id.study_actions);
        emptyStateLayout = findViewById(R.id.layout_empty_state);

        // Gán lệnh click cho container và mặt thẻ
        cardContainer.setOnClickListener(v -> flipCard());
        cardBack.setOnClickListener(v -> flipCard());

        // Gán lệnh click cho ScrollView
        View svCardBack = findViewById(R.id.sv_card_back);
        if (svCardBack != null) {
            svCardBack.setOnClickListener(v -> flipCard());
        }

        // ÉP LỆNH CLICK CHO LINEARLAYOUT CHỨA NỘI DUNG
        View llCardBackContent = findViewById(R.id.ll_card_back_content);
        if (llCardBackContent != null) {
            llCardBackContent.setOnClickListener(v -> flipCard());
        }

        // BẮT SỰ KIỆN CLICK CHO DÒNG CHỮ "TAP TO FLIP BACK"
        View tvTapToFlipBack = findViewById(R.id.tv_tap_to_flip_back);
        if (tvTapToFlipBack != null) {
            tvTapToFlipBack.setOnClickListener(v -> flipCard());
        }

        tvFrontLabel = findViewById(R.id.tv_front_label);
        tvFrontWord = findViewById(R.id.tv_front_word);
        tvFrontPronunciation = findViewById(R.id.tv_front_pronunciation);
        tvFrontType = findViewById(R.id.tv_front_type);
        tvBackMeaning = findViewById(R.id.tv_back_meaning);
        tvBackExample = findViewById(R.id.tv_back_example);

        tvCountNew = findViewById(R.id.tv_count_new);
        tvCountLearning = findViewById(R.id.tv_count_learning);
        tvCountMastered = findViewById(R.id.tv_count_mastered);

        ImageView btnSpeaker = findViewById(R.id.iv_speaker);
        btnSpeaker.setOnClickListener(v -> speakCurrent());

        btnRemember = findViewById(R.id.btn_remember);
        btnForget = findViewById(R.id.btn_forget);
        btnRemember.setOnClickListener(v -> handleAnswer(true));
        btnForget.setOnClickListener(v -> handleAnswer(false));

        View btnReturnHome = findViewById(R.id.btn_return_home);
        if (btnReturnHome != null) {
            btnReturnHome.setOnClickListener(v -> finish());
        }
        View btnCramMode = findViewById(R.id.btn_cram_mode);
        if (btnCramMode != null) {
            btnCramMode.setOnClickListener(v -> {
                viewModel.startCramMode(currentTopicId);
                showStudyState();
                Toast.makeText(this, R.string.study_cram_toast, Toast.LENGTH_SHORT).show();
            });
        }

        tvTitle.setText("");
        tvSubtitle.setText("");
    }

    private void bindTopBar(String title, String category) {
        TextView tvTitle = findViewById(R.id.tv_study_title);
        TextView tvSubtitle = findViewById(R.id.tv_study_subtitle);
        tvTitle.setText(title != null ? title : getString(R.string.study_title_fallback));
        if (category != null && !category.trim().isEmpty()) {
            tvSubtitle.setText(category);
        } else {
            tvSubtitle.setText("");
        }
    }

    private void render(StudyUiState state) {
        if (state == null) {
            return;
        }
        if (state.isLoading()) {
            return;
        }
        if (state.getError() != null) {
            handleError(state.getError());
        }
        tvCountNew.setText(String.valueOf(state.getNewCount()));
        tvCountLearning.setText(String.valueOf(state.getLearningCount()));
        tvCountMastered.setText(String.valueOf(state.getMasteredCount()));

        StudyCard card = state.getCurrentCard();
        if (card == null) {
            showEmptyState();
            return;
        }
        Word word = card.getWord();
        if (word == null) {
            showEmptyState();
            return;
        }
        showStudyState();
        String status = card.getProgress() != null ? card.getProgress().getStatus() : null;
        isCurrentNew = status == null || Word.STATUS_NEW.equals(status);

        tvFrontLabel.setText(R.string.study_label_vocab_mastery);
        String term = resolveTerm(word);
        tvFrontWord.setText(term);
        tvFrontPronunciation.setText(word.getPronunciation() != null ? word.getPronunciation() : "");
        tvFrontType.setText(word.getWordType() != null ? word.getWordType() : getString(R.string.study_word_type_unknown));
        tvBackMeaning.setText(word.getDefinition() != null ? word.getDefinition() : "");
        if (word.getExample() != null && !word.getExample().trim().isEmpty()) {
            tvBackExample.setText(getString(R.string.study_example_format, word.getExample(), term));
        } else {
            tvBackExample.setText("");
        }
    }

    private void showStudyState() {
        if (cardContainer != null) {
            cardContainer.setVisibility(View.VISIBLE);
        }
        if (studyStats != null) {
            studyStats.setVisibility(View.VISIBLE);
        }
        if (studyActions != null) {
            studyActions.setVisibility(View.VISIBLE);
        }
        if (emptyStateLayout != null) {
            emptyStateLayout.setVisibility(View.GONE);
        }
    }

    private void showEmptyState() {
        isCurrentNew = false;
        resetToFront();
        if (cardContainer != null) {
            cardContainer.setVisibility(View.GONE);
        }
        if (studyStats != null) {
            studyStats.setVisibility(View.GONE);
        }
        if (studyActions != null) {
            studyActions.setVisibility(View.GONE);
        }
        if (emptyStateLayout != null) {
            emptyStateLayout.setVisibility(View.VISIBLE);
        }
    }

    private void handleError(String code) {
        if ("NO_DUE_WORDS".equals(code)) {
            Toast.makeText(this, R.string.study_empty_no_due, Toast.LENGTH_SHORT).show();
        } else if ("TOPIC_ID_REQUIRED".equals(code)) {
            Toast.makeText(this, R.string.vocab_error_topic_id_required, Toast.LENGTH_SHORT).show();
            finish();
        } else if ("SAVE_PROGRESS_FAILED".equals(code)) {
            Toast.makeText(this, R.string.study_error_save_failed, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.study_error_load_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void flipCard() {
        if (showingBack) {
            flipToFront(null);
        } else {
            flipToBack(null);
        }
    }

    private void runWithBackVisible(Runnable action) {
        if (showingBack) {
            action.run();
        } else {
            flipToBack(action);
        }
    }

    private void flipToBack(@Nullable Runnable endAction) {
        animateFlip(cardFront, cardBack, 0f, 90f, -90f, 0f, true, endAction);
    }

    private void flipToFront(@Nullable Runnable endAction) {
        animateFlip(cardBack, cardFront, 0f, 90f, -90f, 0f, false, endAction);
    }

    private void animateFlip(View outView,
                             View inView,
                             float outFrom,
                             float outTo,
                             float inFrom,
                             float inTo,
                             boolean toBack,
                             @Nullable Runnable endAction) {
        float scale = getResources().getDisplayMetrics().density;
        cardFront.setCameraDistance(scale * 8000);
        cardBack.setCameraDistance(scale * 8000);

        AnimatorSet set = new AnimatorSet();
        ObjectAnimator out = ObjectAnimator.ofFloat(outView, "rotationY", outFrom, outTo);
        ObjectAnimator in = ObjectAnimator.ofFloat(inView, "rotationY", inFrom, inTo);
        out.setDuration(FLIP_DURATION_MS);
        in.setDuration(FLIP_DURATION_MS);
        out.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                outView.setVisibility(View.INVISIBLE);
                inView.setVisibility(View.VISIBLE);
            }
        });
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                showingBack = toBack;
                if (endAction != null) {
                    endAction.run();
                }
            }
        });
        set.playSequentially(out, in);
        set.start();
    }

    private void resetToFront() {
        showingBack = false;
        cardBack.setVisibility(View.INVISIBLE);
        cardFront.setVisibility(View.VISIBLE);
        cardFront.setRotationY(0f);
        cardBack.setRotationY(-90f);
    }

    private void setupTts() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });
    }

    private void speakCurrent() {
        if (!ttsReady) {
            Toast.makeText(this, R.string.study_tts_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        StudyUiState current = viewModel.getState().getValue();
        if (current == null || current.getCurrentCard() == null || current.getCurrentCard().getWord() == null) {
            return;
        }
        String term = resolveTerm(current.getCurrentCard().getWord());
        if (term == null || term.trim().isEmpty()) {
            return;
        }
        tts.speak(term, TextToSpeech.QUEUE_FLUSH, null, "tts_word");
    }

    private void handleAnswer(boolean remembered) {
        if (isAnimating) {
            return;
        }
//        if (remembered && !showingBack && !isCurrentNew) {
//            return;
//        }
        isAnimating = true;
        animateCardExit(remembered ? 1 : -1, () -> {
            if (remembered) {
                viewModel.onRemembered();
            } else {
                viewModel.onForgot();
            }
            resetToFront();
            resetCardPosition();
            isAnimating = false;
        });
    }

    private void animateCardExit(int direction, @Nullable Runnable endAction) {
        float baseDistance = cardContainer != null ? cardContainer.getWidth() : 0f;
        if (baseDistance <= 0f) {
            float density = getResources().getDisplayMetrics().density;
            baseDistance = 200f * density;
        }
        float targetX = direction * baseDistance * 0.4f;
        ObjectAnimator move = ObjectAnimator.ofFloat(cardContainer, "translationX", 0f, targetX);
        ObjectAnimator fade = ObjectAnimator.ofFloat(cardContainer, "alpha", 1f, 0f);
        AnimatorSet set = new AnimatorSet();
        set.setDuration(EXIT_DURATION_MS);
        set.playTogether(move, fade);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (endAction != null) {
                    endAction.run();
                }
            }
        });
        set.start();
    }

    private void resetCardPosition() {
        if (cardContainer == null) {
            return;
        }
        cardContainer.setTranslationX(0f);
        cardContainer.setAlpha(1f);
    }

    private String resolveTerm(Word word) {
        if (word == null) {
            return "";
        }
        String term = word.getTerm();
        if (term != null && !term.trim().isEmpty()) {
            return term;
        }
        String wordId = word.getWordId();
        if (wordId == null || wordId.trim().isEmpty()) {
            return "";
        }
        String normalized = wordId.trim();
        if (currentTopicId != null && normalized.startsWith(currentTopicId + "_")) {
            normalized = normalized.substring(currentTopicId.length() + 1);
        }
        return normalized.replace('_', ' ');
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.shutdown();
        }
        super.onDestroy();
    }
}