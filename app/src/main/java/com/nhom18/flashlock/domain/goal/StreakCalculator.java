package com.nhom18.flashlock.domain.goal;

import java.util.Calendar;
import java.util.Date;

/**
 * Tính streak hằng ngày một cách thuần (không phụ thuộc Firebase/UI) để tái dùng
 * ở cả màn Home lẫn luồng học. Nhờ đó việc hoàn thành mục tiêu được ghi nhận đúng
 * ngày, không phụ thuộc việc người dùng có mở lại màn Home hay không.
 */
public class StreakCalculator {

    public static class Result {
        public final int streak;
        public final Date lastCompletedDate;
        public final boolean needsUpdate;

        public Result(int streak, Date lastCompletedDate, boolean needsUpdate) {
            this.streak = streak;
            this.lastCompletedDate = lastCompletedDate;
            this.needsUpdate = needsUpdate;
        }
    }

    public Result evaluate(int currentStreak, Date lastCompletedDate, boolean goalMetToday, Date now) {
        long daysDiff = daysBetween(lastCompletedDate, now);

        // Đã ghi nhận hoàn thành trong hôm nay -> không đổi.
        if (lastCompletedDate != null && daysDiff == 0) {
            return new Result(currentStreak, lastCompletedDate, false);
        }

        // Hôm qua vừa hoàn thành: nếu hôm nay đạt thì nối chuỗi, chưa đạt thì giữ nguyên (ngày chưa kết thúc).
        if (daysDiff == 1) {
            if (goalMetToday) {
                return new Result(currentStreak + 1, now, true);
            }
            return new Result(currentStreak, lastCompletedDate, false);
        }

        // Bỏ lỡ trọn >= 1 ngày, hoặc chưa từng hoàn thành.
        if (goalMetToday) {
            return new Result(1, now, true);
        }
        if (currentStreak > 0) {
            return new Result(0, lastCompletedDate, true);
        }
        return new Result(currentStreak, lastCompletedDate, false);
    }

    private long daysBetween(Date from, Date to) {
        if (from == null || to == null) {
            return -1;
        }
        long start = startOfDayMillis(from);
        long end = startOfDayMillis(to);
        return (end - start) / (24L * 60 * 60 * 1000);
    }

    private long startOfDayMillis(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
