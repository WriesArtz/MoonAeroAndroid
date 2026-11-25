package com.limelight.aero;

/**
 * Small, dependency-free box blur on ARGB pixel arrays. Three passes of a box
 * blur approximate a Gaussian closely enough for frosted glass, and it works on
 * every Android version (RenderEffect needs API 31, RenderScript is deprecated).
 * It is only ever run on a tiny (~190 px) copy of the background.
 */
final class AeroBlur {
    private AeroBlur() {}

    /** Blurs {@code px} (width * height ARGB pixels) in place. */
    static void blur(int[] px, int width, int height, int radius) {
        if (radius < 1 || width < 1 || height < 1 || px.length < width * height) {
            return;
        }
        int[] tmp = new int[width * height];
        for (int i = 0; i < 3; i++) {
            pass(px, tmp, width, height, radius, true);
            pass(tmp, px, width, height, radius, false);
        }
    }

    private static int clamp(int v, int max) {
        return v < 0 ? 0 : (v > max ? max : v);
    }

    private static void pass(int[] in, int[] out, int w, int h, int r, boolean horizontal) {
        int len = horizontal ? w : h;
        int lines = horizontal ? h : w;
        int step = horizontal ? 1 : w;
        int win = 2 * r + 1;
        int last = len - 1;
        for (int line = 0; line < lines; line++) {
            int base = horizontal ? line * w : line;
            int a = 0, rr = 0, g = 0, b = 0;
            for (int k = -r; k <= r; k++) {
                int p = in[base + clamp(k, last) * step];
                a += p >>> 24;
                rr += (p >> 16) & 0xff;
                g += (p >> 8) & 0xff;
                b += p & 0xff;
            }
            for (int i = 0; i < len; i++) {
                int half = win / 2;   // round to nearest, otherwise every pass darkens the picture a little
                out[base + i * step] = (((a + half) / win) << 24) | (((rr + half) / win) << 16)
                        | (((g + half) / win) << 8) | ((b + half) / win);
                int add = in[base + clamp(i + r + 1, last) * step];
                int rem = in[base + clamp(i - r, last) * step];
                a += (add >>> 24) - (rem >>> 24);
                rr += ((add >> 16) & 0xff) - ((rem >> 16) & 0xff);
                g += ((add >> 8) & 0xff) - ((rem >> 8) & 0xff);
                b += (add & 0xff) - (rem & 0xff);
            }
        }
    }
}
