package com.Veibai.VNable.data;

import android.content.Context;
import android.net.Uri;

import com.Veibai.VNable.R;
import com.Veibai.VNable.util.JsonText;
import com.Veibai.VNable.util.LogExporter;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class PackImporter {

    public static final class Result {
        public final ScriptPack pack;
        public final String error;

        private Result(ScriptPack pack, String error) {
            this.pack = pack;
            this.error = error;
        }

        static Result ok(ScriptPack pack) {
            return new Result(pack, null);
        }

        static Result fail(String error) {
            return new Result(null, error);
        }

        public boolean succeeded() {
            return pack != null;
        }
    }

    private static final String[] JUNK_PREFIXES = {"__MACOSX/", ".DS_Store"};

    private PackImporter() {
    }

    public static Result importPack(Context context, Uri uri) {
        Report report = new Report(context);
        File temp = null;
        File target = null;
        boolean targetReady = false;
        try {
            temp = stashToCache(context, uri, report);
            try (ZipFile zip = new ZipFile(temp)) {
                ZipEntry infoEntry = locate(zip, ScriptPack.INFO_FILE);
                if (infoEntry == null) {
                    return fail(context, report, context.getString(R.string.import_err_no_info));
                }
                String prefix = dirPrefix(infoEntry.getName());

                JSONObject info = new JSONObject(
                        JsonText.strip(text(zip, infoEntry)));
                String uid = info.optString("uid", "").trim();
                if (uid.isEmpty() || uid.contains("/") || uid.contains("\\")
                        || ".".equals(uid) || "..".equals(uid)) {
                    return fail(context, report,
                            context.getString(R.string.import_err_bad_uid, uid));
                }
                if (locate(zip, prefix + ScriptPack.ENTRY_FILE) == null) {
                    return fail(context, report, context.getString(R.string.import_err_no_main));
                }

                report.step(context.getString(R.string.import_log_pack_info,
                        info.optString("name", uid), uid, info.optString("ver", "")));

                target = new File(ScriptPack.packsRoot(context), uid);
                if (target.exists()) {
                    report.step(context.getString(R.string.import_log_old_found));
                    ScriptPack.deleteRecursively(target);
                }
                if (!target.mkdirs() && !target.isDirectory()) {
                    return fail(context, report,
                            context.getString(R.string.import_err_mkdir, target.getPath()));
                }
                targetReady = true;

                extract(context, zip, target, prefix, report);
                ScriptPack pack = ScriptPack.load(target);
                if (pack == null) {
                    ScriptPack.deleteRecursively(target);
                    targetReady = false;
                    return fail(context, report,
                            context.getString(R.string.import_err_parse_info));
                }
                report.step(context.getString(R.string.import_log_extracted, report.fileCount));
                return Result.ok(pack);
            }
        } catch (Exception e) {
            if (targetReady && target != null) ScriptPack.deleteRecursively(target);
            return fail(context, report,
                    context.getString(R.string.import_err_exception, e.getMessage()));
        } finally {
            if (temp != null) temp.delete();
        }
    }

    private static File stashToCache(Context context, Uri uri, Report report) throws Exception {
        String name = queryDisplayName(context, uri);
        report.step(context.getString(R.string.import_log_file,
                name != null ? name : uri.toString()));
        File temp = File.createTempFile("pack_", ".zip", context.getCacheDir());
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(temp)) {
            if (in == null) throw new IllegalStateException(
                    context.getString(R.string.import_err_read));
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
        report.step(context.getString(R.string.import_log_cached, temp.length()));
        return temp;
    }

    private static ZipEntry locate(ZipFile zip, String fileName) {
        ZipEntry exact = zip.getEntry(fileName);
        if (exact != null) return exact;
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String path = entry.getName();

            if (!entry.isDirectory()
                    && path.endsWith("/" + fileName)
                    && path.indexOf('/') == path.lastIndexOf('/')) {
                return entry;
            }
        }
        return null;
    }

    private static String dirPrefix(String entryName) {
        int slash = entryName.lastIndexOf('/');
        return slash < 0 ? "" : entryName.substring(0, slash + 1);
    }

    private static String text(ZipFile zip, ZipEntry entry) throws Exception {
        try (InputStream in = zip.getInputStream(entry)) {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            return bos.toString("UTF-8");
        }
    }

    private static void extract(Context context, ZipFile zip, File target, String prefix,
                                Report report) throws Exception {
        String canonicalTarget = target.getCanonicalPath() + File.separator;
        Enumeration<? extends ZipEntry> entries = zip.entries();
        int count = 0;
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String path = entry.getName();
            if (entry.isDirectory() || isJunk(path)) continue;
            if (path.startsWith("/")) path = path.substring(1);

            if (!prefix.isEmpty()) {
                if (!path.startsWith(prefix)) continue;
                path = path.substring(prefix.length());
            }
            if (path.isEmpty()) continue;

            File out = new File(target, path);

            if (!out.getCanonicalPath().startsWith(canonicalTarget)) {
                throw new SecurityException(context.getString(
                        R.string.import_err_slip, entry.getName()));
            }
            File parent = out.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (InputStream in = zip.getInputStream(entry);
                 OutputStream fos = new FileOutputStream(out)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) fos.write(buf, 0, n);
            }
            count++;
        }
        report.fileCount = count;
    }

    private static boolean isJunk(String path) {
        for (String prefix : JUNK_PREFIXES) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }

    private static Result fail(Context context, Report report, String reason) {
        report.step(context.getString(R.string.import_log_fail, reason));
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(new Date());
        LogExporter.export(context, "VNable_import_" + stamp + ".log", report.dump());
        return Result.fail(reason);
    }

    private static String queryDisplayName(Context context, Uri uri) {
        try (android.database.Cursor cursor = context.getContentResolver().query(uri, null,
                null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(
                        android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return cursor.getString(idx);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static final class Report {
        private final Context context;
        private final StringBuilder lines = new StringBuilder();
        private final long startedAt = System.currentTimeMillis();
        private int fileCount;

        Report(Context context) {
            this.context = context;
        }

        void step(String line) {
            lines.append(new SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
                    .format(new Date())).append(' ').append(line).append('\n');
        }

        String dump() {
            return context.getString(R.string.import_log_title)
                    + "\n" + context.getString(R.string.import_log_time)
                    + new Date(startedAt) + "\n---\n" + lines;
        }
    }
}
