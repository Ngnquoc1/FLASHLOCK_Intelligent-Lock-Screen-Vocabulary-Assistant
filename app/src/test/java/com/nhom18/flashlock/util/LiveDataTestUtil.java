package com.nhom18.flashlock.util;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class LiveDataTestUtil {
    private LiveDataTestUtil() {
    }

    public static <T> T getOrAwaitValue(LiveData<T> liveData, long timeoutMs) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final Object[] data = new Object[1];
        Observer<T> observer = new Observer<T>() {
            @Override
            public void onChanged(T t) {
                data[0] = t;
                latch.countDown();
                liveData.removeObserver(this);
            }
        };
        liveData.observeForever(observer);
        if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            liveData.removeObserver(observer);
            throw new AssertionError("LiveData value was never set.");
        }
        @SuppressWarnings("unchecked")
        T value = (T) data[0];
        return value;
    }
}

