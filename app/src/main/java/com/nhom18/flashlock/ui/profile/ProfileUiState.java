package com.nhom18.flashlock.ui.profile;

import com.nhom18.flashlock.data.model.UserProfile;

public class ProfileUiState {
    public enum Status { IDLE, LOADING, CONTENT, SAVING, SUCCESS, ERROR }

    private final Status status;
    private final UserProfile userProfile;
    private final String errorMessage;

    private ProfileUiState(Status status, UserProfile userProfile, String errorMessage) {
        this.status = status;
        this.userProfile = userProfile;
        this.errorMessage = errorMessage;
    }

    public static ProfileUiState idle() { return new ProfileUiState(Status.IDLE, null, null); }
    public static ProfileUiState loading() { return new ProfileUiState(Status.LOADING, null, null); }
    public static ProfileUiState content(UserProfile profile) { return new ProfileUiState(Status.CONTENT, profile, null); }
    public static ProfileUiState saving(UserProfile profile) { return new ProfileUiState(Status.SAVING, profile, null); }
    public static ProfileUiState success(UserProfile profile) { return new ProfileUiState(Status.SUCCESS, profile, null); }
    public static ProfileUiState error(String message) { return new ProfileUiState(Status.ERROR, null, message); }

    public Status getStatus() { return status; }
    public UserProfile getUserProfile() { return userProfile; }
    public String getErrorMessage() { return errorMessage; }
}
