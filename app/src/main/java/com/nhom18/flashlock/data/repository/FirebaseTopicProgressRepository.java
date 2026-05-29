package com.nhom18.flashlock.data.repository;

import com.google.android.gms.tasks.Task;
import com.nhom18.flashlock.data.model.TopicProgress;
import com.nhom18.flashlock.data.remote.FirebaseTopicProgressDataSource;
import java.util.List;

public class FirebaseTopicProgressRepository implements TopicProgressRepository {
    private final FirebaseTopicProgressDataSource dataSource;

    public FirebaseTopicProgressRepository(FirebaseTopicProgressDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Task<List<TopicProgress>> getProgress() {
        String uid = dataSource.getCurrentUid();
        return dataSource.getProgress(uid);
    }

    @Override
    public Task<Void> saveProgress(TopicProgress progress) {
        String uid = dataSource.getCurrentUid();
        return dataSource.saveProgress(uid, progress);
    }

    @Override
    public Task<TopicProgress> getLatestTopicProgress() {
        String uid = dataSource.getCurrentUid();
        return dataSource.getLatestTopicProgress(uid);
    }
}