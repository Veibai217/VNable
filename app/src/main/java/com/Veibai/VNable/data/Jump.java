package com.Veibai.VNable.data;

import org.json.JSONObject;

public final class Jump {

    public final String dir;
    public final String target;

    private Jump(String dir, String target) {
        this.dir = dir;
        this.target = target;
    }

    public static Jump parse(JSONObject o) {
        if (o == null) return null;
        String dir = o.optString("dir", "").trim();
        String target = o.optString("target", "").trim();
        if (dir.isEmpty() || target.isEmpty()) return null;
        return new Jump(dir, target);
    }

    public String ref() {
        String base = dir.startsWith("/") ? dir : "/" + dir;
        return base.endsWith("/") ? base + target : base + "/" + target;
    }

    @Override
    public String toString() {
        return ref();
    }
}
