package com.Veibai.VNable.util;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class LauncherArt {

    private static final int MAX_VARIANTS = 64;

    private static final Random RANDOM = new Random();

    private LauncherArt() {
    }

    public static int pick(Context context, String base) {
        List<Integer> pool = new ArrayList<>();
        collect(context, base, pool);
        for (int i = 1; i <= MAX_VARIANTS; i++) {
            int before = pool.size();
            collect(context, base + "_" + i, pool);
            if (pool.size() == before) break;
        }
        if (pool.isEmpty()) return 0;
        return pool.get(RANDOM.nextInt(pool.size()));
    }

    private static void collect(Context context, String name, List<Integer> pool) {
        int id = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
        if (id != 0) pool.add(id);
    }
}
