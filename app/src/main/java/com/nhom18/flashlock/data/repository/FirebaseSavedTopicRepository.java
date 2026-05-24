package com.nhom18.flashlock.data.repository;

import com.google.android.gms.tasks.Task;
import com.nhom18.flashlock.data.model.Topic;
import com.nhom18.flashlock.data.remote.FirebaseSavedTopicDataSource;
import java.util.List;

public class FirebaseSavedTopicRepository implements SavedTopicRepository {
    private final FirebaseSavedTopicDataSource dataSource;

    public FirebaseSavedTopicRepository(FirebaseSavedTopicDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Task<List<Topic>> getSavedTopics() {
        String uid = dataSource.getCurrentUid();
        return dataSource.getSavedTopics(uid);
    }
}

