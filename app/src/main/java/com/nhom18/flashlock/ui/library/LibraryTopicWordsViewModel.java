package com.nhom18.flashlock.ui.library;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.nhom18.flashlock.data.model.Word;
import com.nhom18.flashlock.data.remote.FirebaseTopicWordDataSource;
import com.nhom18.flashlock.data.repository.FirebaseTopicWordRepository;
import com.nhom18.flashlock.data.repository.TopicWordRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class LibraryTopicWordsViewModel extends ViewModel {
    private static final Executor DIRECT_EXECUTOR = Runnable::run;
    private final TopicWordRepository repository;

    private final MutableLiveData<List<Word>> words = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>(null);

    public LibraryTopicWordsViewModel() {
        this(new FirebaseTopicWordRepository(new FirebaseTopicWordDataSource()));
    }

    LibraryTopicWordsViewModel(TopicWordRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<Word>> getWords() {
        return words;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadTopicWords(String topicId) {
        if (topicId == null || topicId.trim().isEmpty()) {
            error.setValue("TOPIC_ID_REQUIRED");
            return;
        }
        loading.setValue(true);
        repository.getTopicWords(topicId).addOnCompleteListener(DIRECT_EXECUTOR, task -> {
            loading.postValue(false);
            if (task.isSuccessful()) {
                List<Word> result = task.getResult();
                words.postValue(result != null ? result : new ArrayList<>());
            } else {
                error.postValue(task.getException() != null ? task.getException().getMessage() : "LOAD_TOPIC_WORDS_FAILED");
            }
        });
    }
}



