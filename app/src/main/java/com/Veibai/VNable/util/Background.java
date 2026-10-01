package com.Veibai.VNable.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Background {

    private static final String TAG = "Background";
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Background() {
    }

    public static void io(Runnable task) {
        IO.execute(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                Log.e(TAG, "io task failed", t);
            }
        });
    }

    public static void main(Runnable task) {
        MAIN.post(task);
    }
}
