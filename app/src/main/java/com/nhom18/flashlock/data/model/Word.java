package com.nhom18.flashlock.data.model;

import com.google.firebase.Timestamp;

public class Word {
    public static final String STATUS_NEW = "NEW";
    public static final String STATUS_LEARNING = "LEARNING";
    public static final String STATUS_MASTERED = "MASTERED";

    private String wordId;
    private String term;
    private String definition;
    private String status;
    private Timestamp nextReviewAt;
    private Timestamp createdAt;

    private String example;
    private String pronunciation;
    private String audioUrl;
    private String topicId;

    public Word() {}

    public String getWordId() { return wordId; }
    public void setWordId(String wordId) { this.wordId = wordId; }

    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }

    public String getDefinition() { return definition; }
    public void setDefinition(String definition) { this.definition = definition; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getNextReviewAt() { return nextReviewAt; }
    public void setNextReviewAt(Timestamp nextReviewAt) { this.nextReviewAt = nextReviewAt; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getExample() { return example; }
    public void setExample(String example) { this.example = example; }

    public String getPronunciation() { return pronunciation; }
    public void setPronunciation(String pronunciation) { this.pronunciation = pronunciation; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public String getTopicId() { return topicId; }
    public void setTopicId(String topicId) { this.topicId = topicId; }
}

