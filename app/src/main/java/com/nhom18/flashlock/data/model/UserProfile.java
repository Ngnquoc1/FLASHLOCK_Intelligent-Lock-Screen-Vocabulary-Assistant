package com.nhom18.flashlock.data.model;

import com.google.firebase.firestore.PropertyName;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UserProfile {
    private String uid;
    private String email;
    private String displayName;
    private String avatarUrl;
    private String avatarPath;
    private String provider;
    private String status;
    private Date createdAt;
    private Date updatedAt;
    private Settings settings;

    public UserProfile() {}

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public Settings getSettings() { return settings; }
    public void setSettings(Settings settings) { this.settings = settings; }

    public static class Settings {
        private boolean lockScreenEnabled = true;
        private int reminderHour = 20;
        private int reminderMinute = 30;

        public Settings() {}

        @PropertyName("lockScreenEnabled")
        public boolean isLockScreenEnabled() { return lockScreenEnabled; }
        @PropertyName("lockScreenEnabled")
        public void setLockScreenEnabled(boolean lockScreenEnabled) { this.lockScreenEnabled = lockScreenEnabled; }

        public int getReminderHour() { return reminderHour; }
        public void setReminderHour(int reminderHour) { this.reminderHour = reminderHour; }

        public int getReminderMinute() { return reminderMinute; }
        public void setReminderMinute(int reminderMinute) { this.reminderMinute = reminderMinute; }
    }
}
