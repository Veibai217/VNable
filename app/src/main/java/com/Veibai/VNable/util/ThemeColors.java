package com.Veibai.VNable.util;

import android.content.Context;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;

public final class ThemeColors {

    private ThemeColors() {
    }

    public static int resolve(Context context, int attr) {
        TypedValue value = new TypedValue();
        if (!context.getTheme().resolveAttribute(attr, value, true)) {
            return 0;
        }
        if (value.type == TypedValue.TYPE_STRING) {
            return value.resourceId != 0
                    ? ContextCompat.getColor(context, value.resourceId) : 0;
        }
        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return value.data;
        }
        return value.resourceId != 0
                ? ContextCompat.getColor(context, value.resourceId) : 0;
    }
}
