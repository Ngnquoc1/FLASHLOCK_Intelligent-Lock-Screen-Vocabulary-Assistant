package com.nhom18.flashlock.data.repository;

import com.google.android.gms.tasks.Task;

public interface StudyRepository {
    Task<Void> logStudyEvent(String wordId, String eventType, String sessionId);
}
