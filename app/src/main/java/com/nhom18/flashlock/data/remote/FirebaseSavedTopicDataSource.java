package com.nhom18.flashlock.data.remote;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nhom18.flashlock.data.model.Topic;
import java.util.ArrayList;
import java.util.List;

public class FirebaseSavedTopicDataSource {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public FirebaseSavedTopicDataSource() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public String getCurrentUid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    public Task<List<Topic>> getSavedTopics(String uid) {
        if (uid == null) {
            return Tasks.forException(new Exception("User not logged in"));
        }
        return db.collection("users")
                .document(uid)
                .collection("saved_topics")
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    List<Topic> topics = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        Topic topic = doc.toObject(Topic.class);
                        if (topic != null && (topic.getTopicId() == null || topic.getTopicId().isEmpty())) {
                            topic.setTopicId(doc.getId());
                        }
                        if (topic != null) {
                            topics.add(topic);
                        }
                    }
                    return topics;
                });
    }
}

