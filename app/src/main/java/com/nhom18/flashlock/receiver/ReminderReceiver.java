package com.nhom18.flashlock.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.nhom18.flashlock.ui.main.MainActivity;
import com.nhom18.flashlock.R;
import com.nhom18.flashlock.utils.ReminderManager;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "flashlock_reminder_v1";
    private static final int NOTIFICATION_ID = 2001;

    @Override
    public void onReceive(Context context, Intent intent) {

        createNotificationChannel(context);

        // FLAG_ACTIVITY_NEW_TASK đủ để mở app từ Broadcast; bỏ CLEAR_TASK để không
        // xóa stack hiện tại nếu user đang ở giữa app.
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                mainIntent, 
                flags
        );
// 3. Nặn hình dáng của thông báo đa ngôn ngữ
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_flashlock)

                // GỌI TỪ STRINGS.XML QUA BIẾN CONTEXT
                .setContentTitle(context.getString(R.string.reminder_title))
                .setContentText(context.getString(R.string.reminder_content))

                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        // 4. Bắn thông báo lên màn hình
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }

        // 5. QUAN TRỌNG: Đặt lại báo thức cho ngày mai
        // Lấy thông tin từ SharedPreferences để cài tiếp cho ngày mai
        SharedPreferences prefs = context.getSharedPreferences("FlashLockPrefs", Context.MODE_PRIVATE);
        boolean isEnabled = prefs.getBoolean("dailyReminderEnabled", false);
        
        if (isEnabled) {
            int hour = prefs.getInt("reminderHour", 20);
            int minute = prefs.getInt("reminderMinute", 30);
            ReminderManager.setDailyReminder(context, hour, minute);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(com.nhom18.flashlock.R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(com.nhom18.flashlock.R.string.reminder_channel_description));
        manager.createNotificationChannel(channel);
    }
}