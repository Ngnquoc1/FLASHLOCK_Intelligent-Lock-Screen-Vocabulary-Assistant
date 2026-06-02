package com.nhom18.flashlock.utils;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/**
 * Tập hợp kiểm tra/khởi tạo các quyền cần cho tính năng lock-screen + reminder.
 * Mỗi method là pure-ish: nhận Context/Activity, không giữ state.
 */
public final class PermissionsHelper {
    public static final int REQ_POST_NOTIFICATIONS = 1101;

    private PermissionsHelper() {}

    // --- 1. POST_NOTIFICATIONS (Android 13+) ---

    public static boolean hasNotificationPermission(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQ_POST_NOTIFICATIONS);
        }
    }

    // --- 2. Notifications globally enabled cho app + channel không bị mute ---

    public static boolean areAppNotificationsEnabled(Context ctx) {
        return NotificationManagerCompat.from(ctx).areNotificationsEnabled();
    }

    public static boolean isChannelEnabled(Context ctx, String channelId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true;
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm == null) return false;
        NotificationChannel ch = nm.getNotificationChannel(channelId);
        // Chưa tạo channel cũng coi là chưa enabled — sẽ enable khi service start.
        return ch != null && ch.getImportance() != NotificationManager.IMPORTANCE_NONE;
    }

    /** Mở Settings notification của app (Android 8+) hoặc của channel cụ thể. */
    public static Intent appNotificationSettings(Context ctx, String channelId) {
        Intent i = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (channelId != null) {
                i.setAction(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                i.putExtra(Settings.EXTRA_CHANNEL_ID, channelId);
            } else {
                i.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            }
            i.putExtra(Settings.EXTRA_APP_PACKAGE, ctx.getPackageName());
        } else {
            i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.parse("package:" + ctx.getPackageName()));
        }
        return i;
    }

    // --- 3. Bỏ tối ưu pin (giúp service sống lâu hơn, đặc biệt trên OEM) ---

    public static boolean isIgnoringBatteryOptimizations(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(ctx.getPackageName());
    }

    /** Intent mở Settings để user whitelist app khỏi battery optimization. */
    @SuppressWarnings("BatteryLife")
    public static Intent requestIgnoreBatteryOptimizationsIntent(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            i.setData(Uri.parse("package:" + ctx.getPackageName()));
            return i;
        }
        return null;
    }

}
