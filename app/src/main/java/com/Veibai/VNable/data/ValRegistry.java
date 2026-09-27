package com.Veibai.VNable.data;

import com.Veibai.VNable.util.JsonText;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ValRegistry {

    public static final class Item {
        public final String id;
        public final File icon;

        Item(String id, File icon) {
            this.id = id;
            this.icon = icon;
        }
    }

    private final Map<String, Item> items = new LinkedHashMap<>();

    private ValRegistry() {
    }

    public static ValRegistry load(ScriptPack pack) {
        ValRegistry registry = new ValRegistry();
        File file = new File(pack.dir, ScriptPack.VALUE_FILE);
        if (!file.isFile()) return registry;
        try {
            JSONObject json = new JSONObject(
                    JsonText.strip(readAll(file)));
            java.util.Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String id = keys.next();
                String ref = json.optString(id, null);
                if (ref == null || ref.isEmpty()) continue;
                File icon = PackRes.file(pack, ref, "image");
                registry.items.put(id, new Item(id, icon));
            }
        } catch (Exception ignored) {

        }
        return registry;
    }

    public Item find(String id) {
        return items.get(id);
    }

    public List<Item> all() {
        return Collections.unmodifiableList(new ArrayList<>(items.values()));
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
}
