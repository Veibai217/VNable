package com.Veibai.VNable.data;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Condition {

    public static final int MAX_DEPTH = 3;

    public static final String MODE_ABOVE = "above";
    public static final String MODE_BELOW = "below";
    public static final String MODE_EQUAL = "equal";

    public final String mode;
    public final Map<String, Integer> cond;

    public final Object thenBranch;
    public final Object elseBranch;

    private Condition(String mode, Map<String, Integer> cond,
                      Object thenBranch, Object elseBranch) {
        this.mode = mode;
        this.cond = cond;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    public static Condition parse(JSONObject o, int depth) {
        if (o == null || !o.has("mode")) return null;
        String mode = o.optString("mode", MODE_ABOVE);
        Map<String, Integer> cond = new LinkedHashMap<>();
        JSONObject condJson = o.optJSONObject("cond");
        if (condJson != null) {
            Iterator<String> it = condJson.keys();
            while (it.hasNext()) {
                String key = it.next();
                cond.put(key, condJson.optInt(key, 0));
            }
        }
        return new Condition(mode, cond,
                parseBranch(o.opt("then"), depth),
                parseBranch(o.opt("else"), depth));
    }

    private static Object parseBranch(Object raw, int depth) {
        if (raw == null || raw == JSONObject.NULL) return "next";
        if (raw instanceof String) return raw;
        JSONObject json = (JSONObject) raw;
        if (json.has("mode")) {
            return depth < MAX_DEPTH ? parse(json, depth + 1) : null;
        }
        return Jump.parse(json);
    }

    public boolean test(Inventory inventory) {
        for (Map.Entry<String, Integer> entry : cond.entrySet()) {
            int held = inventory.count(entry.getKey());
            int need = entry.getValue() == null ? 0 : entry.getValue();
            switch (mode) {
                case MODE_BELOW:
                    if (held > need) return false;
                    break;
                case MODE_EQUAL:
                    if (held != need) return false;
                    break;
                case MODE_ABOVE:
                default:
                    if (held < need) return false;
                    break;
            }
        }
        return true;
    }
}
