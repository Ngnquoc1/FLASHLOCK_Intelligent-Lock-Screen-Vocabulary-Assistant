package com.nhom18.flashlock.ui.library;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.nhom18.flashlock.data.model.Topic;
import com.nhom18.flashlock.data.remote.FirebaseTopicDataSource;
import com.nhom18.flashlock.data.remote.FirebaseSavedTopicDataSource;
import com.nhom18.flashlock.data.repository.FirebaseTopicRepository;
import com.nhom18.flashlock.data.repository.FirebaseSavedTopicRepository;
import com.nhom18.flashlock.data.repository.SavedTopicRepository;
import com.nhom18.flashlock.data.repository.TopicRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class LibraryViewModel extends ViewModel {
    private final TopicRepository topicRepository;
    private final SavedTopicRepository savedTopicRepository;

    private final MutableLiveData<List<Topic>> topics = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Set<String>> savedTopicIds = new MutableLiveData<>(new HashSet<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>(null);
    private final MutableLiveData<String> message = new MutableLiveData<>(null);
    private final MutableLiveData<String> actionError = new MutableLiveData<>(null);
    private com.google.firebase.firestore.ListenerRegistration topicsListener;
    private boolean initialTopicsLoaded = false;

    private static final String TAG = "LibraryViewModel";

    public LibraryViewModel() {
        this(new FirebaseTopicRepository(new FirebaseTopicDataSource()),
                new FirebaseSavedTopicRepository(new FirebaseSavedTopicDataSource()));
    }

    LibraryViewModel(TopicRepository topicRepository, SavedTopicRepository savedTopicRepository) {
        this.topicRepository = topicRepository;
        this.savedTopicRepository = savedTopicRepository;
    }

    public LiveData<List<Topic>> getTopics() { return topics; }
    public LiveData<Set<String>> getSavedTopicIds() { return savedTopicIds; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<String> getMessage() { return message; }
    public LiveData<String> getActionError() { return actionError; }

    public void clearMessage() { message.setValue(null); }
    public void clearActionError() { actionError.setValue(null); }

    public void loadLibrary() {
        android.util.Log.d(TAG, "loadLibrary: start");
        loading.setValue(true);
        error.setValue(null);
        initialTopicsLoaded = false;
        if (topicsListener != null) {
            topicsListener.remove();
            topicsListener = null;
        }
        AtomicInteger pending = new AtomicInteger(2);
        observeTopicsInternal(pending);
        loadSavedTopicsInternal(pending);
    }

    private void observeTopicsInternal(AtomicInteger pending) {
        topicsListener = topicRepository.observeTopics(new TopicRepository.TopicListListener() {
            @Override
            public void onChanged(List<Topic> items) {
                android.util.Log.d(TAG, "observeTopics: received=" + (items != null ? items.size() : 0));
                topics.postValue(items != null ? items : new ArrayList<>());
                if (!initialTopicsLoaded) {
                    initialTopicsLoaded = true;
                    finishLoading(pending);
                }
            }

            @Override
            public void onError(Exception exception) {
                android.util.Log.w(TAG, "observeTopics: error", exception);
                error.postValue(exception != null ? exception.getMessage() : "LOAD_TOPICS_FAILED");
                if (!initialTopicsLoaded) {
                    initialTopicsLoaded = true;
                    finishLoading(pending);
                }
            }
        });
    }

    private void loadSavedTopicsInternal(AtomicInteger pending) {
        savedTopicRepository.getSavedTopics().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                android.util.Log.d(TAG, "loadSavedTopics: success count=" + (task.getResult() != null ? task.getResult().size() : 0));
                Set<String> ids = new HashSet<>();
                List<Topic> items = task.getResult();
                if (items != null) {
                    for (Topic topic : items) {
                        if (topic != null && topic.getTopicId() != null) {
                            ids.add(topic.getTopicId());
                        }
                    }
                }
                savedTopicIds.postValue(ids);
            } else {
                android.util.Log.w(TAG, "loadSavedTopics: failed", task.getException());
                // Khong lam gian doan viec hien thi danh sach chinh neu chi loi load saved topics
                actionError.postValue("LOAD_SAVED_IDS_FAILED");
            }
            finishLoading(pending);
        });
    }

    private void finishLoading(AtomicInteger pending) {
        if (pending.decrementAndGet() == 0) {
            loading.postValue(false);
        }
    }

    public void saveTopic(Topic topic) {
        if (topic == null || topic.getTopicId() == null) {
            actionError.setValue("LIBRARY_TOPIC_ID_REQUIRED");
            return;
        }
        loading.setValue(true);
        savedTopicRepository.saveTopic(topic).addOnCompleteListener(task -> {
            loading.postValue(false);
            if (task.isSuccessful()) {
                addSavedTopicId(topic.getTopicId());
                message.postValue("LIBRARY_TOPIC_SAVED");
            } else {
                actionError.postValue(task.getException() != null ? task.getException().getMessage() : "SAVE_TOPIC_FAILED");
            }
        });
    }

    private void addSavedTopicId(String topicId) {
        Set<String> current = savedTopicIds.getValue();
        Set<String> updated = current != null ? new HashSet<>(current) : new HashSet<>();
        updated.add(topicId);
        savedTopicIds.postValue(updated);
    }

    @Override
    protected void onCleared() {
        if (topicsListener != null) {
            topicsListener.remove();
            topicsListener = null;
        }
    }
}
