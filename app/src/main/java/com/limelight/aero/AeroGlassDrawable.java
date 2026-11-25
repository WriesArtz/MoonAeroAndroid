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
 * A glossy Frutiger Aero glass surface: tinted translucent body, bright gloss
 * highlight over the top half, warm glow along the bottom, a thin white rim and
 * a soft shadow. Port of AeroGlass.qml from the desktop theme.
 *
 * Everything is drawn inside the drawable's bounds (shadow and focus ring live
 * in the {@code inset} margin) so it survives parents that clip their children.
 */
final class AeroGlassDrawable extends Drawable {
    private final int tint;
    private final float density;     // 0..1, how opaque the glass is
    private final float dp;          // pixels per dp
    private final float radiusDp;    // corner radius; ignored for orbs
    private final float insetDp;     // margin kept free around the body
    private final boolean orb;
    private final boolean shadow;
    private final boolean hovered;
    private final boolean pressed;
    private final boolean focused;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF body = new RectF();
    private final RectF glow = new RectF();
    private final RectF gloss = new RectF();
    private final RectF tmp = new RectF();

    private Shader bodyShader;
    private Shader glowShader;
    private Shader glossShader;
    private float radius;
    private float innerRadius;

    AeroGlassDrawable(int tint, float density, float dp, float radiusDp, float insetDp,
                      boolean orb, boolean shadow, boolean hovered, boolean pressed, boolean focused) {
        this.tint = tint;
        this.density = density;
        this.dp = dp;
        this.radiusDp = radiusDp;
        this.insetDp = insetDp;
        this.orb = orb;
        this.shadow = shadow;
        this.hovered = hovered;
        this.pressed = pressed;
        this.focused = focused;
    }

    private float strength() {
        return 0.25f + 0.55f * density;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        float inset = insetDp * dp;
        body.set(bounds.left + inset, bounds.top + inset, bounds.right - inset, bounds.bottom - inset);
        if (body.width() <= 0 || body.height() <= 0) {
            body.setEmpty();
            return;
        }

        radius = orb ? Math.min(body.width(), body.height()) / 2f
                     : Math.min(radiusDp * dp, Math.min(body.width(), body.height()) / 2f);
        innerRadius = Math.max(0f, radius - 2f * dp);

        float a = strength();
        float hover = hovered ? 0.10f : 0f;
        bodyShader = new LinearGradient(0, body.top, 0, body.bottom,
                new int[] {
                        AeroColors.withAlpha(AeroColors.lighter(tint, 1.15f), a + hover),
                        AeroColors.withAlpha(tint, a + hover * 0.6f),
                        AeroColors.withAlpha(AeroColors.darker(tint, 1.45f), Math.min(1f, a + 0.12f)),
                },
                new float[] {0f, 0.5f, 1f}, Shader.TileMode.CLAMP);

        // Warm glow along the bottom edge (bottom 32%)
        glow.set(body.left + 2f * dp, body.bottom - body.height() * 0.32f - 2f * dp,
                body.right - 2f * dp, body.bottom - 2f * dp);
        glowShader = new LinearGradient(0, glow.top, 0, glow.bottom,
                AeroColors.withAlpha(AeroColors.lighter(tint, 1.4f), 0f),
                AeroColors.withAlpha(AeroColors.lighter(tint, 1.4f), 0.28f),
                Shader.TileMode.CLAMP);

        // Gloss highlight over the top half
        gloss.set(body.left + 2f * dp, body.top + 2f * dp,
                body.right - 2f * dp, body.top + body.height() * 0.5f);
        glossShader = new LinearGradient(0, gloss.top, 0, gloss.bottom,
                0x8cffffff,   // white at 55%
                0x0fffffff,   // white at 6%
                Shader.TileMode.CLAMP);
    }

    @Override
    public void draw(Canvas canvas) {
        if (body.isEmpty() || bodyShader == null) {
            return;
        }

        // Soft drop shadow: two stacked translucent shapes
        if (shadow) {
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);

            paint.setColor(0x1f000000);
            tmp.set(body);
            tmp.inset(-1f * dp, -1f * dp);
            tmp.offset(0f, 1.5f * dp);
            canvas.drawRoundRect(tmp, radius + dp, radius + dp, paint);

            paint.setColor(0x2e000000);
            tmp.set(body);
            tmp.offset(0f, 1f * dp);
            canvas.drawRoundRect(tmp, radius, radius, paint);
        }

        // Tinted body
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(bodyShader);
        canvas.drawRoundRect(body, radius, radius, paint);

        // Glow
        paint.setShader(glowShader);
        canvas.drawRoundRect(glow, innerRadius, innerRadius, paint);

        // Gloss, slightly stronger on opaque glass
        paint.setShader(glossShader);
        paint.setAlpha(Math.round(255f * Math.min(1f, 0.55f + 0.45f * density)));
        canvas.drawRoundRect(gloss, innerRadius, innerRadius, paint);
        paint.setAlpha(255);
        paint.setShader(null);

        // Pressed state darkens the whole surface
        if (pressed) {
            paint.setColor(0x40000000);
            canvas.drawRoundRect(body, radius, radius, paint);
        }

        // Rim
        float rimWidth = (focused ? 2f : 1f) * dp;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(rimWidth);
        paint.setColor(focused ? 0xffffffff : 0x8cffffff);
        tmp.set(body);
        tmp.inset(rimWidth / 2f, rimWidth / 2f);
        canvas.drawRoundRect(tmp, Math.max(0f, radius - rimWidth / 2f), Math.max(0f, radius - rimWidth / 2f), paint);

        // Selection ring for keyboard / gamepad / TV-remote navigation
        if (focused) {
            float ringWidth = 3f * dp;
            Rect b = getBounds();
            paint.setStrokeWidth(ringWidth);
            paint.setColor(AeroColors.withAlpha(AeroColors.lighter(tint, 1.5f), 0.85f));
            tmp.set(b.left + ringWidth / 2f, b.top + ringWidth / 2f, b.right - ringWidth / 2f, b.bottom - ringWidth / 2f);
            float ringRadius = radius + (body.left - tmp.left);
            canvas.drawRoundRect(tmp, ringRadius, ringRadius, paint);
        }

        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void setAlpha(int alpha) {
        // The glass is already translucent by design; a constant alpha is not supported.
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        // Not supported
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
