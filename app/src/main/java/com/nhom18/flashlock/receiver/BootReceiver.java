package com.nhom18.flashlock.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.nhom18.flashlock.service.LockScreenStudyService;
import com.nhom18.flashlock.utils.ReminderManager;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !"android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            return;
        }

        // 1. Khởi động Service màn hình khóa
        Intent serviceIntent = new Intent(context, LockScreenStudyService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.w(TAG, "Cannot start FGS at boot: " + e.getMessage());
        }

        // 2. Khôi phục báo thức nhắc nhở học tập (Reminder)
        SharedPreferences prefs = context.getSharedPreferences("FlashLockPrefs", Context.MODE_PRIVATE);
        boolean isEnabled = prefs.getBoolean("dailyReminderEnabled", false);
        if (isEnabled) {
            int hour = prefs.getInt("reminderHour", 20);
            int minute = prefs.getInt("reminderMinute", 30);
            ReminderManager.setDailyReminder(context, hour, minute);
            Log.d(TAG, "Restored daily reminder at " + hour + ":" + minute);
        }
    }
}
