package com.nhom18.flashlock.data.model;

import com.google.firebase.Timestamp;

public class UserWordProgress {
    private String userId;
    private String wordId;
    private String topicId;
    private String status;
    private int boxLevel;
    private Timestamp nextReviewAt;
    private Timestamp updatedAt;

    public UserWordProgress() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getWordId() { return wordId; }
    public void setWordId(String wordId) { this.wordId = wordId; }

    public String getTopicId() { return topicId; }
    public void setTopicId(String topicId) { this.topicId = topicId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getBoxLevel() { return boxLevel; }
    public void setBoxLevel(int boxLevel) { this.boxLevel = boxLevel; }

    public Timestamp getNextReviewAt() { return nextReviewAt; }
    public void setNextReviewAt(Timestamp nextReviewAt) { this.nextReviewAt = nextReviewAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}

