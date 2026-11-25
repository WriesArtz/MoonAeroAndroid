package com.limelight.aero;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Frutiger Aero backdrop: sky gradient, soft sun glow, light rays and two
 * rolling hills. Port of AeroBackground.qml (the floating bubbles are drawn by
 * {@link AeroBubbleView} so they can animate without redrawing this).
 */
final class AeroBackgroundDrawable extends Drawable {
    private static final float RAY_ROTATION_DEG = 24f;

    private final AeroPalette palette;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Shader skyShader;
    private Shader sunShader;
    private float sunX;
    private float sunY;
    private float sunRadius;

    private final Shader[] rayShaders = new Shader[3];
    private final float[][] rays = new float[3][];   // left, top, right, bottom

    private Shader hill1Shader;
    private Shader hill2Shader;
    private float[] hill1;   // cx, cy, r
    private float[] hill2;

    AeroBackgroundDrawable(AeroPalette palette) {
        this.palette = palette;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        float w = bounds.width();
        float h = bounds.height();
        if (w <= 0 || h <= 0) {
            return;
        }
        float l = bounds.left;
        float t = bounds.top;

        skyShader = new LinearGradient(0, t, 0, t + h,
                new int[] {palette.top, palette.mid, palette.bottom},
                new float[] {0f, 0.55f, 1f}, Shader.TileMode.CLAMP);

        // Sun glow, top-left (six translucent discs in the QML version)
        float base = Math.max(w, h) * 0.55f;
        sunX = l + w * 0.18f;
        sunY = t - h * 0.10f + base * 0.15f;
        sunRadius = base / 2f;
        sunShader = new RadialGradient(sunX, sunY, sunRadius,
                new int[] {0x3dffffff, 0x0bffffff, 0x00ffffff},
                new float[] {0f, 0.9f, 1f}, Shader.TileMode.CLAMP);

        // Light rays
        for (int i = 0; i < 3; i++) {
            float rw = w * (0.10f + i * 0.05f);
            float rh = h * 1.6f;
            float rx = l + w * (0.05f + i * 0.17f);
            float ry = t - h * 0.3f;
            rays[i] = new float[] {rx, ry, rx + rw, ry + rh};
            rayShaders[i] = new LinearGradient(0, ry, 0, ry + rh, 0x24ffffff, 0x00ffffff, Shader.TileMode.CLAMP);
        }

        // Hills: the top arcs of two huge, mostly off-screen circles
        float r1 = w * 0.75f;
        float top1 = t + h - h * 0.13f;
        hill1 = new float[] {l - w * 0.35f + r1, top1 + r1, r1};
        hill1Shader = new LinearGradient(0, top1, 0, top1 + 2f * r1,
                new int[] {
                        AeroColors.withAlpha(AeroColors.lighter(palette.bottom, 1.2f), 0.45f),
                        AeroColors.withAlpha(palette.bottom, 0.30f),
                        AeroColors.withAlpha(palette.bottom, 0f)},
                new float[] {0f, 0.2f, 1f}, Shader.TileMode.CLAMP);

        float r2 = w * 0.65f;
        float top2 = t + h - h * 0.08f;
        hill2 = new float[] {l + w * 0.30f + r2, top2 + r2, r2};
        hill2Shader = new LinearGradient(0, top2, 0, top2 + 2f * r2,
                new int[] {
                        AeroColors.withAlpha(AeroColors.darker(palette.bottom, 1.15f), 0.50f),
                        AeroColors.withAlpha(AeroColors.darker(palette.bottom, 1.3f), 0.30f),
                        AeroColors.withAlpha(palette.bottom, 0f)},
                new float[] {0f, 0.2f, 1f}, Shader.TileMode.CLAMP);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (skyShader == null || b.isEmpty()) {
            return;
        }

        int save = canvas.save();
        canvas.clipRect(b);

        paint.setStyle(Paint.Style.FILL);

        paint.setShader(skyShader);
        canvas.drawRect(b, paint);

        paint.setShader(sunShader);
        canvas.drawCircle(sunX, sunY, sunRadius, paint);

        for (int i = 0; i < rays.length; i++) {
            float[] r = rays[i];
            int rs = canvas.save();
            canvas.rotate(RAY_ROTATION_DEG, (r[0] + r[2]) / 2f, r[1]);
            paint.setShader(rayShaders[i]);
            canvas.drawRect(r[0], r[1], r[2], r[3], paint);
            canvas.restoreToCount(rs);
        }

        paint.setShader(hill1Shader);
        canvas.drawCircle(hill1[0], hill1[1], hill1[2], paint);
        paint.setShader(hill2Shader);
        canvas.drawCircle(hill2[0], hill2[1], hill2[2], paint);

        paint.setShader(null);
        canvas.restoreToCount(save);
    }

    @Override
    public void setAlpha(int alpha) {
        // Opaque backdrop: constant alpha is not supported.
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        // Not supported
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
