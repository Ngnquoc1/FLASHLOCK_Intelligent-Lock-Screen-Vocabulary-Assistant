package com.nhom18.flashlock.data.remote;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.nhom18.flashlock.data.model.UserWordProgress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseUserWordProgressDataSource {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public FirebaseUserWordProgressDataSource() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public String getCurrentUid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    public Task<List<UserWordProgress>> getProgressByTopic(String uid, String topicId) {
        if (uid == null) {
            return Tasks.forException(new Exception("User not logged in"));
        }
        if (topicId == null || topicId.trim().isEmpty()) {
            return Tasks.forException(new Exception("topicId is required"));
        }

        return db.collection("users")
                .document(uid)
                .collection("word_progress")
                .whereEqualTo("topicId", topicId)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    List<UserWordProgress> items = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        UserWordProgress progress = doc.toObject(UserWordProgress.class);
                        if (progress != null && (progress.getWordId() == null || progress.getWordId().isEmpty())) {
                            progress.setWordId(doc.getId());
                        }
                        if (progress != null) {
                            items.add(progress);
                        }
                    }
                    return items;
                });
    }

    public Task<List<UserWordProgress>> getDueWords(String uid, Timestamp now) {
        if (uid == null) {
            return Tasks.forException(new Exception("User not logged in"));
        }
        if (now == null) {
            return Tasks.forException(new Exception("currentTime is required"));
        }

        Task<List<UserWordProgress>> newTask = db.collection("users")
                .document(uid)
                .collection("word_progress")
                .whereEqualTo("status", "NEW")
                .get()
                .continueWith(task -> mapSnapshots(task));

        Task<List<UserWordProgress>> dueTask = db.collection("users")
                .document(uid)
                .collection("word_progress")
                .whereLessThanOrEqualTo("nextReviewAt", now)
                .orderBy("nextReviewAt", Query.Direction.ASCENDING)
                .get()
                .continueWith(task -> mapSnapshots(task));

        return Tasks.whenAllSuccess(newTask, dueTask)
                .continueWith(task -> {
                    List<UserWordProgress> merged = new ArrayList<>();
                    for (Object item : task.getResult()) {
                        if (item instanceof List) {
                            for (Object progress : (List<?>) item) {
                                if (progress instanceof UserWordProgress) {
                                    merged.add((UserWordProgress) progress);
                                }
                            }
                        }
                    }
                    return merged;
                });
    }

    public Task<Void> upsertProgress(String uid, UserWordProgress progress) {
        if (uid == null) {
            return Tasks.forException(new Exception("User not logged in"));
        }
        if (progress == null || progress.getWordId() == null || progress.getWordId().trim().isEmpty()) {
            return Tasks.forException(new Exception("wordId is required"));
        }

        Map<String, Object> payload = toFirestoreMap(uid, progress);
        return db.collection("users")
                .document(uid)
                .collection("word_progress")
                .document(progress.getWordId())
                .set(payload);
    }

    public Task<Integer> getStudiedWordsCountToday(String uid) {
        if (uid == null) {
            return Tasks.forException(new Exception("User not logged in"));
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Timestamp startOfDay = new Timestamp(calendar.getTime());

        return db.collection("users")
                .document(uid)
                .collection("word_progress")
                .whereGreaterThanOrEqualTo("updatedAt", startOfDay)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return task.getResult().size();
                });
    }

    private Map<String, Object> toFirestoreMap(String uid, UserWordProgress progress) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", uid);
        payload.put("wordId", progress.getWordId());
        if (progress.getTopicId() != null) {
            payload.put("topicId", progress.getTopicId());
        }
        if (progress.getStatus() != null) {
            payload.put("status", progress.getStatus());
        }
        payload.put("boxLevel", progress.getBoxLevel());
        if (progress.getNextReviewAt() != null) {
            payload.put("nextReviewAt", progress.getNextReviewAt());
        }
        if (progress.getUpdatedAt() != null) {
            payload.put("updatedAt", progress.getUpdatedAt());
        } else {
            payload.put("updatedAt", Timestamp.now());
        }
        return payload;
    }

    private List<UserWordProgress> mapSnapshots(com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task) throws Exception {
        if (!task.isSuccessful()) {
            throw task.getException();
        }
        List<UserWordProgress> items = new ArrayList<>();
        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
            UserWordProgress progress = doc.toObject(UserWordProgress.class);
            if (progress != null && (progress.getWordId() == null || progress.getWordId().isEmpty())) {
                progress.setWordId(doc.getId());
            }
            if (progress != null) {
                items.add(progress);
            }
        }
        return items;
    }
}