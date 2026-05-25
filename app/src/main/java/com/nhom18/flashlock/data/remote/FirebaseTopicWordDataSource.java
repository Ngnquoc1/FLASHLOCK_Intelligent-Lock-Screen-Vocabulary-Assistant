package com.nhom18.flashlock.data.remote;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.nhom18.flashlock.data.model.Word;

import java.util.ArrayList;
import java.util.List;

public class FirebaseTopicWordDataSource {
    private final FirebaseFirestore db;

    public FirebaseTopicWordDataSource() {
        this.db = FirebaseFirestore.getInstance();
    }

    public Task<List<Word>> getTopicWords(String topicId) {
        if (topicId == null || topicId.trim().isEmpty()) {
            return Tasks.forException(new Exception("topicId is required"));
        }
        return db.collection("topics")
                .document(topicId)
                .collection("words")
                .orderBy("term", Query.Direction.ASCENDING)
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
}

