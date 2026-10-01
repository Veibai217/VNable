package com.Veibai.VNable.data;

import org.json.JSONObject;

public final class VnButton {

    public final String id;
    public final String text;
    public final String action;
    public final Jump jump;

    private VnButton(String id, String text, String action, Jump jump) {
        this.id = id;
        this.text = text;
        this.action = action;
        this.jump = jump;
    }

    public static VnButton parse(JSONObject o, int index) {
        if (o == null) return null;
        String fallbackId = "btn_" + index;
        Object fun = o.opt("fun");
        if (fun instanceof JSONObject) {
            return new VnButton(
                    o.optString("id", fallbackId),
                    o.optString("text", ""),
                    null,
                    Jump.parse((JSONObject) fun));
        }
        return new VnButton(
                o.optString("id", fallbackId),
                o.optString("text", ""),
                o.optString("fun", "next"),
                null);
    }

    public boolean hasAction() {
        return action != null;
    }
}
