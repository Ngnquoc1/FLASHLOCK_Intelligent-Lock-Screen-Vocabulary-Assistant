package com.nhom18.flashlock.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.nhom18.flashlock.data.model.Topic;
import com.nhom18.flashlock.data.model.TopicProgress;
import com.nhom18.flashlock.data.model.UserProfile;
import com.nhom18.flashlock.data.model.Word;
import com.nhom18.flashlock.data.remote.FirebaseProfileDataSource;
import com.nhom18.flashlock.data.remote.FirebaseSavedTopicDataSource;
import com.nhom18.flashlock.data.remote.FirebaseTopicProgressDataSource;
import com.nhom18.flashlock.data.remote.FirebaseTopicWordDataSource;
import com.nhom18.flashlock.data.remote.FirebaseUserWordProgressDataSource;
import com.nhom18.flashlock.data.remote.FirebaseWordDataSource;
import com.nhom18.flashlock.data.repository.FirebaseProfileRepository;
import com.nhom18.flashlock.data.repository.FirebaseSavedTopicRepository;
import com.nhom18.flashlock.data.repository.FirebaseTopicProgressRepository;
import com.nhom18.flashlock.data.repository.FirebaseTopicWordRepository;
import com.nhom18.flashlock.data.repository.FirebaseUserWordProgressRepository;
import com.nhom18.flashlock.data.repository.FirebaseWordRepository;
import com.nhom18.flashlock.data.repository.ProfileRepository;
import com.nhom18.flashlock.data.repository.SavedTopicRepository;
import com.nhom18.flashlock.data.repository.TopicProgressRepository;
import com.nhom18.flashlock.data.repository.TopicWordRepository;
import com.nhom18.flashlock.data.repository.UserWordProgressRepository;
import com.nhom18.flashlock.data.repository.WordRepository;
import com.nhom18.flashlock.domain.goal.StreakCalculator;
import com.nhom18.flashlock.domain.word.WordOfTheDayPicker;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeDashboardViewModel extends ViewModel {
    private final ProfileRepository profileRepository;
    private final UserWordProgressRepository userWordProgressRepository;
    private final TopicProgressRepository topicProgressRepository;
    private final TopicWordRepository topicWordRepository;
    private final SavedTopicRepository savedTopicRepository;
    private final WordRepository wordRepository;
    private final StreakCalculator streakCalculator = new StreakCalculator();
    private final WordOfTheDayPicker wordOfTheDayPicker = new WordOfTheDayPicker();

    private List<Word> cachedMyWords = new ArrayList<>();

    private final MutableLiveData<Integer> dailyCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> dailyGoal = new MutableLiveData<>(5);
    private final MutableLiveData<Integer> progressPercentage = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> streakCount = new MutableLiveData<>(0);
    private final MutableLiveData<TopicProgress> latestTopicProgress = new MutableLiveData<>();
    private final MutableLiveData<Word> wordOfTheDay = new MutableLiveData<>();
    private final MutableLiveData<Boolean> wordInMyWords = new MutableLiveData<>(false);
    private final MutableLiveData<String> infoMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public HomeDashboardViewModel() {
        this.profileRepository = new FirebaseProfileRepository(new FirebaseProfileDataSource());
        this.userWordProgressRepository = new FirebaseUserWordProgressRepository(new FirebaseUserWordProgressDataSource());
        this.topicProgressRepository = new FirebaseTopicProgressRepository(new FirebaseTopicProgressDataSource());
        this.topicWordRepository = new FirebaseTopicWordRepository(new FirebaseTopicWordDataSource());
        this.savedTopicRepository = new FirebaseSavedTopicRepository(new FirebaseSavedTopicDataSource());
        this.wordRepository = new FirebaseWordRepository(new FirebaseWordDataSource());
    }

    public LiveData<Integer> getDailyCount() { return dailyCount; }
    public LiveData<Integer> getDailyGoal() { return dailyGoal; }
    public LiveData<Integer> getProgressPercentage() { return progressPercentage; }
    public LiveData<Integer> getStreakCount() { return streakCount; }
    public LiveData<TopicProgress> getLatestTopicProgress() { return latestTopicProgress; }
    public LiveData<Word> getWordOfTheDay() { return wordOfTheDay; }
    public LiveData<Boolean> getWordInMyWords() { return wordInMyWords; }
    public LiveData<String> getInfoMessage() { return infoMessage; }
    public void clearInfoMessage() { infoMessage.setValue(null); }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    public void loadDashboardData() {
        loading.setValue(true);

        Task<UserProfile> profileTask = profileRepository.getCurrentUserProfile();
        Task<Integer> countTodayTask = userWordProgressRepository.getStudiedWordsCountToday();
        Task<TopicProgress> latestTopicTask = topicProgressRepository.getLatestTopicProgress();
        Task<List<Word>> myWordsTask = wordRepository.getAllWords();

        Tasks.whenAllComplete(profileTask, countTodayTask, latestTopicTask, myWordsTask)
                .addOnCompleteListener(task -> {
                    cachedMyWords = myWordsTask.isSuccessful() && myWordsTask.getResult() != null
                            ? myWordsTask.getResult() : new ArrayList<>();
                    loading.postValue(false);

                    int currentGoal = 5;
                    int currentCount = 0;
                    UserProfile profile = null;

                    if (profileTask.isSuccessful() && profileTask.getResult() != null) {
                        profile = profileTask.getResult();
                        if (profile.getSettings() != null) {
                            currentGoal = profile.getSettings().getDailyGoal();
                        }
                    }
                    dailyGoal.postValue(currentGoal);

                    if (countTodayTask.isSuccessful() && countTodayTask.getResult() != null) {
                        currentCount = countTodayTask.getResult();
                    }
                    dailyCount.postValue(currentCount);

                    if (currentGoal > 0) {
                        int percentage = (currentCount * 100) / currentGoal;
                        progressPercentage.postValue(Math.min(percentage, 100));
                    }

                    if (latestTopicTask.isSuccessful()) {
                        latestTopicProgress.postValue(latestTopicTask.getResult());
                    }

                    // Word of the Day: chọn deterministic từ cùng pool với lock screen,
                    // để Home và lock screen luôn đồng bộ trong ngày.
                    if (profile != null) {
                        loadWordOfTheDay(profile);
                    }

                    checkAndUpdateStreak(profile, currentCount, currentGoal);

                    if (!profileTask.isSuccessful() || !countTodayTask.isSuccessful()) {
                        error.postValue("Failed to load some dashboard data");
                    }
                });
    }

    private void loadWordOfTheDay(UserProfile profile) {
        String uid = profile.getUid();
        List<String> topicIds = profile.getSettings() != null && profile.getSettings().getLockScreenTopicIds() != null
                ? new ArrayList<>(profile.getSettings().getLockScreenTopicIds())
                : new ArrayList<>();

        if (!topicIds.isEmpty()) {
            fetchAndPickWordOfTheDay(uid, topicIds);
            return;
        }
        // Pool rỗng (= "Tất cả"): My_words + mọi saved topic
        savedTopicRepository.getSavedTopics().addOnCompleteListener(task -> {
            List<String> ids = new ArrayList<>();
            ids.add(Topic.MY_WORDS_TOPIC_ID);
            if (task.isSuccessful() && task.getResult() != null) {
                for (Topic t : task.getResult()) {
                    if (t.getTopicId() != null && !ids.contains(t.getTopicId())) {
                        ids.add(t.getTopicId());
                    }
                }
            }
            fetchAndPickWordOfTheDay(uid, ids);
        });
    }

    private void fetchAndPickWordOfTheDay(String uid, List<String> topicIds) {
        List<Task<List<Word>>> tasks = new ArrayList<>();
        for (String id : topicIds) {
            if (id != null && !id.trim().isEmpty()) {
                tasks.add(topicWordRepository.getTopicWords(id));
            }
        }
        if (tasks.isEmpty()) {
            wordOfTheDay.postValue(null);
            return;
        }
        Tasks.whenAllComplete(tasks).addOnCompleteListener(done -> {
            List<Word> pool = new ArrayList<>();
            for (Task<List<Word>> t : tasks) {
                if (t.isSuccessful() && t.getResult() != null) {
                    pool.addAll(t.getResult());
                }
            }
            Word picked = wordOfTheDayPicker.pick(uid, new Date(), pool);
            wordOfTheDay.postValue(picked);
            wordInMyWords.postValue(isInMyWords(picked));
        });
    }

    private boolean isInMyWords(Word word) {
        if (word == null || word.getTerm() == null) return false;
        String target = word.getTerm().trim().toLowerCase(Locale.US);
        if (target.isEmpty()) return false;
        for (Word w : cachedMyWords) {
            if (w == null || w.getTerm() == null) continue;
            if (target.equals(w.getTerm().trim().toLowerCase(Locale.US))) {
                return true;
            }
        }
        return false;
    }

    public void addWordOfTheDayToMyWords() {
        Word wod = wordOfTheDay.getValue();
        if (wod == null) return;
        if (Boolean.TRUE.equals(wordInMyWords.getValue())) return;

        Word copy = new Word();
        copy.setTerm(wod.getTerm());
        copy.setDefinition(wod.getDefinition());
        copy.setPronunciation(wod.getPronunciation());
        copy.setExample(wod.getExample());
        copy.setWordType(wod.getWordType());
        copy.setStatus(Word.STATUS_NEW);
        copy.setNextReviewAt(com.google.firebase.Timestamp.now());

        wordRepository.addWord(copy).addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                cachedMyWords.add(copy);
                wordInMyWords.postValue(true);
                infoMessage.postValue("ADDED_TO_MY_WORDS");
            } else {
                infoMessage.postValue("ADD_TO_MY_WORDS_FAILED");
            }
        });
    }

    private void checkAndUpdateStreak(UserProfile profile, int currentCount, int currentGoal) {
        if (profile == null) {
            streakCount.postValue(0);
            return;
        }

        boolean isGoalMetToday = currentGoal > 0 && currentCount >= currentGoal;
        StreakCalculator.Result result = streakCalculator.evaluate(
                profile.getCurrentStreak(),
                profile.getLastGoalCompletedDate(),
                isGoalMetToday,
                new Date());

        streakCount.postValue(result.streak);
        if (result.needsUpdate) {
            profileRepository.updateUserStreak(result.streak, result.lastCompletedDate);
        }
    }
}