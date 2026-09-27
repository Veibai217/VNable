package com.Veibai.VNable.util;

public final class JsonText {

    private JsonText() {
    }

    public static String strip(String raw) {
        if (raw == null) return "";
        StringBuilder out = new StringBuilder(raw.length());
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (inString) {
                out.append(c);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                out.append(c);
                continue;
            }
            if (c == '/' && i + 1 < raw.length()) {
                char next = raw.charAt(i + 1);
                if (next == '/') {
                    while (i < raw.length() && raw.charAt(i) != '\n') i++;
                    if (i < raw.length()) out.append('\n');
                    continue;
                }
                if (next == '*') {
                    i += 2;
                    while (i + 1 < raw.length()
                            && !(raw.charAt(i) == '*' && raw.charAt(i + 1) == '/')) i++;
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString().replace("\uFEFF", "").trim();
    }
}
