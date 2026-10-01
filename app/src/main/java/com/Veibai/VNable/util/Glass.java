package com.Veibai.VNable.util;

import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

public final class Glass {

    private Glass() {
    }

    public static void behind(Window window) {
        if (window == null) return;
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(0.55f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                WindowManager.LayoutParams lp = window.getAttributes();
                lp.setBlurBehindRadius(32);
                window.setAttributes(lp);

                window.setDimAmount(0.38f);
            } catch (Exception ignored) {

            }
        }
    }
}
