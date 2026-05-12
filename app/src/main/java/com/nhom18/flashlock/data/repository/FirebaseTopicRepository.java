package com.nhom18.flashlock.data.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.ListenerRegistration;
import com.nhom18.flashlock.data.model.Topic;
import com.nhom18.flashlock.data.remote.FirebaseTopicDataSource;
import java.util.List;

public class FirebaseTopicRepository implements TopicRepository {
    private final FirebaseTopicDataSource dataSource;

    public FirebaseTopicRepository(FirebaseTopicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Task<List<Topic>> getAllTopics() {
        return dataSource.getAllTopics();
    }

    @Override
    public ListenerRegistration observeTopics(TopicListListener listener) {
        return dataSource.observeTopics(toDataSourceListener(listener));
    }

    private FirebaseTopicDataSource.TopicListListener toDataSourceListener(TopicListListener listener) {
        return new FirebaseTopicDataSource.TopicListListener() {
            @Override
            public void onChanged(List<Topic> topics) {
                listener.onChanged(topics);
            }

            @Override
            public void onError(Exception error) {
                listener.onError(error);
            }
        };
    }
}

