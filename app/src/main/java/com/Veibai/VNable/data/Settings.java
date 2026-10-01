package com.Veibai.VNable.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.Veibai.VNable.BuildConfig;

public final class Settings {

    public static final String VERSION = BuildConfig.VERSION_NAME;

    public enum TextSpeed {
        SLOW(90), NORMAL(45), FAST(18), INSTANT(0);
        public final long delayMs;

        TextSpeed(long delayMs) {
            this.delayMs = delayMs;
        }
    }

    public enum FontSize { SMALL, STANDARD, LARGE }

    private static final String PREFS = "vn_settings";
    private static final String KEY_SPEED = "text_speed";
    private static final String KEY_AUDIO = "audio_on";
    private static final String KEY_FONT = "font_size";
    private static final String KEY_THEME = "theme_id";
    private static final boolean DEFAULT_AUDIO = true;

    private Settings() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static TextSpeed textSpeed(Context context) {
        int idx = prefs(context).getInt(KEY_SPEED, TextSpeed.NORMAL.ordinal());
        TextSpeed[] values = TextSpeed.values();
        return values[Math.max(0, Math.min(idx, values.length - 1))];
    }

    public static void setTextSpeed(Context context, TextSpeed speed) {
        prefs(context).edit().putInt(KEY_SPEED, speed.ordinal()).apply();
    }

    public static boolean audioEnabled(Context context) {
        return prefs(context).getBoolean(KEY_AUDIO, DEFAULT_AUDIO);
    }

    public static void setAudioEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_AUDIO, enabled).apply();
    }

    public static FontSize fontSize(Context context) {
        int idx = prefs(context).getInt(KEY_FONT, FontSize.STANDARD.ordinal());
        FontSize[] values = FontSize.values();
        return values[Math.max(0, Math.min(idx, values.length - 1))];
    }

    public static void setFontSize(Context context, FontSize size) {
        prefs(context).edit().putInt(KEY_FONT, size.ordinal()).apply();
    }

    public static ThemeMode theme(Context context) {
        return ThemeMode.byId(prefs(context).getString(KEY_THEME, null));
    }

    public static void setTheme(Context context, ThemeMode mode) {
        prefs(context).edit().putString(KEY_THEME, mode.id).apply();
    }
}
