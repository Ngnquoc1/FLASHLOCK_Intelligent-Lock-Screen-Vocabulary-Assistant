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

    // ID của kênh thông báo (Bắt buộc phải có từ Android 8.0 trở lên)
    private static final String CHANNEL_ID = "FLASHLOCK_REMINDER_CHANNEL";

    @Override
    public void onReceive(Context context, Intent intent) {
        
        // 1. Tạo Notification Channel
        createNotificationChannel(context);

        // 2. Tạo Intent để mở app khi người dùng bấm vào thông báo
        Intent mainIntent = new Intent(context, MainActivity.class); 
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
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
            // Số 2001 là ID của thông báo này, để hệ thống phân biệt với các thông báo khác
            notificationManager.notify(2001, builder.build());
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

    // Hàm tạo Kênh thông báo (Chỉ chạy trên Android 8.0 trở lên)
    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Nhắc nhở học tập";
            String description = "Kênh gửi thông báo nhắc nhở học từ vựng hàng ngày";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}