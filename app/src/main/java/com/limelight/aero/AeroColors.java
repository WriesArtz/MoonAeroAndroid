package com.limelight.aero;

/**
 * Small colour toolbox for the Frutiger Aero theme.
 *
 * Deliberately free of android.* imports so the maths can be unit-tested on a plain JVM.
 * Colours are packed ARGB ints, exactly like android.graphics.Color.
 */
final class AeroColors {
    private AeroColors() {}

    static int argb(int a, int r, int g, int b) {
        return (clamp255(a) << 24) | (clamp255(r) << 16) | (clamp255(g) << 8) | clamp255(b);
    }

    static int alpha(int c) { return (c >>> 24) & 0xff; }
    static int red(int c)   { return (c >>> 16) & 0xff; }
    static int green(int c) { return (c >>> 8) & 0xff; }
    static int blue(int c)  { return c & 0xff; }

    private static int clamp255(int v) {
        return v < 0 ? 0 : Math.min(v, 255);
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }

    /** Returns the colour with its alpha replaced by {@code a} (0..1). */
    static int withAlpha(int c, float a) {
        return (Math.round(clamp01(a) * 255f) << 24) | (c & 0x00ffffff);
    }

    /** Returns the colour with its alpha multiplied by {@code f} (0..1). */
    static int scaleAlpha(int c, float f) {
        return withAlpha(c, (alpha(c) / 255f) * f);
    }

    static int blend(int from, int to, float t) {
        t = clamp01(t);
        return argb(Math.round(alpha(from) + (alpha(to) - alpha(from)) * t),
                Math.round(red(from) + (red(to) - red(from)) * t),
                Math.round(green(from) + (green(to) - green(from)) * t),
                Math.round(blue(from) + (blue(to) - blue(from)) * t));
    }

    // ------------------------------------------------------------------ HSV

    /** Fills {h (0..1), s, v} for the colour. */
    private static void toHsv(int c, float[] out) {
        float r = red(c) / 255f, g = green(c) / 255f, b = blue(c) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h;
        if (d == 0f) {
            h = 0f;
        } else if (max == r) {
            h = ((g - b) / d) % 6f;
        } else if (max == g) {
            h = (b - r) / d + 2f;
        } else {
            h = (r - g) / d + 4f;
        }
        h /= 6f;
        if (h < 0f) h += 1f;
        out[0] = h;
        out[1] = max == 0f ? 0f : d / max;
        out[2] = max;
    }

    private static int fromHsv(float h, float s, float v, int a) {
        h = h - (float) Math.floor(h);
        float c = v * s;
        float x = c * (1f - Math.abs((h * 6f) % 2f - 1f));
        float m = v - c;
        float r, g, b;
        int sector = (int) (h * 6f);
        switch (sector) {
            case 0:  r = c; g = x; b = 0; break;
            case 1:  r = x; g = c; b = 0; break;
            case 2:  r = 0; g = c; b = x; break;
            case 3:  r = 0; g = x; b = c; break;
            case 4:  r = x; g = 0; b = c; break;
            default: r = c; g = 0; b = x; break;
        }
        return argb(a, Math.round((r + m) * 255f), Math.round((g + m) * 255f), Math.round((b + m) * 255f));
    }

    /**
     * Same behaviour as Qt's Qt.lighter(): scales the HSV value, and lets any
     * overflow wash out the saturation instead of clipping.
     */
    static int lighter(int c, float factor) {
        float[] hsv = new float[3];
        toHsv(c, hsv);
        float v = hsv[2] * factor;
        float s = hsv[1];
        if (v > 1f) {
            s = Math.max(0f, s - (v - 1f));
            v = 1f;
        }
        return fromHsv(hsv[0], s, v, alpha(c));
    }

    /** Same behaviour as Qt's Qt.darker(): divides the HSV value. */
    static int darker(int c, float factor) {
        float[] hsv = new float[3];
        toHsv(c, hsv);
        return fromHsv(hsv[0], hsv[1], hsv[2] / factor, alpha(c));
    }

    // ------------------------------------------------------------------ HSL

    /** Fills {h (0..1), s, l} for the colour. Hue is 0 for greys. */
    static void toHsl(int c, float[] out) {
        float r = red(c) / 255f, g = green(c) / 255f, b = blue(c) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float l = (max + min) / 2f;
        float h;
        float s;
        if (d == 0f) {
            h = 0f;
            s = 0f;
        } else {
            s = d / (1f - Math.abs(2f * l - 1f));
            if (max == r) {
                h = ((g - b) / d) % 6f;
            } else if (max == g) {
                h = (b - r) / d + 2f;
            } else {
                h = (r - g) / d + 4f;
            }
            h /= 6f;
            if (h < 0f) h += 1f;
        }
        out[0] = h;
        out[1] = s;
        out[2] = l;
    }

    static int fromHsl(float h, float s, float l) {
        h = h - (float) Math.floor(h);
        s = clamp01(s);
        l = clamp01(l);
        float c = (1f - Math.abs(2f * l - 1f)) * s;
        float x = c * (1f - Math.abs((h * 6f) % 2f - 1f));
        float m = l - c / 2f;
        float r, g, b;
        int sector = (int) (h * 6f);
        switch (sector) {
            case 0:  r = c; g = x; b = 0; break;
            case 1:  r = x; g = c; b = 0; break;
            case 2:  r = 0; g = c; b = x; break;
            case 3:  r = 0; g = x; b = c; break;
            case 4:  r = x; g = 0; b = c; break;
            default: r = c; g = 0; b = x; break;
        }
        return argb(255, Math.round((r + m) * 255f), Math.round((g + m) * 255f), Math.round((b + m) * 255f));
    }

    // ------------------------------------------------------------------ Parsing

    /** True for strictly "#rrggbb". */
    static boolean isHex(String s) {
        if (s == null || s.length() != 7 || s.charAt(0) != '#') {
            return false;
        }
        for (int i = 1; i < 7; i++) {
            char ch = s.charAt(i);
            boolean ok = (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F');
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /** Parses "#rrggbb" into an opaque colour, or returns {@code fallback} when it is not valid. */
    static int parseHex(String s, int fallback) {
        if (!isHex(s)) {
            return fallback;
        }
        return 0xff000000 | Integer.parseInt(s.substring(1), 16);
    }
}
