package com.Veibai.VNable.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VnScene {

    public static final String SIDE_LEFT = "l";
    public static final String SIDE_RIGHT = "r";
    public static final String AUDIO_LOOP = "loop";

    public final String key;
    public final String bg;
    public final String audioDir;
    public final boolean audioLoop;
    public final String actorRes;
    public final String actorSide;
    public final String actorName;
    public final String actorMsg;
    public final List<VnButton> buttons;
    public final Map<String, Integer> add;
    public final Map<String, Integer> sub;
    public final Condition cond;
    public final Jump next;

    private VnScene(String key, String bg, String audioDir, boolean audioLoop,
                    String actorRes, String actorSide, String actorName, String actorMsg,
                    List<VnButton> buttons, Map<String, Integer> add,
                    Map<String, Integer> sub, Condition cond, Jump next) {
        this.key = key;
        this.bg = bg;
        this.audioDir = audioDir;
        this.audioLoop = audioLoop;
        this.actorRes = actorRes;
        this.actorSide = actorSide;
        this.actorName = actorName;
        this.actorMsg = actorMsg;
        this.buttons = buttons;
        this.add = add;
        this.sub = sub;
        this.cond = cond;
        this.next = next;
    }

    public static VnScene parse(String key, JSONObject o) {
        JSONObject audio = o.optJSONObject("audio");
        JSONObject actor = o.optJSONObject("actor");

        List<VnButton> buttons = new ArrayList<>();
        JSONArray btnArr = o.optJSONArray("btn");
        if (btnArr != null) {
            for (int i = 0; i < btnArr.length(); i++) {
                VnButton btn = VnButton.parse(btnArr.optJSONObject(i), i);
                if (btn != null) buttons.add(btn);
            }
        }

        return new VnScene(
                key,
                o.optString("bg", null),
                audio == null ? null : audio.optString("dir", null),
                audio != null && AUDIO_LOOP.equals(audio.optString("mode", "")),
                actor == null ? null : actor.optString("res", null),
                actor == null ? SIDE_LEFT : actor.optString("side", SIDE_LEFT),
                actor == null ? null : actor.optString("name", null),
                actor == null ? "" : actor.optString("msg", ""),
                buttons,
                counts(o.optJSONObject("add")),
                counts(o.optJSONObject("sub")),
                Condition.parse(o.optJSONObject("if"), 1),
                Jump.parse(o.optJSONObject("next")));
    }

    private static Map<String, Integer> counts(JSONObject o) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (o == null) return map;
        Iterator<String> it = o.keys();
        while (it.hasNext()) {
            String id = it.next();
            map.put(id, o.optInt(id, 0));
        }
        return map;
    }

    public boolean hasButtons() {
        return buttons != null && !buttons.isEmpty();
    }
}
