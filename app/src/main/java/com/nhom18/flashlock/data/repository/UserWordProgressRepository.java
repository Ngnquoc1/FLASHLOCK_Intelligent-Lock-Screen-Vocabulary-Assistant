package com.nhom18.flashlock.data.repository;

import com.google.android.gms.tasks.Task;
import com.nhom18.flashlock.data.model.UserWordProgress;
import java.util.List;

public interface UserWordProgressRepository {
    Task<List<UserWordProgress>> getProgressByTopic(String topicId);
    Task<Void> upsertProgress(UserWordProgress progress);
}

