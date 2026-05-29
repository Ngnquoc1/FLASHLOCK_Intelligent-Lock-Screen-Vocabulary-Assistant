package com.nhom18.flashlock.data.remote;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nhom18.flashlock.data.model.StudyEvent;

import java.util.HashMap;
import java.util.Map;

public class FirebaseStudyEventDataSource {

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public FirebaseStudyEventDataSource() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public String getCurrentUid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    public Task<Void> logStudyEvent(String uid, StudyEvent event) {
        if (uid == null) {
            return Tasks.forException(new Exception("User not logged in"));
        }
        if (event == null || event.getWordId() == null || event.getWordId().trim().isEmpty()) {
            return Tasks.forException(new Exception("wordId is required"));
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("wordId", event.getWordId());
        payload.put("eventType", event.getEventType());
        payload.put("sessionId", event.getSessionId());
        payload.put("createdAt", FieldValue.serverTimestamp());

        return db.collection("users")
                .document(uid)
                .collection("study_events")
                .document()
                .set(payload);
    }
}