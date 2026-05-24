package com.nhom18.flashlock.data.remote;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.nhom18.flashlock.data.model.Topic;
import java.util.ArrayList;
import java.util.List;

public class FirebaseTopicDataSource {
    private final FirebaseFirestore db;

    public FirebaseTopicDataSource() {
        this.db = FirebaseFirestore.getInstance();
    }

    public Task<List<Topic>> getAllTopics() {
        return db.collection("topics")
                .orderBy("createdAt", Query.Direction.DESCENDING)
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

    public ListenerRegistration observeTopics(TopicListListener listener) {
        return db.collection("topics")
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
                    List<Topic> topics = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Topic topic = doc.toObject(Topic.class);
                        if (topic != null && (topic.getTopicId() == null || topic.getTopicId().isEmpty())) {
                            topic.setTopicId(doc.getId());
                        }
                        if (topic != null) {
                            topics.add(topic);
                        }
                    }
                    listener.onChanged(topics);
                });
    }

    public interface TopicListListener {
        void onChanged(List<Topic> topics);
        void onError(Exception error);
    }
}

