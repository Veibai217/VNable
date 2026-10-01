package com.Veibai.VNable.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;

import java.io.File;
import java.util.Locale;

public final class PackRes {

    private static final String[] IMAGE_EXTS = {"webp", "png", "jpg", "jpeg", "gif", "bmp"};
    private static final String[] AUDIO_EXTS = {"wav", "mp3", "ogg", "m4a", "flac", "aac"};

    private static final LruCache<String, Bitmap> BITMAP_CACHE =
            new LruCache<String, Bitmap>((int)
                    (Runtime.getRuntime().maxMemory() / 8)) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return value.getByteCount();
                }
            };

    private PackRes() {
    }

    public static File file(ScriptPack pack, String ref, String kind) {
        if (pack == null || ref == null || ref.isEmpty()) return null;
        String rel = ref.startsWith("/") ? ref.substring(1) : ref;
        File direct = new File(pack.dir, rel);
        if (direct.isFile()) return direct;

        String[] exts = "audio".equals(kind) ? AUDIO_EXTS : IMAGE_EXTS;
        for (String ext : exts) {
            File candidate = new File(pack.dir, rel + "." + ext);
            if (candidate.isFile()) return candidate;
        }
        return null;
    }

    public static Bitmap bitmap(ScriptPack pack, String ref, int maxDim) {
        File file = file(pack, ref, "image");
        return file == null ? null : decode(pack.uid, file, maxDim);
    }

    public static Bitmap decode(String packUid, File file, int maxDim) {
        String key = (packUid == null ? "-" : packUid) + "|"
                + file.getAbsolutePath() + "@" + maxDim;
        Bitmap cached = BITMAP_CACHE.get(key);
        if (cached != null && !cached.isRecycled()) return cached;

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDim);
        Bitmap decoded = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
        if (decoded != null) BITMAP_CACHE.put(key, decoded);
        return decoded;
    }

    public static void clearPack(String packUid) {
        if (packUid == null) return;
        String prefix = packUid + "|";
        for (String key : BITMAP_CACHE.snapshot().keySet()) {
            if (key.startsWith(prefix)) BITMAP_CACHE.remove(key);
        }
    }

    private static int sampleSize(int width, int height, int maxDim) {
        int size = 1;
        if (width <= 0 || height <= 0 || maxDim <= 0) return size;
        while (width / (size * 2) >= maxDim || height / (size * 2) >= maxDim) {
            size *= 2;
        }
        return size;
    }

    public static int screenMaxDim(Context context) {
        return Math.max(context.getResources().getDisplayMetrics().widthPixels,
                context.getResources().getDisplayMetrics().heightPixels);
    }

    public static File audio(ScriptPack pack, String ref) {
        return file(pack, ref, "audio");
    }

    public static String describe(ScriptPack pack, String ref) {
        return String.format(Locale.US, "pack=%s ref=%s",
                pack == null ? "null" : pack.uid, ref);
    }
}
