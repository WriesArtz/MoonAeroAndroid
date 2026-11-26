package com.limelight.aero;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Glass finish drawn on top of opaque artwork (app box art): a soft colour wash, a sheen that fades
 * out, a faint inner glow and the specular rim. It lets artwork look like it sits under the glass.
 */
final class AeroGlossOverlay extends Drawable {
    private final int tint;
    private final float radius;
    private final float dp;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF body = new RectF();
    private final RectF tmp = new RectF();
    private Shader washShader;
    private Shader sheenShader;
    private Shader rimShader;

    AeroGlossOverlay(int tint, float radius, float dp) {
        this.tint = tint;
        this.radius = radius;
        this.dp = dp;
    }

    @Override
    protected void onBoundsChange(Rect b) {
        body.set(b);
        if (body.isEmpty()) {
            return;
        }
        washShader = new LinearGradient(0, body.top, 0, body.bottom,
                AeroColors.withAlpha(AeroColors.lighter(tint, 1.3f), 0.26f),
                AeroColors.withAlpha(AeroColors.darker(tint, 1.2f), 0.30f), Shader.TileMode.CLAMP);
        sheenShader = new LinearGradient(0, body.top, 0, body.bottom,
                new int[] {0x4dffffff, 0x00ffffff, 0x00ffffff, AeroColors.withAlpha(AeroColors.lighter(tint, 1.7f), 0.22f)},
                new float[] {0f, 0.42f, 0.8f, 1f}, Shader.TileMode.CLAMP);
        rimShader = new LinearGradient(body.left, body.top, body.right, body.bottom,
                new int[] {0xffffffff, 0xccffffff, 0x2effffff, 0xb3ffffff, 0xf2ffffff},
                new float[] {0f, 0.18f, 0.5f, 0.82f, 1f}, Shader.TileMode.CLAMP);
    }

    @Override
    public void draw(Canvas canvas) {
        if (body.isEmpty() || washShader == null) {
            return;
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(washShader);
        canvas.drawRoundRect(body, radius, radius, paint);
        paint.setShader(sheenShader);
        canvas.drawRoundRect(body, radius, radius, paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f * dp);
        for (int i = 0; i < 3; i++) {
            float off = (1f + i * 1.5f) * dp;
            tmp.set(body.left + off, body.top + off, body.right - off, body.bottom - off);
            paint.setColor(AeroColors.withAlpha(0xffffffff, 0.12f - i * 0.04f));
            canvas.drawRoundRect(tmp, Math.max(0f, radius - off), Math.max(0f, radius - off), paint);
        }

        paint.setStrokeWidth(1.5f * dp);
        paint.setShader(rimShader);
        tmp.set(body);
        tmp.inset(0.75f * dp, 0.75f * dp);
        canvas.drawRoundRect(tmp, Math.max(0f, radius - 0.75f * dp), Math.max(0f, radius - 0.75f * dp), paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
