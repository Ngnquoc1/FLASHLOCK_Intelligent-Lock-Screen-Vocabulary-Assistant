package com.nhom18.flashlock.data.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.ListenerRegistration;
import com.nhom18.flashlock.data.model.Topic;
import java.util.List;

public interface TopicRepository {
    Task<List<Topic>> getAllTopics();
    ListenerRegistration observeTopics(TopicListListener listener);

    interface TopicListListener {
        void onChanged(List<Topic> topics);
        void onError(Exception error);
    }
}

