package com.nhom18.flashlock.data.remote;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.nhom18.flashlock.data.model.TopicProgress;
import java.util.ArrayList;
import java.util.List;

public class FirebaseTopicProgressDataSource {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public FirebaseTopicProgressDataSource() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public String getCurrentUid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    public Task<List<TopicProgress>> getProgress(String uid) {
        if (uid == null) {
            return Tasks.forException(new Exception("User not logged in"));
        }
        return db.collection("users")
                .document(uid)
                .collection("topic_progress")
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    List<TopicProgress> progressList = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        TopicProgress progress = doc.toObject(TopicProgress.class);
                        if (progress != null && (progress.getTopicId() == null || progress.getTopicId().isEmpty())) {
                            progress.setTopicId(doc.getId());
                        }
                        if (progress != null) {
                            progressList.add(progress);
                        }
                    }
                    return progressList;
                });
    }

    public Task<Void> saveProgress(String uid, TopicProgress progress) {
        if (uid == null) {
            return Tasks.forException(new Exception("User not logged in"));
        }
        if (progress == null || progress.getTopicId() == null || progress.getTopicId().trim().isEmpty()) {
            return Tasks.forException(new Exception("topicId is required"));
        }
        return db.collection("users")
                .document(uid)
                .collection("topic_progress")
                .document(progress.getTopicId())
                .set(progress);
    }

    public Task<TopicProgress> getLatestTopicProgress(String uid) {
        if (uid == null) {
            return Tasks.forException(new Exception("User not logged in"));
        }
        return db.collection("users")
                .document(uid)
                .collection("topic_progress")
                .orderBy("lastStudiedAt", Query.Direction.DESCENDING) // Sắp xếp mới nhất lên đầu
                .limit(1) // Chỉ lấy 1 cái
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        return task.getResult().getDocuments().get(0).toObject(TopicProgress.class);
                    }
                    return null;
                });
    }
}