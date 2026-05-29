package com.nhom18.flashlock.data.model;

import com.google.firebase.Timestamp;

public class Topic {
    private String topicId;
    private String documentId;
    private String title;
    private String category;
    private String thumbnailUrl;
    private long wordCount;
    private Timestamp createdAt;
    private String language;

    public Topic() {}

    public String getTopicId() { return topicId; }
    public void setTopicId(String topicId) { this.topicId = topicId; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public long getWordCount() { return wordCount; }
    public void setWordCount(long wordCount) { this.wordCount = wordCount; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public static final String MY_WORDS_TOPIC_ID = "topic_my_words_default";
}