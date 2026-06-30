package com.nhom18.flashlock;

import android.app.Application;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

/**
 * Application class. FirebaseApp tự init qua ContentProvider, Crashlytics tự bật
 * khi dep được include. Ở đây chỉ bật/tắt collection theo build type và set custom keys.
 */
public class FlashLockApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Chỉ thu thập crash trong build release để debug builds không làm nhiễu data.
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG);
    }
}
