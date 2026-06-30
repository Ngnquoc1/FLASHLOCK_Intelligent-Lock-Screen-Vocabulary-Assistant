package com.nhom18.flashlock.data.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

/**
 * Wrap 1 Firebase Task để retry với exponential backoff khi network/transient lỗi.
 * Mất hết retry → log exception vào Crashlytics (với custom keys tag) rồi reject Task ngoài.
 *
 * Dùng:
 *   RetryHelper.withRetry(
 *       "upsertProgress",
 *       () -> dataSource.upsertProgress(uid, progress),
 *       3, 500L);
 */
public final class RetryHelper {
    private static final String TAG = "RetryHelper";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private RetryHelper() {}

    public interface TaskSupplier<T> {
        Task<T> get();
    }

    public static <T> Task<T> withRetry(String tag, TaskSupplier<T> supplier,
                                        int maxAttempts, long baseDelayMs) {
        TaskCompletionSource<T> outer = new TaskCompletionSource<>();
        attempt(tag, supplier, 1, maxAttempts, baseDelayMs, outer);
        return outer.getTask();
    }

    /** Mặc định 3 lần, base delay 500ms (→ 500, 1000, 2000ms). */
    public static <T> Task<T> withRetry(String tag, TaskSupplier<T> supplier) {
        return withRetry(tag, supplier, 3, 500L);
    }

    private static <T> void attempt(String tag, TaskSupplier<T> supplier,
                                    int attempt, int max, long baseDelayMs,
                                    TaskCompletionSource<T> outer) {
        supplier.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                outer.setResult(task.getResult());
                return;
            }
            Exception e = task.getException();
            if (attempt >= max) {
                // Hết retry → log Crashlytics + reject.
                Log.w(TAG, tag + " failed after " + max + " attempts: "
                        + (e != null ? e.getMessage() : "unknown"));
                try {
                    FirebaseCrashlytics fc = FirebaseCrashlytics.getInstance();
                    fc.setCustomKey("retry_tag", tag);
                    fc.setCustomKey("retry_attempts", max);
                    fc.recordException(e != null ? e : new RuntimeException(tag + " failed"));
                } catch (Exception ignored) {
                    // Crashlytics chưa init (test env) → bỏ qua.
                }
                outer.setException(e != null ? e : new RuntimeException(tag + " failed"));
                return;
            }
            long delay = baseDelayMs * (1L << (attempt - 1)); // 500, 1000, 2000...
            Log.d(TAG, tag + " attempt " + attempt + " failed, retry in " + delay + "ms");
            MAIN.postDelayed(
                    () -> attempt(tag, supplier, attempt + 1, max, baseDelayMs, outer),
                    delay);
        });
    }
}
