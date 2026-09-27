package com.Veibai.VNable.data;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class SaveManager {

    public static final int SLOTS = 3;

    public static class SaveData {
        public String packUid;
        public String sceneRef;
        public String label;
        public long savedAt;
        public JSONObject items = new JSONObject();

        JSONObject toJson() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("pack", packUid == null ? "" : packUid);
            o.put("scene", sceneRef);
            o.put("label", label == null ? "" : label);
            o.put("time", savedAt);
            o.put("items", items);
            return o;
        }

        static SaveData fromJson(JSONObject o) {
            SaveData d = new SaveData();
            d.packUid = o.optString("pack", null);
            d.sceneRef = o.optString("scene", null);
            d.label = o.optString("label", "");
            d.savedAt = o.optLong("time", 0L);
            d.items = o.optJSONObject("items");
            if (d.items == null) d.items = new JSONObject();
            return d;
        }
    }

    private final File dir;

    public SaveManager(Context context) {
        dir = new File(context.getFilesDir(), "saves");
        if (!dir.exists()) dir.mkdirs();
    }

    private File file(int slot) {
        return new File(dir, "slot_" + slot + ".json");
    }

    public SaveData load(int slot) {
        File f = file(slot);
        if (!f.exists()) return null;
        try (FileInputStream fis = new FileInputStream(f)) {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = fis.read(buf)) > 0) bos.write(buf, 0, n);
            return SaveData.fromJson(new JSONObject(bos.toString("UTF-8")));
        } catch (Exception e) {
            return null;
        }
    }

    public void save(int slot, SaveData data) {
        File target = file(slot);
        File tmp = new File(dir, target.getName() + ".tmp");
        try (FileOutputStream fos = new FileOutputStream(tmp)) {
            fos.write(data.toJson().toString().getBytes(StandardCharsets.UTF_8));
            fos.getFD().sync();
        } catch (IOException | JSONException e) {
            tmp.delete();
            return;
        }
        if (!tmp.renameTo(target)) {
            target.delete();
            if (!tmp.renameTo(target)) tmp.delete();
        }
    }

    public void delete(int slot) {
        File f = file(slot);
        if (f.exists())
            f.delete();
    }

    public int latestSlot(String packUid) {
        int best = -1;
        long bestTime = -1;
        for (int i = 1; i <= SLOTS; i++) {
            SaveData d = load(i);
            if (d == null || d.savedAt <= bestTime) continue;
            if (packUid != null && !packUid.equals(d.packUid)) continue;
            bestTime = d.savedAt;
            best = i;
        }
        return best;
    }

    public static JSONObject itemsToJson(Inventory inventory) {
        JSONObject o = new JSONObject();
        for (java.util.Map.Entry<String, Integer> e : inventory.snapshot().entrySet()) {
            try {
                o.put(e.getKey(), e.getValue());
            } catch (JSONException ignored) {
            }
        }
        return o;
    }

    public static void jsonToItems(JSONObject items, Inventory inventory) {
        if (items == null) return;
        Iterator<String> it = items.keys();
        while (it.hasNext()) {
            String k = it.next();
            int v = items.optInt(k, 0);
            if (v > 0) inventory.add(k, v);
        }
    }
}
