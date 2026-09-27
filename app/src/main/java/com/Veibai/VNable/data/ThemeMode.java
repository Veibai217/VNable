package com.Veibai.VNable.data;

import com.Veibai.VNable.R;

public enum ThemeMode {

    AMBER("amber", R.string.theme_amber, R.style.AppTheme, R.style.AppTheme_Splash, 0xFFE5BC7F),
    OCEAN("ocean", R.string.theme_ocean, R.style.AppTheme_Ocean, R.style.AppTheme_Ocean_Splash, 0xFFA6C8FF),
    CELADON("celadon", R.string.theme_celadon, R.style.AppTheme_Celadon, R.style.AppTheme_Celadon_Splash, 0xFF87D6BC),
    SAKURA("sakura", R.string.theme_sakura, R.style.AppTheme_Sakura, R.style.AppTheme_Sakura_Splash, 0xFFFFB0CB),
    LAVENDER("lavender", R.string.theme_lavender, R.style.AppTheme_Lavender, R.style.AppTheme_Lavender_Splash, 0xFFCFBDFE);

    public final String id;
    public final int labelRes;
    public final int styleRes;
    public final int splashStyleRes;
    public final int swatchColor;

    ThemeMode(String id, int labelRes, int styleRes, int splashStyleRes, int swatchColor) {
        this.id = id;
        this.labelRes = labelRes;
        this.styleRes = styleRes;
        this.splashStyleRes = splashStyleRes;
        this.swatchColor = swatchColor;
    }

    public static ThemeMode byId(String id) {
        if (id != null) {
            for (ThemeMode mode : values()) {
                if (mode.id.equals(id)) return mode;
            }
        }
        return AMBER;
    }
}
