package com.limelight.aero;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;

/**
 * The blurred picture of the background that glass panels show through them.
 *
 * It is a tiny (~190 px) screen-sized render of sky + wallpaper (first frame for GIFs and
 * videos) + dimming, blurred once. Each glass drawable then draws the part of it that lies
 * behind itself, which is what makes it look like frosted glass without per-view blur work.
 */
final class AeroBackdrop {
    private AeroBackdrop() {}

    private static final int LONG_SIDE = 192;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static volatile Bitmap bitmap;
    private static volatile int windowWidth = 1;
    private static volatile int windowHeight = 1;
    private static String key = "";
    private static int generation = 0;

    static Bitmap bitmap() {
        return bitmap;
    }

    static int windowWidth() {
        return windowWidth;
    }

    static int windowHeight() {
        return windowHeight;
    }

    /**
     * Makes sure the backdrop matches the palette and screen size. The sky version is ready
     * immediately; the wallpaper version replaces it later, and {@code onChanged} then runs
     * on the main thread so views can redraw.
     */
    static void ensure(final Context context, final AeroPalette palette, final Runnable onChanged) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        final int w = Math.max(1, dm.widthPixels);
        final int h = Math.max(1, dm.heightPixels);
        String newKey = palette.top + ":" + palette.mid + ":" + palette.bottom + ":" + palette.blur
                + ":" + palette.wallpaper + ":" + palette.wallpaperDim + ":" + w + "x" + h;
        if (newKey.equals(key)) {
            return;
        }
        key = newKey;
        final int mine = ++generation;
        windowWidth = w;
        windowHeight = h;

        bitmap = render(palette, null, w, h);

        if (palette.wallpaper.length() > 0) {
            final Context app = context.getApplicationContext();
            final Uri uri = Uri.parse(palette.wallpaper);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final Bitmap still = AeroWallpaper.loadStill(app, uri, LONG_SIDE * 2);
                    if (still == null) {
                        return;
                    }
                    final Bitmap blurred = render(palette, still, w, h);
                    MAIN.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mine == generation) {
                                bitmap = blurred;
                                if (onChanged != null) {
                                    onChanged.run();
                                }
                            }
                        }
                    });
                }
            }, "aero-backdrop").start();
        }
    }

    private static Bitmap render(AeroPalette palette, Bitmap wallpaper, int w, int h) {
        float scale = (float) LONG_SIDE / Math.max(w, h);
        int sw = Math.max(8, Math.round(w * scale));
        int sh = Math.max(8, Math.round(h * scale));
        Bitmap out = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);

        AeroBackgroundDrawable sky = new AeroBackgroundDrawable(palette);
        sky.setBounds(0, 0, sw, sh);
        sky.draw(canvas);

        if (wallpaper != null) {
            // Centre-crop the wallpaper to the screen's aspect ratio
            float ww = wallpaper.getWidth();
            float wh = wallpaper.getHeight();
            float want = (float) sw / sh;
            Rect src;
            if (ww / wh > want) {
                int cw = Math.round(wh * want);
                src = new Rect(Math.round((ww - cw) / 2f), 0, Math.round((ww - cw) / 2f) + cw, (int) wh);
            }
            else {
                int ch = Math.round(ww / want);
                src = new Rect(0, Math.round((wh - ch) / 2f), (int) ww, Math.round((wh - ch) / 2f) + ch);
            }
            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
            canvas.drawBitmap(wallpaper, src, new Rect(0, 0, sw, sh), paint);
            if (palette.wallpaperDim > 0f) {
                canvas.drawColor(Color.argb(Math.round(palette.wallpaperDim * 255f), 0, 0, 0));
            }
        }

        if (palette.blur > 0f) {
            int radius = 1 + Math.round(palette.blur * 8f);
            int[] px = new int[sw * sh];
            out.getPixels(px, 0, sw, 0, 0, sw, sh);
            AeroBlur.blur(px, sw, sh, radius);
            out.setPixels(px, 0, sw, 0, 0, sw, sh);
        }
        return out;
    }
}
