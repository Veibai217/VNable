package com.Veibai.VNable.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Background {

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Background() {
    }

    public static void io(Runnable task) {
        IO.execute(task);
    }

    public static void main(Runnable task) {
        MAIN.post(task);
    }
}
