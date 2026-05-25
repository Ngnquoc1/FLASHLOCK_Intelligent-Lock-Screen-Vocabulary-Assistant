package com.nhom18.flashlock.data.repository;

import com.google.android.gms.tasks.Task;
import com.nhom18.flashlock.data.model.Word;

import java.util.List;

public interface TopicWordRepository {
    Task<List<Word>> getTopicWords(String topicId);
}

