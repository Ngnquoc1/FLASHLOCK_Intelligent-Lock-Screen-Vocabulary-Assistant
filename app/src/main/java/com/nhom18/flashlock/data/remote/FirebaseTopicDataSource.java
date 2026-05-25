package com.nhom18.flashlock.data.remote;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Source;
import com.nhom18.flashlock.data.model.Topic;
import java.util.ArrayList;
import java.util.List;

public class FirebaseTopicDataSource {
    private static final String TAG = "FirebaseTopicDataSource";
    private final FirebaseFirestore db;

    public FirebaseTopicDataSource() {
        this.db = FirebaseFirestore.getInstance();
    }

    public Task<List<Topic>> getAllTopics() {
        return db.collection("topics")
                .get(Source.SERVER)
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        android.util.Log.d(TAG, "getAllTopics: server count=" + task.getResult().size());
                        return Tasks.forResult(mapTopics(task.getResult().getDocuments()));
                    }
                    android.util.Log.w(TAG, "getAllTopics: server failed, fallback cache", task.getException());
                    return db.collection("topics")
                            .get(Source.CACHE)
                            .continueWith(cacheTask -> {
                                if (!cacheTask.isSuccessful()) {
                                    throw cacheTask.getException();
                                }
                                android.util.Log.d(TAG, "getAllTopics: cache count=" + cacheTask.getResult().size());
                                return mapTopics(cacheTask.getResult().getDocuments());
                            });
                });
    }

    public ListenerRegistration observeTopics(TopicListListener listener) {
        return db.collection("topics")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        android.util.Log.w(TAG, "observeTopics: error", error);
                        listener.onError(error);
                        return;
                    }
                    if (snapshot == null) {
                        android.util.Log.d(TAG, "observeTopics: empty snapshot");
                        listener.onChanged(new ArrayList<>());
                        return;
                    }
                    android.util.Log.d(TAG, "observeTopics: count=" + snapshot.size());
                    listener.onChanged(mapTopics(snapshot.getDocuments()));
                });
    }

    private List<Topic> mapTopics(List<DocumentSnapshot> documents) {
        List<Topic> topics = new ArrayList<>();
        for (DocumentSnapshot doc : documents) {
            Topic topic = doc.toObject(Topic.class);
            if (topic != null) {
                topic.setDocumentId(doc.getId());
                if (topic.getTopicId() == null || topic.getTopicId().isEmpty()) {
                    topic.setTopicId(doc.getId());
                }
                topics.add(topic);
            }
        }
        return topics;
    }

    public interface TopicListListener {
        void onChanged(List<Topic> topics);
        void onError(Exception error);
    }
}
