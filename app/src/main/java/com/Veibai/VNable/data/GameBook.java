package com.Veibai.VNable.data;

import android.util.Log;

import com.Veibai.VNable.util.JsonText;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class GameBook {

    private static final String TAG = "GameBook";

    private static final Map<String, GameBook> CACHE = new HashMap<>();

    private final Map<String, VnScene> scenes = new HashMap<>();

    private GameBook(JSONObject root) {
        Iterator<String> it = root.keys();
        while (it.hasNext()) {
            String key = it.next();
            JSONObject sceneJson = root.optJSONObject(key);
            if (sceneJson != null) {
                scenes.put(key, VnScene.parse(key, sceneJson));
            }
        }
    }

    public static GameBook load(ScriptPack pack, String fileName) throws IOException {
        String cacheKey = pack.uid + "/" + fileName;
        synchronized (CACHE) {
            GameBook cached = CACHE.get(cacheKey);
            if (cached != null) return cached;
        }
        File file = new File(pack.scriptsDir, fileName);
        if (!file.isFile()) {
            throw new IOException("script file not found: " + file);
        }
        GameBook book;
        try {
            book = new GameBook(new JSONObject(JsonText.strip(readAll(file))));
        } catch (Exception e) {
            throw new IOException("malformed script json: " + file, e);
        }
        synchronized (CACHE) {
            CACHE.put(cacheKey, book);
        }
        return book;
    }

    public VnScene scene(String key) {
        return scenes.get(key);
    }

    public static VnScene resolve(ScriptPack pack, String address) {
        if (pack == null || address == null) return null;
        String path = address.startsWith("/") ? address.substring(1) : address;
        int split = path.lastIndexOf('/');
        if (split <= 0 || split == path.length() - 1) return null;
        String fileName = path.substring(0, split);
        String sceneKey = path.substring(split + 1);
        try {
            return load(pack, fileName).scene(sceneKey);
        } catch (IOException e) {
            Log.w(TAG, "resolve failed: " + address, e);
            return null;
        }
    }

    private static String readAll(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader =
                     new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            char[] buf = new char[4096];
            int n;
            while ((n = reader.read(buf)) > 0) sb.append(buf, 0, n);
        }
        return sb.toString();
    }

    public static void invalidate(String packUid) {
        synchronized (CACHE) {
            CACHE.keySet().removeIf(key -> key.startsWith(packUid + "/"));
        }
    }
}
