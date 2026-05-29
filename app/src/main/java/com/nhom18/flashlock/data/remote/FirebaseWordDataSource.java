package com.nhom18.flashlock.data.remote;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.nhom18.flashlock.data.model.Word;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Random; // Đã thêm import cho hàm Random

public class FirebaseWordDataSource {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public FirebaseWordDataSource() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public String getCurrentUid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    public Task<String> addWord(String uid, Word word) {
        if (uid == null) {
            return Tasks.forException(new Exception("User not logged in"));
        }

        DocumentReference docRef = db.collection("users")
                .document(uid)
                .collection("my_words")
                .document();

        String wordId = docRef.getId();
        Map<String, Object> payload = toFirestoreMap(word, wordId, true);

        return docRef.set(payload).continueWith(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return wordId;
        });
    }

    public Task<Void> updateWord(String uid, Word word) {
        if (uid == null) {
            return Tasks.forException(new Exception("User not logged in"));
        }
        if (word == null || word.getWordId() == null || word.getWordId().trim().isEmpty()) {
            return Tasks.forException(new Exception("wordId is required"));
        }

        Map<String, Object> payload = toFirestoreMap(word, word.getWordId(), false);
        if (payload.isEmpty()) {
            return Tasks.forResult(null);
        }

        return db.collection("users")
                .document(uid)
                .collection("my_words")
                .document(word.getWordId())
                .update(payload);
    }

    public Task<Void> deleteWord(String uid, String wordId) {
        if (uid == null) {
            return Tasks.forException(new Exception("User not logged in"));
        }
        if (wordId == null || wordId.trim().isEmpty()) {
            return Tasks.forException(new Exception("wordId is required"));
        }

        return db.collection("users")
                .document(uid)
                .collection("my_words")
                .document(wordId)
                .delete();
    }

    public Task<List<Word>> getAllWords(String uid) {
        if (uid == null) {
            return Tasks.forException(new Exception("User not logged in"));
        }

        return db.collection("users")
                .document(uid)
                .collection("my_words")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    List<Word> words = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        Word word = doc.toObject(Word.class);
                        if (word != null && (word.getWordId() == null || word.getWordId().isEmpty())) {
                            word.setWordId(doc.getId());
                        }
                        if (word != null) {
                            words.add(word);
                        }
                    }
                    return words;
                });
    }

    public Task<List<Word>> getDueWords(String uid, Timestamp currentTime) {
        if (uid == null) {
            return Tasks.forException(new Exception("User not logged in"));
        }
        if (currentTime == null) {
            return Tasks.forException(new Exception("currentTime is required"));
        }

        return db.collection("users")
                .document(uid)
                .collection("my_words")
                .whereLessThanOrEqualTo("nextReviewAt", currentTime)
                .orderBy("nextReviewAt", Query.Direction.ASCENDING)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    List<Word> words = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        Word word = doc.toObject(Word.class);
                        if (word == null) {
                            continue;
                        }
                        if (word.getWordId() == null || word.getWordId().isEmpty()) {
                            word.setWordId(doc.getId());
                        }
                        if (!Word.STATUS_MASTERED.equals(word.getStatus())) {
                            words.add(word);
                        }
                    }
                    return words;
                });
    }

    public Task<List<Word>> searchWordsByTerm(String uid, String query) {
        if (uid == null) {
            return Tasks.forException(new Exception("User not logged in"));
        }
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.US);
        if (normalized.isEmpty()) {
            return getAllWords(uid);
        }

        return db.collection("users")
                .document(uid)
                .collection("my_words")
                .orderBy("termLower")
                .startAt(normalized)
                .endAt(normalized + "\uf8ff")
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    List<Word> words = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        Word word = doc.toObject(Word.class);
                        if (word != null && (word.getWordId() == null || word.getWordId().isEmpty())) {
                            word.setWordId(doc.getId());
                        }
                        if (word != null) {
                            words.add(word);
                        }
                    }
                    if (!words.isEmpty()) {
                        return Tasks.forResult(words);
                    }
                    return getAllWords(uid).continueWith(filterTask -> {
                        if (!filterTask.isSuccessful()) {
                            throw filterTask.getException();
                        }
                        return filterWordsByQuery(filterTask.getResult(), normalized);
                    });
                });
    }

    public Task<Word> getRandomWordOfDay(String uid) {
        if (uid == null) {
            return Tasks.forException(new Exception("User not logged in"));
        }
        return db.collection("users")
                .document(uid)
                .collection("my_words")
                .whereNotEqualTo("status", Word.STATUS_MASTERED)
                .limit(20)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        List<DocumentSnapshot> docs = task.getResult().getDocuments();

                        int randomIndex = new Random().nextInt(docs.size());
                        DocumentSnapshot randomDoc = docs.get(randomIndex);

                        Word word = randomDoc.toObject(Word.class);
                        if (word != null && (word.getWordId() == null || word.getWordId().isEmpty())) {
                            word.setWordId(randomDoc.getId());
                        }
                        return word;
                    }
                    return null;
                });
    }

    public ListenerRegistration observeAllWords(String uid, WordListListener listener) {
        if (uid == null) {
            listener.onError(new Exception("User not logged in"));
            return null;
        }

        return db.collection("users")
                .document(uid)
                .collection("my_words")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    if (snapshot == null) {
                        listener.onChanged(new ArrayList<>());
                        return;
                    }
                    List<Word> words = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Word word = doc.toObject(Word.class);
                        if (word != null && (word.getWordId() == null || word.getWordId().isEmpty())) {
                            word.setWordId(doc.getId());
                        }
                        if (word != null) {
                            words.add(word);
                        }
                    }
                    listener.onChanged(words);
                });
    }

    public ListenerRegistration observeDueWords(String uid, Timestamp currentTime, WordListListener listener) {
        if (uid == null) {
            listener.onError(new Exception("User not logged in"));
            return null;
        }
        if (currentTime == null) {
            listener.onError(new Exception("currentTime is required"));
            return null;
        }

        return db.collection("users")
                .document(uid)
                .collection("my_words")
                .whereLessThanOrEqualTo("nextReviewAt", currentTime)
                .orderBy("nextReviewAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    if (snapshot == null) {
                        listener.onChanged(new ArrayList<>());
                        return;
                    }
                    List<Word> words = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Word word = doc.toObject(Word.class);
                        if (word == null) {
                            continue;
                        }
                        if (word.getWordId() == null || word.getWordId().isEmpty()) {
                            word.setWordId(doc.getId());
                        }
                        if (!Word.STATUS_MASTERED.equals(word.getStatus())) {
                            words.add(word);
                        }
                    }
                    listener.onChanged(words);
                });
    }

    private Map<String, Object> toFirestoreMap(Word word, String wordId, boolean isCreate) {
        Map<String, Object> payload = new HashMap<>();
        if (wordId != null) {
            payload.put("wordId", wordId);
        }
        if (word != null) {
            if (word.getTerm() != null) {
                payload.put("term", word.getTerm());
                payload.put("termLower", word.getTerm().toLowerCase(Locale.US));
            }
            if (word.getDefinition() != null) payload.put("definition", word.getDefinition());
            if (word.getStatus() != null) {
                payload.put("status", word.getStatus());
            } else if (isCreate) {
                payload.put("status", Word.STATUS_NEW);
            }
            if (word.getNextReviewAt() != null) payload.put("nextReviewAt", word.getNextReviewAt());
            if (word.getCreatedAt() != null) {
                payload.put("createdAt", word.getCreatedAt());
            } else if (isCreate) {
                payload.put("createdAt", FieldValue.serverTimestamp());
            }
            if (word.getExample() != null) payload.put("example", word.getExample());
            if (word.getPronunciation() != null) payload.put("pronunciation", word.getPronunciation());
            if (word.getAudioUrl() != null) payload.put("audioUrl", word.getAudioUrl());
            if (word.getTopicId() != null) payload.put("topicId", word.getTopicId());
            if (word.getWordType() != null) payload.put("wordType", word.getWordType());
        }
        return payload;
    }

    private List<Word> filterWordsByQuery(List<Word> source, String normalizedQuery) {
        List<Word> filtered = new ArrayList<>();
        if (source == null || normalizedQuery == null || normalizedQuery.isEmpty()) {
            return filtered;
        }
        for (Word word : source) {
            if (word == null) {
                continue;
            }
            String term = word.getTerm() == null ? "" : word.getTerm().trim().toLowerCase(Locale.US);
            String definition = word.getDefinition() == null ? "" : word.getDefinition().trim().toLowerCase(Locale.US);
            if (term.contains(normalizedQuery) || definition.contains(normalizedQuery)) {
                filtered.add(word);
            }
        }
        return filtered;
    }

    public interface WordListListener {
        void onChanged(List<Word> words);
        void onError(Exception error);
    }
}