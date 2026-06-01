package com.nhom18.flashlock.ui.home;

import android.os.Handler;
import android.os.Looper;

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
import com.nhom18.flashlock.data.repository.DictionaryRepository;
import com.nhom18.flashlock.data.remote.dictionary.SuggestionResponse;
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

    private final DictionaryRepository dictionaryRepository;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private final StreakCalculator streakCalculator = new StreakCalculator();
    private final WordOfTheDayPicker wordOfTheDayPicker = new WordOfTheDayPicker();

    private List<Word> cachedMyWords = new ArrayList<>();
    private List<Topic> cachedSavedTopics = new ArrayList<>();

    private long lastLoadAt = 0L;
    private static final long LOAD_TTL_MS = 60_000L;

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

    private final MutableLiveData<List<SearchSuggestionAdapter.SearchItem>> searchResults = new MutableLiveData<>();

    public HomeDashboardViewModel() {
        this.profileRepository = new FirebaseProfileRepository(new FirebaseProfileDataSource());
        this.userWordProgressRepository = new FirebaseUserWordProgressRepository(new FirebaseUserWordProgressDataSource());
        this.topicProgressRepository = new FirebaseTopicProgressRepository(new FirebaseTopicProgressDataSource());
        this.topicWordRepository = new FirebaseTopicWordRepository(new FirebaseTopicWordDataSource());
        this.savedTopicRepository = new FirebaseSavedTopicRepository(new FirebaseSavedTopicDataSource());
        this.wordRepository = new FirebaseWordRepository(new FirebaseWordDataSource());
        this.dictionaryRepository = new DictionaryRepository();
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
    public void clearError() { error.setValue(null); }
    public LiveData<List<SearchSuggestionAdapter.SearchItem>> getSearchResults() { return searchResults; }

    public void loadDashboardData() {
        if (System.currentTimeMillis() - lastLoadAt < LOAD_TTL_MS && wordOfTheDay.getValue() != null) return;
        lastLoadAt = System.currentTimeMillis();
        loading.setValue(true);

        Task<UserProfile> profileTask = profileRepository.getCurrentUserProfile();
        Task<Integer> countTodayTask = userWordProgressRepository.getStudiedWordsCountToday();
        Task<TopicProgress> latestTopicTask = topicProgressRepository.getLatestTopicProgress();
        Task<List<Word>> myWordsTask = wordRepository.getAllWords();
        Task<List<Topic>> savedTopicsTask = savedTopicRepository.getSavedTopics();

        Tasks.whenAllComplete(profileTask, countTodayTask, latestTopicTask, myWordsTask, savedTopicsTask)
                .addOnCompleteListener(task -> {
                    cachedMyWords = myWordsTask.isSuccessful() && myWordsTask.getResult() != null ? myWordsTask.getResult() : new ArrayList<>();
                    cachedSavedTopics = savedTopicsTask.isSuccessful() && savedTopicsTask.getResult() != null ? savedTopicsTask.getResult() : new ArrayList<>();
                    loading.postValue(false);

                    int currentGoal = 5;
                    int currentCount = 0;
                    UserProfile profile = null;

                    if (profileTask.isSuccessful() && profileTask.getResult() != null) {
                        profile = profileTask.getResult();
                        streakCount.postValue(profile.getCurrentStreak());
                        if (profile.getSettings() != null) currentGoal = profile.getSettings().getDailyGoal();
                    }
                    dailyGoal.postValue(currentGoal);
                    if (countTodayTask.isSuccessful() && countTodayTask.getResult() != null) currentCount = countTodayTask.getResult();
                    dailyCount.postValue(currentCount);

                    if (currentGoal > 0) progressPercentage.postValue(Math.min((currentCount * 100) / currentGoal, 100));
                    if (latestTopicTask.isSuccessful()) latestTopicProgress.postValue(latestTopicTask.getResult());
                    if (profile != null) loadWordOfTheDay(profile);
                    checkAndUpdateStreak(profile, currentCount, currentGoal);
                });
    }

    public void performSearch(String query) {
        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);

        if (query == null || query.trim().isEmpty()) {
            searchResults.postValue(new ArrayList<>());
            return;
        }

        String lowerQuery = query.trim().toLowerCase(Locale.US);

        searchRunnable = () -> {
            dictionaryRepository.getSuggestions(lowerQuery, 5, new DictionaryRepository.SuggestionCallback() {
                @Override
                public void onSuccess(List<SuggestionResponse> suggestions) {
                    List<SearchSuggestionAdapter.SearchItem> results = new ArrayList<>();
                    if (suggestions != null) {
                        for (SuggestionResponse s : suggestions) {
                            String definition = "";
                            if (s.defs != null && !s.defs.isEmpty()) {
                                definition = s.defs.get(0);
                                if (definition.contains("\t")) {
                                    definition = definition.substring(definition.indexOf("\t") + 1);
                                }
                            }

                            results.add(new SearchSuggestionAdapter.SearchItem(
                                    s.word,
                                    s.word,
                                    definition,
                                    SearchSuggestionAdapter.SearchItem.TYPE_API
                            ));
                        }
                    }
                    searchResults.postValue(results);
                }

                @Override
                public void onError(String error) {
                    searchResults.postValue(new ArrayList<>());
                }
            });
        };
        searchHandler.postDelayed(searchRunnable, 300);
    }

    public void addCustomWordToMyWords(Word word) {
        if (word == null) return;

        if (isInMyWords(word)) {
            error.postValue("Word already exists!");
            return;
        }

        loading.setValue(true);
        wordRepository.addWord(word).addOnCompleteListener(task -> {
            loading.postValue(false);
            if (task.isSuccessful()) {
                cachedMyWords.add(word);
                infoMessage.postValue("ADDED_TO_MY_WORDS");
            } else {
                error.postValue("Failed to add word to collection");
            }
        });
    }

    private void loadWordOfTheDay(UserProfile profile) {
        String uid = profile.getUid();
        List<String> topicIds = profile.getSettings() != null && profile.getSettings().getLockScreenTopicIds() != null
                ? new ArrayList<>(profile.getSettings().getLockScreenTopicIds()) : new ArrayList<>();

        if (!topicIds.isEmpty()) {
            fetchAndPickWordOfTheDay(uid, topicIds);
            return;
        }

        List<String> ids = new ArrayList<>();
        ids.add(Topic.MY_WORDS_TOPIC_ID);
        for (Topic t : cachedSavedTopics) {
            if (t.getTopicId() != null && !ids.contains(t.getTopicId())) ids.add(t.getTopicId());
        }
        fetchAndPickWordOfTheDay(uid, ids);
    }

    private void fetchAndPickWordOfTheDay(String uid, List<String> topicIds) {
        List<Task<List<Word>>> tasks = new ArrayList<>();
        for (String id : topicIds) {
            if (id != null && !id.trim().isEmpty()) tasks.add(topicWordRepository.getTopicWords(id));
        }
        if (tasks.isEmpty()) {
            wordOfTheDay.postValue(null);
            return;
        }
        Tasks.whenAllComplete(tasks).addOnCompleteListener(done -> {
            List<Word> pool = new ArrayList<>();
            for (Task<List<Word>> t : tasks) {
                if (t.isSuccessful() && t.getResult() != null) pool.addAll(t.getResult());
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
            if (w != null && w.getTerm() != null && target.equals(w.getTerm().trim().toLowerCase(Locale.US))) return true;
        }
        return false;
    }

    public void addWordOfTheDayToMyWords() {
        Word wod = wordOfTheDay.getValue();
        if (wod == null || Boolean.TRUE.equals(wordInMyWords.getValue())) return;
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
            } else infoMessage.postValue("ADD_TO_MY_WORDS_FAILED");
        });
    }

    private void checkAndUpdateStreak(UserProfile profile, int currentCount, int currentGoal) {
        if (profile == null) {
            streakCount.postValue(0);
            return;
        }
        boolean isGoalMetToday = currentGoal > 0 && currentCount >= currentGoal;
        StreakCalculator.Result result = streakCalculator.evaluate(profile.getCurrentStreak(), profile.getLastGoalCompletedDate(), isGoalMetToday, new Date());
        streakCount.postValue(result.streak);
        if (result.needsUpdate) profileRepository.updateUserStreak(result.streak, result.lastCompletedDate);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
    }
}