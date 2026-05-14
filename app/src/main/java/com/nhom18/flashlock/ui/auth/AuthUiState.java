package com.nhom18.flashlock.ui.auth;

public class AuthUiState {

    public enum Status {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR
    }

    private final Status status;
    private final String message;

    private AuthUiState(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public static AuthUiState idle() {
        return new AuthUiState(Status.IDLE, "");
    }

    public static AuthUiState loading() {
        return new AuthUiState(Status.LOADING, "");
    }

    public static AuthUiState success() {
        return new AuthUiState(Status.SUCCESS, "");
    }

    public static AuthUiState error(String message) {
        return new AuthUiState(Status.ERROR, message == null ? "AUTH_UNKNOWN_ERROR" : message);
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}

