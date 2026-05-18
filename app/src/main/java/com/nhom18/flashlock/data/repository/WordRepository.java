package com.nhom18.flashlock.data.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;
import com.nhom18.flashlock.data.model.Word;
import java.util.List;

public interface WordRepository {
    Task<String> addWord(Word word);
    Task<Void> updateWord(Word word);
    Task<Void> deleteWord(String wordId);

    Task<List<Word>> getAllWords();
    Task<List<Word>> getDueWords(Timestamp currentTime);
    Task<List<Word>> searchWordsByTerm(String query);

    ListenerRegistration observeAllWords(WordListListener listener);
    ListenerRegistration observeDueWords(Timestamp currentTime, WordListListener listener);

    interface WordListListener {
        void onChanged(List<Word> words);
        void onError(Exception error);
    }
}
