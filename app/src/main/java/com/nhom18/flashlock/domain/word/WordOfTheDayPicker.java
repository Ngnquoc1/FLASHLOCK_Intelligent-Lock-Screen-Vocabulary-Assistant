package com.nhom18.flashlock.domain.word;

import com.nhom18.flashlock.data.model.Word;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Chọn "Word of the Day" theo cách deterministic: cùng (uid, ngày, pool) → cùng kết quả.
 * Nhờ đó Home và LockScreenStudyService có thể độc lập gọi và luôn ra cùng một từ,
 * không cần chia sẻ state qua process/IPC.
 *
 * Sắp xếp pool theo wordId trước khi chọn để kết quả ổn định bất kể thứ tự input.
 */
public class WordOfTheDayPicker {

    public Word pick(String uid, Date date, List<Word> pool) {
        if (pool == null || pool.isEmpty() || date == null) {
            return null;
        }
        List<Word> sorted = new ArrayList<>(pool);
        Collections.sort(sorted, (a, b) -> {
            String ka = a == null || a.getWordId() == null ? "" : a.getWordId();
            String kb = b == null || b.getWordId() == null ? "" : b.getWordId();
            return ka.compareTo(kb);
        });
        int idx = Math.floorMod(seedFor(uid, date), sorted.size());
        return sorted.get(idx);
    }

    int seedFor(String uid, Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        long ymd = (long) c.get(Calendar.YEAR) * 10000L
                + (c.get(Calendar.MONTH) + 1) * 100L
                + c.get(Calendar.DAY_OF_MONTH);
        String key = (uid == null ? "" : uid) + ":" + ymd;
        return key.hashCode();
    }
}
