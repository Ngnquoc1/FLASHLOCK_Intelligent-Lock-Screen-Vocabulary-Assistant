package com.nhom18.flashlock.data.repository;

import com.google.android.gms.tasks.Task;
import com.nhom18.flashlock.data.model.TopicProgress;
import java.util.List;

public interface TopicProgressRepository {
    Task<List<TopicProgress>> getProgress();
    Task<Void> saveProgress(TopicProgress progress);

    Task<TopicProgress> getLatestTopicProgress();
}