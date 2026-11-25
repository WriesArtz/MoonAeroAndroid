package com.limelight.aero;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Immutable snapshot of the Frutiger Aero settings, plus the four colours
 * (sky top, sky middle, horizon, accent) derived from them.
 *
 * The preset names and colours mirror the desktop (Qt) version of the theme.
 */
public final class AeroPalette {
    public static final String PREF_ENABLED = "checkbox_aero_enabled";
    public static final String PREF_PRESET = "list_aero_preset";
    public static final String PREF_COLOR = "edit_aero_color";
    public static final String PREF_GLASS = "seekbar_aero_glass";
    public static final String PREF_BUBBLES = "checkbox_aero_bubbles";
    public static final String PREF_BLUR = "seekbar_aero_blur";
    public static final String PREF_WALLPAPER = "edit_aero_wallpaper";
    public static final String PREF_WALLPAPER_DIM = "seekbar_aero_wallpaper_dim";

    public static final String PRESET_CUSTOM = "custom";

    public static final boolean DEFAULT_ENABLED = true;
    public static final String DEFAULT_PRESET = "aqua";
    public static final String DEFAULT_COLOR = "#1c9bdc";
    public static final int DEFAULT_GLASS = 55;
    public static final boolean DEFAULT_BUBBLES = true;
    public static final int DEFAULT_BLUR = 60;
    public static final int DEFAULT_WALLPAPER_DIM = 25;
    public static final int WALLPAPER_DIM_MAX = 80;

    public static final int GLASS_MIN = 10;
    public static final int GLASS_MAX = 90;

    private static final int[][] PRESETS = {
            // top, mid, bottom, accent
            {0xff073b88, 0xff1c9bdc, 0xff4fd8c6, 0xff38c9f6}, // aqua
            {0xff12559e, 0xff3fa9e6, 0xff46b85a, 0xff7edc54}, // bliss
            {0xff1b2f7a, 0xffd6568f, 0xfff08a3c, 0xffffb04a}, // sunset
            {0xff06173a, 0xff0c6a86, 0xff1fd0a0, 0xff4af0c8}, // aurora
    };
    private static final String[] PRESET_KEYS = {"aqua", "bliss", "sunset", "aurora"};

    public final boolean enabled;
    public final boolean bubbles;
    /** Glass opacity, 0.1 (very clear) to 0.9 (nearly opaque). */
    public final float glass;
    /** Backdrop blur behind the glass, 0 (plain translucent glass) to 1. */
    public final float blur;
    /** Content URI of the wallpaper (image, GIF or video), or "" for the sky. */
    public final String wallpaper;
    /** How much the wallpaper is darkened, 0 to 0.8. */
    public final float wallpaperDim;
    public final int top;
    public final int mid;
    public final int bottom;
    public final int accent;

    private AeroPalette(boolean enabled, boolean bubbles, float glass, int[] scheme,
                        float blur, String wallpaper, float wallpaperDim) {
        this.blur = blur;
        this.wallpaper = enabled && wallpaper != null ? wallpaper : "";
        this.wallpaperDim = wallpaperDim;
        this.enabled = enabled;
        this.bubbles = enabled && bubbles;
        this.glass = glass;
        this.top = scheme[0];
        this.mid = scheme[1];
        this.bottom = scheme[2];
        this.accent = scheme[3];
    }

    /** Pure factory (no Android dependencies); invalid input falls back to the defaults. */
    public static AeroPalette fromValues(boolean enabled, String preset, String hex, int glassPercent, boolean bubbles) {
        return fromValues(enabled, preset, hex, glassPercent, bubbles, DEFAULT_BLUR, "", DEFAULT_WALLPAPER_DIM);
    }

    public static AeroPalette fromValues(boolean enabled, String preset, String hex, int glassPercent, boolean bubbles,
                                         int blurPercent, String wallpaper, int dimPercent) {
        int clamped = Math.max(GLASS_MIN, Math.min(GLASS_MAX, glassPercent));
        return new AeroPalette(enabled, bubbles, clamped / 100f, schemeFor(preset, hex),
                Math.max(0, Math.min(100, blurPercent)) / 100f, wallpaper,
                Math.max(0, Math.min(WALLPAPER_DIM_MAX, dimPercent)) / 100f);
    }

    public static AeroPalette read(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return fromValues(
                prefs.getBoolean(PREF_ENABLED, DEFAULT_ENABLED),
                prefs.getString(PREF_PRESET, DEFAULT_PRESET),
                prefs.getString(PREF_COLOR, DEFAULT_COLOR),
                prefs.getInt(PREF_GLASS, DEFAULT_GLASS),
                prefs.getBoolean(PREF_BUBBLES, DEFAULT_BUBBLES),
                prefs.getInt(PREF_BLUR, DEFAULT_BLUR),
                prefs.getString(PREF_WALLPAPER, ""),
                prefs.getInt(PREF_WALLPAPER_DIM, DEFAULT_WALLPAPER_DIM));
    }

    /** True for exactly "#rrggbb" (either case). */
    public static boolean isValidColor(String value) {
        return AeroColors.isHex(value);
    }

    /** True if the given SharedPreferences key belongs to the Aero theme. */
    public static boolean isAeroPreference(String key) {
        return PREF_ENABLED.equals(key) || PREF_PRESET.equals(key) || PREF_COLOR.equals(key)
                || PREF_GLASS.equals(key) || PREF_BUBBLES.equals(key)
                || PREF_BLUR.equals(key) || PREF_WALLPAPER.equals(key) || PREF_WALLPAPER_DIM.equals(key);
    }

    public static boolean isEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_ENABLED, DEFAULT_ENABLED);
    }

    private static int[] schemeFor(String preset, String hex) {
        for (int i = 0; i < PRESET_KEYS.length; i++) {
            if (PRESET_KEYS[i].equals(preset)) {
                return PRESETS[i];
            }
        }
        if (PRESET_CUSTOM.equals(preset)) {
            return customScheme(AeroColors.parseHex(hex, PRESETS[0][1]));
        }
        return PRESETS[0];
    }

    /** Builds a whole scheme out of the single colour chosen for the Custom preset. */
    static int[] customScheme(int base) {
        float[] hsl = new float[3];
        AeroColors.toHsl(base, hsl);
        float h = hsl[0];
        // Keep greys grey (graphite / silver look); otherwise make sure the scheme is colourful enough
        float s = hsl[1] < 0.1f ? hsl[1] : Math.max(0.45f, hsl[1]);
        return new int[] {
                AeroColors.fromHsl(h, s, 0.20f),
                AeroColors.fromHsl(h, s, 0.46f),
                AeroColors.fromHsl((h + 0.12f) % 1f, s, 0.52f),
                AeroColors.fromHsl(h, Math.min(1f, s + 0.1f), 0.62f),
        };
    }

    /** Cheap identity used to skip re-styling views that already have this exact look. */
    public int signature() {
        int result = 17;
        result = 31 * result + (enabled ? 1 : 0);
        result = 31 * result + (bubbles ? 1 : 0);
        result = 31 * result + Float.floatToIntBits(glass);
        result = 31 * result + top;
        result = 31 * result + mid;
        result = 31 * result + bottom;
        result = 31 * result + accent;
        result = 31 * result + Float.floatToIntBits(blur);
        result = 31 * result + wallpaper.hashCode();
        result = 31 * result + Float.floatToIntBits(wallpaperDim);
        return result;
    }
}
