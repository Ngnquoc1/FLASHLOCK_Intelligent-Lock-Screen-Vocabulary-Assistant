package com.nhom18.flashlock.data.repository;

import com.google.android.gms.tasks.Task;
import com.nhom18.flashlock.data.model.UserWordProgress;
import com.nhom18.flashlock.data.remote.FirebaseUserWordProgressDataSource;
import java.util.List;

public class FirebaseUserWordProgressRepository implements UserWordProgressRepository {
    private final FirebaseUserWordProgressDataSource dataSource;

    public FirebaseUserWordProgressRepository(FirebaseUserWordProgressDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Task<List<UserWordProgress>> getProgressByTopic(String topicId) {
        String uid = dataSource.getCurrentUid();
        return dataSource.getProgressByTopic(uid, topicId);
    }

    @Override
    public Task<Void> upsertProgress(UserWordProgress progress) {
        String uid = dataSource.getCurrentUid();
        return dataSource.upsertProgress(uid, progress);
    }
}

