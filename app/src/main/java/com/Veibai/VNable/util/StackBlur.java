package com.Veibai.VNable.util;

public final class StackBlur {

    private StackBlur() {
    }

    public static void blur(int[] pixels, int w, int h, int radius) {
        if (pixels == null || w <= 0 || h <= 0 || radius < 1
                || pixels.length < w * h) {
            return;
        }

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int[] r = new int[wh];
        int[] g = new int[wh];
        int[] b = new int[wh];
        int[] vmin = new int[Math.max(w, h)];
        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int[] dv = new int[256 * divsum];
        for (int i = 0; i < dv.length; i++) {
            dv[i] = i / divsum;
        }

        int yi = 0;
        int yw = 0;
        int[][] stack = new int[div][3];

        for (int y = 0; y < h; y++) {
            int rsum = 0;
            int gsum = 0;
            int bsum = 0;
            int rinsum = 0;
            int ginsum = 0;
            int binsum = 0;
            int routsum = 0;
            int goutsum = 0;
            int boutsum = 0;

            for (int i = -radius; i <= radius; i++) {
                int p = pixels[yi + Math.min(wm, Math.max(i, 0))];
                int[] sir = stack[i + radius];
                sir[0] = (p & 0x00ff0000) >> 16;
                sir[1] = (p & 0x0000ff00) >> 8;
                sir[2] = (p & 0x000000ff);
                int rbs = radius + 1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }

            int stackpointer = radius;
            for (int x = 0; x < w; x++) {
                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                int stackstart = stackpointer - radius + div;
                int[] sir = stack[stackstart % div];
                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                int p = pixels[yw + vmin[x]];
                sir[0] = (p & 0x00ff0000) >> 16;
                sir[1] = (p & 0x0000ff00) >> 8;
                sir[2] = (p & 0x000000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer % div];
                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }

        for (int x = 0; x < w; x++) {
            int rsum = 0;
            int gsum = 0;
            int bsum = 0;
            int rinsum = 0;
            int ginsum = 0;
            int binsum = 0;
            int routsum = 0;
            int goutsum = 0;
            int boutsum = 0;

            int yp = -radius * w;
            for (int i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;
                int[] sir = stack[i + radius];
                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];
                int rbs = radius + 1 - Math.abs(i);
                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }

            int stackpointer = radius;
            for (int y = 0; y < h; y++) {
                pixels[yi] = (0xff000000 & pixels[yi])
                        | (dv[rsum] << 16)
                        | (dv[gsum] << 8)
                        | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                int stackstart = stackpointer - radius + div;
                int[] sir = stack[stackstart % div];
                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + radius + 1, hm) * w;
                }
                int p = x + vmin[y];
                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer % div];
                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }
    }
}
