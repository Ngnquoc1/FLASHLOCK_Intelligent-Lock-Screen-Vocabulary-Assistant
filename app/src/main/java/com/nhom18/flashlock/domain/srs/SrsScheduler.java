package com.nhom18.flashlock.domain.srs;

import com.google.firebase.Timestamp;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class SrsScheduler {
    private static final int[] DAY_INTERVALS = new int[]{1, 1, 3, 7, 14};

    public Timestamp nextReviewForLevel(int level, Timestamp now) {
        int safeLevel = Math.min(Math.max(level, 1), DAY_INTERVALS.length);
        long days = DAY_INTERVALS[safeLevel - 1];
        long base = now != null ? now.toDate().getTime() : System.currentTimeMillis();
        return new Timestamp(new Date(base + TimeUnit.DAYS.toMillis(days)));
    }

    public int nextLevelOnRemember(int currentLevel) {
        int safeCurrent = Math.max(currentLevel, 1);
        return Math.min(safeCurrent + 1, DAY_INTERVALS.length);
    }
}
