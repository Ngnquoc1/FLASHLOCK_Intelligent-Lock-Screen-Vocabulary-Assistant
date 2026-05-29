package com.nhom18.flashlock.data.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;
import com.nhom18.flashlock.data.model.Word;
import com.nhom18.flashlock.data.remote.FirebaseWordDataSource;
import java.util.List;

public class FirebaseWordRepository implements WordRepository {
    private final FirebaseWordDataSource dataSource;

    public FirebaseWordRepository(FirebaseWordDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Task<String> addWord(Word word) {
        String uid = dataSource.getCurrentUid();
        return dataSource.addWord(uid, word);
    }

    @Override
    public Task<Void> updateWord(Word word) {
        String uid = dataSource.getCurrentUid();
        return dataSource.updateWord(uid, word);
    }

    @Override
    public Task<Void> deleteWord(String wordId) {
        String uid = dataSource.getCurrentUid();
        return dataSource.deleteWord(uid, wordId);
    }

    @Override
    public Task<List<Word>> getAllWords() {
        String uid = dataSource.getCurrentUid();
        return dataSource.getAllWords(uid);
    }

    @Override
    public Task<List<Word>> getDueWords(Timestamp currentTime) {
        String uid = dataSource.getCurrentUid();
        return dataSource.getDueWords(uid, currentTime);
    }

    @Override
    public Task<List<Word>> searchWordsByTerm(String query) {
        String uid = dataSource.getCurrentUid();
        return dataSource.searchWordsByTerm(uid, query);
    }

    @Override
    public Task<Word> getRandomWordOfDay() {
        String uid = dataSource.getCurrentUid();
        return dataSource.getRandomWordOfDay(uid);
    }

    @Override
    public ListenerRegistration observeAllWords(WordListListener listener) {
        String uid = dataSource.getCurrentUid();
        return dataSource.observeAllWords(uid, toDataSourceListener(listener));
    }

    @Override
    public ListenerRegistration observeDueWords(Timestamp currentTime, WordListListener listener) {
        String uid = dataSource.getCurrentUid();
        return dataSource.observeDueWords(uid, currentTime, toDataSourceListener(listener));
    }

    private FirebaseWordDataSource.WordListListener toDataSourceListener(WordListListener listener) {
        return new FirebaseWordDataSource.WordListListener() {
            @Override
            public void onChanged(List<Word> words) {
                listener.onChanged(words);
            }

            @Override
            public void onError(Exception error) {
                listener.onError(error);
            }
        };
    }
}