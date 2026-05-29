package com.nhom18.flashlock.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.nhom18.flashlock.data.model.TopicProgress;
import com.nhom18.flashlock.data.model.UserProfile;
import com.nhom18.flashlock.data.model.Word;
import com.nhom18.flashlock.data.remote.FirebaseProfileDataSource;
import com.nhom18.flashlock.data.remote.FirebaseTopicProgressDataSource;
import com.nhom18.flashlock.data.remote.FirebaseUserWordProgressDataSource;
import com.nhom18.flashlock.data.remote.FirebaseWordDataSource;
import com.nhom18.flashlock.data.repository.FirebaseProfileRepository;
import com.nhom18.flashlock.data.repository.FirebaseTopicProgressRepository;
import com.nhom18.flashlock.data.repository.FirebaseUserWordProgressRepository;
import com.nhom18.flashlock.data.repository.FirebaseWordRepository;
import com.nhom18.flashlock.data.repository.ProfileRepository;
import com.nhom18.flashlock.data.repository.TopicProgressRepository;
import com.nhom18.flashlock.data.repository.UserWordProgressRepository;
import com.nhom18.flashlock.data.repository.WordRepository;

public class HomeDashboardViewModel extends ViewModel {
    private final ProfileRepository profileRepository;
    private final UserWordProgressRepository userWordProgressRepository;
    private final TopicProgressRepository topicProgressRepository;
    private final WordRepository wordRepository;

    private final MutableLiveData<Integer> dailyCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> dailyGoal = new MutableLiveData<>(5);
    private final MutableLiveData<Integer> progressPercentage = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> streakCount = new MutableLiveData<>(0);
    private final MutableLiveData<TopicProgress> latestTopicProgress = new MutableLiveData<>();
    private final MutableLiveData<Word> wordOfTheDay = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public HomeDashboardViewModel() {
        this.profileRepository = new FirebaseProfileRepository(new FirebaseProfileDataSource());
        this.userWordProgressRepository = new FirebaseUserWordProgressRepository(new FirebaseUserWordProgressDataSource());
        this.topicProgressRepository = new FirebaseTopicProgressRepository(new FirebaseTopicProgressDataSource());
        this.wordRepository = new FirebaseWordRepository(new FirebaseWordDataSource());
    }

    public LiveData<Integer> getDailyCount() { return dailyCount; }
    public LiveData<Integer> getDailyGoal() { return dailyGoal; }
    public LiveData<Integer> getProgressPercentage() { return progressPercentage; }
    public LiveData<Integer> getStreakCount() { return streakCount; }
    public LiveData<TopicProgress> getLatestTopicProgress() { return latestTopicProgress; }
    public LiveData<Word> getWordOfTheDay() { return wordOfTheDay; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    public void loadDashboardData() {
        loading.setValue(true);

        Task<UserProfile> profileTask = profileRepository.getCurrentUserProfile();
        Task<Integer> countTodayTask = userWordProgressRepository.getStudiedWordsCountToday();
        Task<TopicProgress> latestTopicTask = topicProgressRepository.getLatestTopicProgress();
        Task<Word> wordOfDayTask = wordRepository.getRandomWordOfDay();

        Tasks.whenAllComplete(profileTask, countTodayTask, latestTopicTask, wordOfDayTask)
                .addOnCompleteListener(task -> {
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

                    if (wordOfDayTask.isSuccessful()) {
                        wordOfTheDay.postValue(wordOfDayTask.getResult());
                    }

                    checkAndUpdateStreak(profile, currentCount, currentGoal);

                    if (!profileTask.isSuccessful() || !countTodayTask.isSuccessful()) {
                        error.postValue("Failed to load some dashboard data");
                    }
                });
    }

    private void checkAndUpdateStreak(UserProfile profile, int currentCount, int currentGoal) {
        if (profile == null) {
            streakCount.postValue(0);
            return;
        }

        int streak = profile.getCurrentStreak();
        java.util.Date lastCompleted = profile.getLastGoalCompletedDate();
        java.util.Calendar today = java.util.Calendar.getInstance();
        clearTime(today);

        long daysDiff = -1;
        if (lastCompleted != null) {
            java.util.Calendar last = java.util.Calendar.getInstance();
            last.setTime(lastCompleted);
            clearTime(last);
            daysDiff = (today.getTimeInMillis() - last.getTimeInMillis()) / (24 * 60 * 60 * 1000);
        }

        boolean isGoalMetToday = currentCount >= currentGoal && currentGoal > 0;
        boolean needsDbUpdate = false;

        if (daysDiff == 0) {
            streakCount.postValue(streak);
        }
        else if (daysDiff == 1) {
            if (isGoalMetToday) {
                streak += 1;
                lastCompleted = new java.util.Date();
                needsDbUpdate = true;
            }
            streakCount.postValue(streak);
        }
        else {
            if (isGoalMetToday) {
                streak = 1;
                lastCompleted = new java.util.Date();
                needsDbUpdate = true;
            } else {
                if (streak > 0) {
                    streak = 0;
                    needsDbUpdate = true;
                }
            }
            streakCount.postValue(streak);
        }

        if (needsDbUpdate) {
            profileRepository.updateUserStreak(streak, lastCompleted);
        }
    }

    private void clearTime(java.util.Calendar c) {
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
    }
}