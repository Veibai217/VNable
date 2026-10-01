package com.Veibai.VNable.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class LogExporter {

    private LogExporter() {
    }

    public static File export(Context context, String fileName, String content) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return exportViaMediaStore(context, fileName, content);
        }
        return exportLegacy(context, fileName, content);
    }

    private static File exportViaMediaStore(Context context, String fileName, String content) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
        values.put(MediaStore.Downloads.IS_PENDING, 1);
        Uri uri = context.getContentResolver()
                .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) return null;
        try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
            if (os == null) throw new IOException("Downloads resolver returned null stream");
            os.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            context.getContentResolver().delete(uri, null, null);
            return fallbackToPrivate(context, fileName, content);
        }
        ContentValues done = new ContentValues();
        done.put(MediaStore.Downloads.IS_PENDING, 0);
        context.getContentResolver().update(uri, done, null, null);
        return mediaPath(context, fileName);
    }

    private static File exportLegacy(Context context, String fileName, String content) {
        boolean permitted = ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        if (!permitted) return fallbackToPrivate(context, fileName, content);
        File dir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);

        if (!dir.exists()) dir.mkdirs();
        return writeFile(new File(dir, fileName), content);
    }

    private static File fallbackToPrivate(Context context, String fileName, String content) {
        File dir = new File(context.getFilesDir(), "logs");
        if (!dir.exists()) dir.mkdirs();
        return writeFile(new File(dir, fileName), content);
    }

    private static File writeFile(File target, String content) {
        try (FileOutputStream fos = new FileOutputStream(target)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
            return target;
        } catch (IOException e) {
            return null;
        }
    }

    private static File mediaPath(Context context, String fileName) {
        File legacy = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);
        File probe = new File(legacy, fileName);
        return probe.exists() ? probe : null;
    }
}
