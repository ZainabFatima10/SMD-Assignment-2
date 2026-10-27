package com.zainabfatima.i221064.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tiny helper for "do the heavy work off the main thread, then call back on it". */
public final class Async {

    public interface Callback<T> {
        void done(T result, Exception error);
    }

    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Async() { }

    public static <T> void run(final Callable<T> work, final Callback<T> callback) {
        POOL.execute(() -> {
            T result = null;
            Exception error = null;
            try {
                result = work.call();
            } catch (Exception e) {
                error = e;
            }
            final T r = result;
            final Exception err = error;
            MAIN.post(() -> callback.done(r, err));
        });
    }

    public static void ui(Runnable r) {
        MAIN.post(r);
    }
}
