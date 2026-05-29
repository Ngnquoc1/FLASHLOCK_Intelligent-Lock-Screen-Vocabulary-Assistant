package com.nhom18.flashlock.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.nhom18.flashlock.service.LockScreenStudyService;

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

        Intent serviceIntent = new Intent(context, LockScreenStudyService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (Exception e) {
            // Android 12+ có thể ném ForegroundServiceStartNotAllowedException
            // khi app chưa được user mở lần đầu sau boot. Sẽ tự khởi động khi user mở app.
            Log.w(TAG, "Cannot start FGS at boot: " + e.getMessage());
        }
    }
}
