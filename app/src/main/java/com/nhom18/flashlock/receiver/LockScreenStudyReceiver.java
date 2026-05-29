package com.nhom18.flashlock.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.nhom18.flashlock.service.LockScreenStudyService;

public class LockScreenStudyReceiver extends BroadcastReceiver {
    private static final String TAG = "LockScreenStudyReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();
        Log.d(TAG, "Sự kiện nhận được: " + action);
        if (!Intent.ACTION_USER_PRESENT.equals(action)) {
            return;
        }

        Intent serviceIntent = new Intent(context, LockScreenStudyService.class);
        serviceIntent.setAction(LockScreenStudyService.ACTION_REFRESH_WORDS);
        try {
            ContextCompat.startForegroundService(context, serviceIntent);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khởi động Service: " + e.getMessage());
        }
    }
}
