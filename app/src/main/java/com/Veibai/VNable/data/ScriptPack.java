package com.Veibai.VNable.data;

import android.content.Context;

import com.Veibai.VNable.util.JsonText;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ScriptPack implements Comparable<ScriptPack> {

    public static final String INFO_FILE = "Info.json";

    public static final String ENTRY_FILE = "Main.json";

    public static final String VALUE_FILE = "Val.json";

    public static final String SCRIPTS_ROOT = "Parks";

    public static File packsRoot(Context context) {

        File external = context.getExternalFilesDir(null);
        File root = new File(external != null ? external : context.getFilesDir(), "pack");
        if (!root.exists()) root.mkdirs();
        return root;
    }

    public static File scriptsRoot(Context context) {
        File root = new File(context.getFilesDir(), SCRIPTS_ROOT);
        if (!root.exists()) root.mkdirs();
        return root;
    }

    public final File dir;
    public final File scriptsDir;
    public final String name;
    public final String author;
    public final String uid;
    public final String ver;
    public final List<String> tags;

    private ScriptPack(File dir, File scriptsDir, String name, String author,
                       String uid, String ver, List<String> tags) {
        this.dir = dir;
        this.scriptsDir = scriptsDir;
        this.name = name;
        this.author = author;
        this.uid = uid;
        this.ver = ver;
        this.tags = tags;
    }

    public static ScriptPack load(File dir, Context context) {
        File info = new File(dir, INFO_FILE);
        if (!info.isFile()) return null;
        try {
            JSONObject json = new JSONObject(JsonText.strip(readAll(info)));
            String uid = json.optString("uid", "").trim();
            if (uid.isEmpty()) return null;
            List<String> tags = new ArrayList<>();
            org.json.JSONArray arr = json.optJSONArray("type");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    String tag = arr.optString(i, "").trim();
                    if (!tag.isEmpty()) tags.add(tag);
                }
            }
            String key = json.optString("key", "");
            File scriptsDir = key.isEmpty()
                    ? dir : new File(scriptsRoot(context), uid);
            return new ScriptPack(dir, scriptsDir,
                    json.optString("name", uid),
                    json.optString("author", ""),
                    uid,
                    json.optString("ver", "1.0"),
                    tags);
        } catch (Exception e) {
            return null;
        }
    }

    public String displayAuthor() {
        return author == null || author.isEmpty() ? "-" : author;
    }

    public static List<ScriptPack> listInstalled(Context context) {
        List<ScriptPack> packs = new ArrayList<>();
        File[] dirs = packsRoot(context).listFiles(File::isDirectory);
        if (dirs != null) {
            for (File dir : dirs) {
                ScriptPack pack = load(dir, context);
                if (pack != null) packs.add(pack);
            }
        }
        Collections.sort(packs);
        return packs;
    }

    public static ScriptPack findByUid(Context context, String uid) {
        if (uid == null || uid.isEmpty()) return null;
        File dir = new File(packsRoot(context), uid);
        return dir.isDirectory() ? load(dir, context) : null;
    }

    public boolean uninstall() {
        boolean ok = deleteRecursively(dir);
        if (scriptsDir != null && !scriptsDir.equals(dir)) {
            deleteRecursively(scriptsDir);
        }
        return ok;
    }

    public static boolean deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursively(child);
            }
        }
        return file.delete();
    }

    private static String readAll(File file) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            char[] buf = new char[4096];
            int n;
            while ((n = reader.read(buf)) > 0) sb.append(buf, 0, n);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(ScriptPack other) {
        return name.compareToIgnoreCase(other.name);
    }
}
