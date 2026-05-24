package com.nhom18.flashlock.data.repository;

import com.google.android.gms.tasks.Task;
import com.nhom18.flashlock.data.model.Topic;
import java.util.List;

public interface SavedTopicRepository {
    Task<List<Topic>> getSavedTopics();
    Task<Void> saveTopic(Topic topic);
}