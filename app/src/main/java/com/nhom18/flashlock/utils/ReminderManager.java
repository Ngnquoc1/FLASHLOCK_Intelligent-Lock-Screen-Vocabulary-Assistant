package com.nhom18.flashlock.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;

import com.nhom18.flashlock.receiver.ReminderReceiver;

public class ReminderManager {

    // Mã định danh cho báo thức này để sau này gọi ra hủy cho đúng
    private static final int REMINDER_REQUEST_CODE = 1001;

    public static void setDailyReminder(Context context, int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        // Trỏ tới class ReminderReceiver (chúng ta sẽ tạo ở Bước 3)
        Intent intent = new Intent(context, ReminderReceiver.class);
        
        // Tạo PendingIntent với cờ FLAG_IMMUTABLE (bắt buộc từ Android 12 trở lên)
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REMINDER_REQUEST_CODE, intent, flags);

        // Sử dụng Calendar để tính toán thời gian báo thức
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // LOGIC QUAN TRỌNG: 
        // Nếu giờ người dùng chọn <= giờ hiện tại (tức là đã qua rồi) -> Đẩy báo thức sang ngày mai
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Đặt báo thức
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Cho Android 6.0 trở lên: Ép hệ thống thức dậy đúng giờ dù đang ngủ sâu
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                // Cho các máy cũ hơn
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
        }
    }

    public static void cancelReminder(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        // Phải gọi lại y chang cái Intent và PendingIntent như lúc Set thì mới Hủy được
        Intent intent = new Intent(context, ReminderReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REMINDER_REQUEST_CODE, intent, flags);

        // Hủy báo thức
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}