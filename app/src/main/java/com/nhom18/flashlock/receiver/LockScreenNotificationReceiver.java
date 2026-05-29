package com.nhom18.flashlock.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.nhom18.flashlock.service.LockScreenStudyService;

public class LockScreenNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "LockScreenNotifReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        if (!LockScreenStudyService.ACTION_NEXT_WORD.equals(action)
                && !LockScreenStudyService.ACTION_PREV_WORD.equals(action)) {
            return;
        }

        Intent serviceIntent = new Intent(context, LockScreenStudyService.class)
                .setAction(action);
        try {
            ContextCompat.startForegroundService(context, serviceIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to forward action to service: " + e.getMessage());
        }
    }
}
