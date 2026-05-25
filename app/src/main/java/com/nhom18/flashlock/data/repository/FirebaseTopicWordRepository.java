package com.nhom18.flashlock.data.repository;

import com.google.android.gms.tasks.Task;
import com.nhom18.flashlock.data.model.Word;
import com.nhom18.flashlock.data.remote.FirebaseTopicWordDataSource;

import java.util.List;

public class FirebaseTopicWordRepository implements TopicWordRepository {
    private final FirebaseTopicWordDataSource dataSource;

    public FirebaseTopicWordRepository(FirebaseTopicWordDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Task<List<Word>> getTopicWords(String topicId) {
        return dataSource.getTopicWords(topicId);
    }
}

