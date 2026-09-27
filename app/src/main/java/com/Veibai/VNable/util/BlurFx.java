package com.Veibai.VNable.util;

import android.graphics.Bitmap;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.widget.ImageView;

public final class BlurFx {

    private static final float RENDER_BLUR_DP = 24f;

    private static final int FALLBACK_DOWNSCALE = 4;

    private static final int FALLBACK_RADIUS = 16;

    private BlurFx() {
    }

    public static void blurImage(ImageView view) {
        if (view == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            float radius = RENDER_BLUR_DP
                    * view.getResources().getDisplayMetrics().density;
            view.setRenderEffect(RenderEffect.createBlurEffect(
                    radius, radius, Shader.TileMode.CLAMP));
            return;
        }
        Bitmap blurred = fallbackBlurred(view);
        if (blurred != null) view.setImageBitmap(blurred);
    }

    private static Bitmap fallbackBlurred(ImageView view) {
        if (!(view.getDrawable() instanceof BitmapDrawable)) return null;
        Bitmap src = ((BitmapDrawable) view.getDrawable()).getBitmap();
        if (src == null || src.isRecycled()) return null;

        int targetW = Math.max(1, view.getResources()
                .getDisplayMetrics().widthPixels / FALLBACK_DOWNSCALE);
        Bitmap work = src;
        if (work.getWidth() > targetW) {
            int targetH = Math.max(1, (int) ((long) work.getHeight()
                    * targetW / work.getWidth()));
            Bitmap scaled = Bitmap.createScaledBitmap(src, targetW, targetH, true);
            if (scaled != null && scaled != src) work = scaled;
        }
        Bitmap mutable = work.copy(Bitmap.Config.ARGB_8888, true);
        if (mutable == null) return null;

        int[] pixels = new int[mutable.getWidth() * mutable.getHeight()];
        mutable.getPixels(pixels, 0, mutable.getWidth(), 0, 0,
                mutable.getWidth(), mutable.getHeight());
        StackBlur.blur(pixels, mutable.getWidth(), mutable.getHeight(), FALLBACK_RADIUS);
        mutable.setPixels(pixels, 0, mutable.getWidth(), 0, 0,
                mutable.getWidth(), mutable.getHeight());
        return mutable;
    }
}
