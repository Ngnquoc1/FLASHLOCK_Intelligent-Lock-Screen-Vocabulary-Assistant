package com.nhom18.flashlock.data.repository;

import com.google.android.gms.tasks.Task;
import com.nhom18.flashlock.data.model.StudyEvent;
import com.nhom18.flashlock.data.remote.FirebaseStudyEventDataSource;

public class FirebaseStudyRepository implements StudyRepository {

    private final FirebaseStudyEventDataSource dataSource;

    public FirebaseStudyRepository(FirebaseStudyEventDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Task<Void> logStudyEvent(String wordId, String eventType, String sessionId) {
        StudyEvent event = new StudyEvent();
        event.setWordId(wordId);
        event.setEventType(eventType);
        event.setSessionId(sessionId);
        return dataSource.logStudyEvent(dataSource.getCurrentUid(), event);
    }
}
