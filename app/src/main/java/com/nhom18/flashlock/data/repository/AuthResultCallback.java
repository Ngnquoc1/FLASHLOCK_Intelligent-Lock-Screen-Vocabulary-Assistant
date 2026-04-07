package com.nhom18.flashlock.data.repository;

public interface AuthResultCallback {
    void onSuccess();

    void onError(String message);
}
