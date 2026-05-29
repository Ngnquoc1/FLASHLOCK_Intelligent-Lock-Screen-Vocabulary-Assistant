package com.nhom18.flashlock.data.repository;

import com.google.android.gms.tasks.Task;
import com.nhom18.flashlock.data.model.Topic; // Import ĐÚNG class Topic của dự án
import com.nhom18.flashlock.data.model.Word;
import com.nhom18.flashlock.data.remote.FirebaseTopicWordDataSource;
import com.nhom18.flashlock.data.remote.FirebaseWordDataSource;

import java.util.List;

public class FirebaseTopicWordRepository implements TopicWordRepository {
    private final FirebaseTopicWordDataSource dataSource;

    public FirebaseTopicWordRepository(FirebaseTopicWordDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Task<List<Word>> getTopicWords(String topicId) {
        if (Topic.MY_WORDS_TOPIC_ID.equals(topicId)) {
            return new FirebaseWordRepository(new FirebaseWordDataSource()).getAllWords();
        }
        return dataSource.getTopicWords(topicId);
    }
}