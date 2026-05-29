package com.nhom18.flashlock.ui.study;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.nhom18.flashlock.data.model.UserProfile;
import com.nhom18.flashlock.data.model.UserWordProgress;
import com.nhom18.flashlock.data.model.Word;
import com.nhom18.flashlock.data.remote.FirebaseProfileDataSource;
import com.nhom18.flashlock.data.remote.FirebaseTopicWordDataSource;
import com.nhom18.flashlock.data.remote.FirebaseUserWordProgressDataSource;
import com.nhom18.flashlock.data.repository.FirebaseProfileRepository;
import com.nhom18.flashlock.data.repository.FirebaseTopicWordRepository;
import com.nhom18.flashlock.data.repository.FirebaseUserWordProgressRepository;
import com.nhom18.flashlock.data.repository.ProfileRepository;
import com.nhom18.flashlock.data.repository.TopicWordRepository;
import com.nhom18.flashlock.data.repository.UserWordProgressRepository;
import com.nhom18.flashlock.domain.goal.StreakCalculator;
import com.nhom18.flashlock.domain.srs.SrsScheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class StudyViewModel extends ViewModel {
    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    private final TopicWordRepository topicRepository;
    private final UserWordProgressRepository progressRepository;
    private final ProfileRepository profileRepository;
    private final SrsScheduler scheduler;
    private final StreakCalculator streakCalculator = new StreakCalculator();
    // Chỉ đánh giá mục tiêu khi phiên có trả lời ít nhất 1 thẻ.
    private boolean hasAnswered = false;

    private final MutableLiveData<StudyUiState> state =
            new MutableLiveData<>(new StudyUiState(false, null, null, 0, 0, 0, 0, 0));

    private List<StudyCard> queue = new ArrayList<>();
    private List<Word> allWords = new ArrayList<>();
    private int currentIndex = 0;
    private Map<String, UserWordProgress> progressByWord = new HashMap<>();
    private String topicId;

    private boolean isCramMode = false;
    // Tăng mỗi lần bắt đầu session (loadTopic/cram) để bỏ qua callback của session cũ.
    private int sessionToken = 0;

    public StudyViewModel() {
        this(new FirebaseTopicWordRepository(new FirebaseTopicWordDataSource()),
                new FirebaseUserWordProgressRepository(new FirebaseUserWordProgressDataSource()),
                new FirebaseProfileRepository(new FirebaseProfileDataSource()),
                new SrsScheduler());
    }

    StudyViewModel(TopicWordRepository topicRepository,
                   UserWordProgressRepository progressRepository,
                   ProfileRepository profileRepository,
                   SrsScheduler scheduler) {
        this.topicRepository = topicRepository;
        this.progressRepository = progressRepository;
        this.profileRepository = profileRepository;
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
        final int token = ++sessionToken;

        Task<List<Word>> wordsTask = topicRepository.getTopicWords(topicId);
        Task<List<UserWordProgress>> progressTask = progressRepository.getProgressByTopic(topicId);

        Tasks.whenAllComplete(wordsTask, progressTask)
                .addOnCompleteListener(DIRECT_EXECUTOR, task -> {
                    if (token != sessionToken) {
                        return;
                    }
                    // Fail cứng nếu progress lỗi: coi là rỗng sẽ khiến mọi từ thành NEW
                    // và lần trả lời kế tiếp ghi đè/reset tiến độ SRS thật.
                    if (!wordsTask.isSuccessful() || !progressTask.isSuccessful()) {
                        state.postValue(new StudyUiState(false, "LOAD_STUDY_FAILED", null, 0, 0, 0, 0, 0));
                        return;
                    }
                    List<Word> words = wordsTask.getResult() != null ? wordsTask.getResult() : new ArrayList<>();
                    List<UserWordProgress> progress = progressTask.getResult() != null
                            ? progressTask.getResult() : new ArrayList<>();
                    buildSession(words, progress);
                });
    }

    // Chế độ "Học từ chưa nhớ": gom các từ đã học mà lần trả lời cuối là Chưa nhớ
    // (status REVIEW & boxLevel == 1), bỏ qua điều kiện đến hạn nextReviewAt.
    // Có lưu tiến độ (như học thường) nên Nhớ 1 lần sẽ đưa từ ra khỏi bộ này.
    public void loadWeakWords(String topicId) {
        isCramMode = false;
        if (topicId == null || topicId.trim().isEmpty()) {
            state.setValue(new StudyUiState(false, "TOPIC_ID_REQUIRED", null, 0, 0, 0, 0, 0));
            return;
        }
        this.topicId = topicId;
        state.setValue(new StudyUiState(true, null, null, 0, 0, 0, 0, 0));
        final int token = ++sessionToken;

        Task<List<Word>> wordsTask = topicRepository.getTopicWords(topicId);
        Task<List<UserWordProgress>> progressTask = progressRepository.getProgressByTopic(topicId);

        Tasks.whenAllComplete(wordsTask, progressTask)
                .addOnCompleteListener(DIRECT_EXECUTOR, task -> {
                    if (token != sessionToken) {
                        return;
                    }
                    if (!wordsTask.isSuccessful() || !progressTask.isSuccessful()) {
                        state.postValue(new StudyUiState(false, "LOAD_STUDY_FAILED", null, 0, 0, 0, 0, 0));
                        return;
                    }
                    List<Word> words = wordsTask.getResult() != null ? wordsTask.getResult() : new ArrayList<>();
                    List<UserWordProgress> progress = progressTask.getResult() != null
                            ? progressTask.getResult() : new ArrayList<>();
                    buildWeakSession(words, progress);
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
        final int token = ++sessionToken;
        topicRepository.getTopicWords(topicId)
                .addOnCompleteListener(DIRECT_EXECUTOR, task -> {
                    if (token != sessionToken) {
                        return;
                    }
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
        hasAnswered = true;
        UserWordProgress previous = progressByWord.get(progress.getWordId());
        progressByWord.put(progress.getWordId(), progress);
        moveNext();
        persistProgress(progress, previous, true);
    }

    // Ghi nhận hoàn thành mục tiêu ngay trong luồng học (gọi khi rời màn),
    // để streak không phụ thuộc việc người dùng có mở lại màn Home hay không.
    public void recordDailyGoalIfMet() {
        if (!hasAnswered || profileRepository == null) {
            return;
        }
        Task<UserProfile> profileTask = profileRepository.getCurrentUserProfile();
        Task<Integer> countTask = progressRepository.getStudiedWordsCountToday();
        Tasks.whenAllComplete(profileTask, countTask).addOnCompleteListener(DIRECT_EXECUTOR, t -> {
            if (!profileTask.isSuccessful() || profileTask.getResult() == null) return;
            if (!countTask.isSuccessful() || countTask.getResult() == null) return;

            UserProfile profile = profileTask.getResult();
            int goal = profile.getSettings() != null ? profile.getSettings().getDailyGoal() : 0;
            boolean goalMet = goal > 0 && countTask.getResult() >= goal;

            StreakCalculator.Result result = streakCalculator.evaluate(
                    profile.getCurrentStreak(),
                    profile.getLastGoalCompletedDate(),
                    goalMet,
                    new Date());
            if (result.needsUpdate) {
                profileRepository.updateUserStreak(result.streak, result.lastCompletedDate);
            }
        });
    }

    private void persistProgress(UserWordProgress progress, UserWordProgress previous, boolean allowRetry) {
        progressRepository.upsertProgress(progress).addOnCompleteListener(DIRECT_EXECUTOR, task -> {
            if (task.isSuccessful()) {
                return;
            }
            if (allowRetry) {
                persistProgress(progress, previous, false);
                return;
            }
            // Thất bại dứt điểm: hoàn tác entry cục bộ để bộ đếm không lệch với server,
            // rồi báo lỗi cho người dùng.
            if (previous != null) {
                progressByWord.put(progress.getWordId(), previous);
            } else {
                progressByWord.remove(progress.getWordId());
            }
            state.postValue(new StudyUiState(false, "SAVE_PROGRESS_FAILED",
                    getCurrentCard(), Math.min(currentIndex + 1, queue.size()), queue.size(),
                    getNewCount(), getLearningCount(), getMasteredCount()));
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

    private void buildWeakSession(List<Word> words, List<UserWordProgress> progress) {
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
        if (words != null) {
            for (Word word : words) {
                if (word == null) continue;
                UserWordProgress existing = progressByWord.get(word.getWordId());
                // "Chưa nhớ" = đã học (REVIEW) và lần cuối bấm Chưa nhớ (boxLevel == 1).
                // Từ mới (status NEW) bị loại; từ đã Nhớ có boxLevel >= 2 nên cũng bị loại.
                if (existing != null
                        && Word.STATUS_REVIEW.equals(existing.getStatus())
                        && existing.getBoxLevel() == 1) {
                    queue.add(new StudyCard(word, existing));
                }
            }
        }
        Collections.shuffle(queue);
        currentIndex = 0;
        if (queue.isEmpty()) {
            state.postValue(new StudyUiState(false, "NO_WEAK_WORDS", null, 0, 0,
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
        if (word.getStatus() != null && !word.getStatus().trim().isEmpty()) {
            return word.getStatus();
        }
        return Word.STATUS_NEW;
    }
}
