package com.nhom18.flashlock.ui.study;

public class StudyUiState {
    private final boolean loading;
    private final String error;
    private final StudyCard currentCard;
    private final int index;
    private final int total;
    private final int newCount;
    private final int learningCount;
    private final int masteredCount;

    public StudyUiState(boolean loading,
                        String error,
                        StudyCard currentCard,
                        int index,
                        int total,
                        int newCount,
                        int learningCount,
                        int masteredCount) {
        this.loading = loading;
        this.error = error;
        this.currentCard = currentCard;
        this.index = index;
        this.total = total;
        this.newCount = newCount;
        this.learningCount = learningCount;
        this.masteredCount = masteredCount;
    }

    public boolean isLoading() { return loading; }
    public String getError() { return error; }
    public StudyCard getCurrentCard() { return currentCard; }
    public int getIndex() { return index; }
    public int getTotal() { return total; }
    public int getNewCount() { return newCount; }
    public int getLearningCount() { return learningCount; }
    public int getMasteredCount() { return masteredCount; }
}

