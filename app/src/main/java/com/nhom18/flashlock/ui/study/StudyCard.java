package com.nhom18.flashlock.ui.study;

import com.nhom18.flashlock.data.model.UserWordProgress;
import com.nhom18.flashlock.data.model.Word;

public class StudyCard {
    private final Word word;
    private final UserWordProgress progress;

    public StudyCard(Word word, UserWordProgress progress) {
        this.word = word;
        this.progress = progress;
    }

    public Word getWord() { return word; }

    public UserWordProgress getProgress() { return progress; }
}

