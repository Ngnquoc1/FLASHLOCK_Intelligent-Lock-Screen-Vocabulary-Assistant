package com.nhom18.flashlock.ui.study;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.nhom18.flashlock.data.model.UserWordProgress;
import com.nhom18.flashlock.data.model.Word;
import com.nhom18.flashlock.data.remote.FirebaseTopicWordDataSource;
import com.nhom18.flashlock.data.remote.FirebaseUserWordProgressDataSource;
import com.nhom18.flashlock.data.repository.FirebaseTopicWordRepository;
import com.nhom18.flashlock.data.repository.FirebaseUserWordProgressRepository;
import com.nhom18.flashlock.data.repository.TopicWordRepository;
import com.nhom18.flashlock.data.repository.UserWordProgressRepository;
import com.nhom18.flashlock.domain.srs.SrsScheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class StudyViewModel extends ViewModel {
    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    private final TopicWordRepository topicRepository;
    private final UserWordProgressRepository progressRepository;
    private final SrsScheduler scheduler;

    private final MutableLiveData<StudyUiState> state =
            new MutableLiveData<>(new StudyUiState(false, null, null, 0, 0, 0, 0, 0));

    private List<StudyCard> queue = new ArrayList<>();
    private List<Word> allWords = new ArrayList<>();
    private int currentIndex = 0;
    private Map<String, UserWordProgress> progressByWord = new HashMap<>();
    private String topicId;

    private boolean isCramMode = false;

    public StudyViewModel() {
        this(new FirebaseTopicWordRepository(new FirebaseTopicWordDataSource()),
                new FirebaseUserWordProgressRepository(new FirebaseUserWordProgressDataSource()),
                new SrsScheduler());
    }

    StudyViewModel(TopicWordRepository topicRepository,
                   UserWordProgressRepository progressRepository,
                   SrsScheduler scheduler) {
        this.topicRepository = topicRepository;
        this.progressRepository = progressRepository;
        this.scheduler = scheduler;
    }

    public LiveData<StudyUiState> getState() {
        return state;
    }

    public void loadTopic(String topicId) {
        isCramMode = false;
        if (topicId == null || topicId.trim().isEmpty()) {
            state.setValue(new StudyUiState(false, "TOPIC_ID_REQUIRED", null, 0, 0, 0, 0, 0));
            return;
        }
        this.topicId = topicId;
        state.setValue(new StudyUiState(true, null, null, 0, 0, 0, 0, 0));

        Task<List<Word>> wordsTask = topicRepository.getTopicWords(topicId);
        Task<List<UserWordProgress>> progressTask = progressRepository.getProgressByTopic(topicId);

        Tasks.whenAllComplete(wordsTask, progressTask)
                .addOnCompleteListener(DIRECT_EXECUTOR, task -> {
                    if (!wordsTask.isSuccessful()) {
                        state.postValue(new StudyUiState(false, "LOAD_STUDY_FAILED", null, 0, 0, 0, 0, 0));
                        return;
                    }
                    List<Word> words = wordsTask.getResult() != null ? wordsTask.getResult() : new ArrayList<>();
                    List<UserWordProgress> progress = progressTask.isSuccessful() && progressTask.getResult() != null
                            ? progressTask.getResult() : new ArrayList<>();
                    buildSession(words, progress);
                });
    }

    public void startCramMode(String topicId) {
        isCramMode = true;
        if (topicId == null || topicId.trim().isEmpty()) {
            state.setValue(new StudyUiState(false, "TOPIC_ID_REQUIRED", null, 0, 0, 0, 0, 0));
            return;
        }
        this.topicId = topicId;
        state.setValue(new StudyUiState(true, null, null, 0, 0, 0, 0, 0));
        topicRepository.getTopicWords(topicId)
                .addOnCompleteListener(DIRECT_EXECUTOR, task -> {
                    if (!task.isSuccessful()) {
                        state.postValue(new StudyUiState(false, "LOAD_STUDY_FAILED", null, 0, 0, 0, 0, 0));
                        return;
                    }
                    List<Word> words = task.getResult() != null ? task.getResult() : new ArrayList<>();
                    buildCramSession(words);
                });
    }

    public void onRemembered() {
        if (isCramMode) {
            moveNext();
            return;
        }
        StudyCard card = getCurrentCard();
        if (card == null) {
            return;
        }
        UserWordProgress progress = ensureProgress(card);
        int nextLevel = scheduler.nextLevelOnRemember(progress.getBoxLevel());
        progress.setBoxLevel(nextLevel);
        progress.setStatus(nextLevel >= 5 ? Word.STATUS_MASTERED : Word.STATUS_REVIEW);
        progress.setNextReviewAt(scheduler.nextReviewForLevel(nextLevel, Timestamp.now()));
        progress.setUpdatedAt(Timestamp.now());
        updateProgressAndAdvance(progress);
    }

    public void onForgot() {
        if (isCramMode) {
            moveNext();
            return;
        }
        StudyCard card = getCurrentCard();
        if (card == null) {
            return;
        }
        UserWordProgress progress = ensureProgress(card);
        progress.setBoxLevel(1);
        progress.setStatus(Word.STATUS_REVIEW);
        progress.setNextReviewAt(scheduler.nextReviewForLevel(1, Timestamp.now()));
        progress.setUpdatedAt(Timestamp.now());
        updateProgressAndAdvance(progress);
    }

    private void updateProgressAndAdvance(UserWordProgress progress) {
        progressByWord.put(progress.getWordId(), progress);
        moveNext();
        progressRepository.upsertProgress(progress).addOnCompleteListener(DIRECT_EXECUTOR, task -> {
            if (!task.isSuccessful()) {
                state.postValue(new StudyUiState(false, "SAVE_PROGRESS_FAILED",
                        getCurrentCard(), currentIndex + 1, queue.size(),
                        getNewCount(), getLearningCount(), getMasteredCount()));
            }
        });
    }

    private void buildSession(List<Word> words, List<UserWordProgress> progress) {
        isCramMode = false;
        progressByWord = new HashMap<>();
        if (progress != null) {
            for (UserWordProgress item : progress) {
                if (item != null && item.getWordId() != null) {
                    progressByWord.put(item.getWordId(), item);
                }
            }
        }
        queue = new ArrayList<>();
        allWords = words != null ? new ArrayList<>(words) : new ArrayList<>();
        Timestamp now = Timestamp.now();
        if (words != null) {
            for (Word word : words) {
                if (word == null) continue;
                UserWordProgress existing = progressByWord.get(word.getWordId());
                String status = existing != null ? existing.getStatus() : Word.STATUS_NEW;
                boolean due = Word.STATUS_NEW.equals(status);
                if (!due && existing != null && existing.getNextReviewAt() != null) {
                    due = existing.getNextReviewAt().compareTo(now) <= 0;
                }
                if (due && !Word.STATUS_MASTERED.equals(status)) {
                    queue.add(new StudyCard(word, existing));
                }
            }
        }
        currentIndex = 0;
        if (queue.isEmpty()) {
            state.postValue(new StudyUiState(false, "NO_DUE_WORDS", null, 0, 0,
                    getNewCount(), getLearningCount(), getMasteredCount()));
        } else {
            state.postValue(new StudyUiState(false, null, queue.get(0), 1, queue.size(),
                    getNewCount(), getLearningCount(), getMasteredCount()));
        }
    }

    private void buildCramSession(List<Word> words) {
        queue = new ArrayList<>();
        allWords = words != null ? new ArrayList<>(words) : new ArrayList<>();
        if (words != null) {
            for (Word word : words) {
                if (word == null) {
                    continue;
                }
                queue.add(new StudyCard(word, progressByWord.get(word.getWordId())));
            }
        }
        Collections.shuffle(queue);
        currentIndex = 0;
        if (queue.isEmpty()) {
            state.postValue(new StudyUiState(false, "NO_DUE_WORDS", null, 0, 0,
                    getNewCount(), getLearningCount(), getMasteredCount()));
        } else {
            state.postValue(new StudyUiState(false, null, queue.get(0), 1, queue.size(),
                    getNewCount(), getLearningCount(), getMasteredCount()));
        }
    }

    private void moveNext() {
        if (queue.isEmpty()) {
            return;
        }
        currentIndex++;
        if (currentIndex >= queue.size()) {
            state.postValue(new StudyUiState(false, "NO_DUE_WORDS", null, 0, 0,
                    getNewCount(), getLearningCount(), getMasteredCount()));
            return;
        }
        state.postValue(new StudyUiState(false, null, queue.get(currentIndex), currentIndex + 1, queue.size(),
                getNewCount(), getLearningCount(), getMasteredCount()));
    }

    private StudyCard getCurrentCard() {
        if (queue.isEmpty() || currentIndex >= queue.size()) {
            return null;
        }
        return queue.get(currentIndex);
    }

    private UserWordProgress ensureProgress(StudyCard card) {
        UserWordProgress progress = card.getProgress();
        if (progress == null) {
            progress = new UserWordProgress();
            progress.setWordId(card.getWord().getWordId());
            progress.setTopicId(topicId);
            progress.setStatus(Word.STATUS_NEW);
            progress.setBoxLevel(1);
        }
        return progress;
    }

    private int getNewCount() {
        return countByStatus(Word.STATUS_NEW);
    }

    private int getLearningCount() {
        int count = 0;
        for (Word word : allWords) {
            String value = statusFor(word);
            if (Word.STATUS_LEARNING.equals(value) || Word.STATUS_REVIEW.equals(value)) {
                count++;
            }
        }
        return count;
    }

    private int getMasteredCount() {
        return countByStatus(Word.STATUS_MASTERED);
    }

    private int countByStatus(String status) {
        int count = 0;
        for (Word word : allWords) {
            String value = statusFor(word);
            if (status.equals(value)) {
                count++;
            }
        }
        return count;
    }

    private String statusFor(Word word) {
        if (word == null) {
            return Word.STATUS_NEW;
        }
        UserWordProgress progress = progressByWord.get(word.getWordId());
        if (progress != null && progress.getStatus() != null) {
            return progress.getStatus();
        }
        return Word.STATUS_NEW;
    }
}
