package com.nhom18.flashlock.data.model;

import com.google.firebase.Timestamp;

public class TopicProgress {
    private String topicId;
    private int totalWords;
    private int learnedWords;
    private Timestamp lastStudiedAt;

    public TopicProgress() {}

    public String getTopicId() { return topicId; }
    public void setTopicId(String topicId) { this.topicId = topicId; }

    public int getTotalWords() { return totalWords; }
    public void setTotalWords(int totalWords) { this.totalWords = totalWords; }

    public int getLearnedWords() { return learnedWords; }
    public void setLearnedWords(int learnedWords) { this.learnedWords = learnedWords; }

    public Timestamp getLastStudiedAt() { return lastStudiedAt; }
    public void setLastStudiedAt(Timestamp lastStudiedAt) { this.lastStudiedAt = lastStudiedAt; }
}
