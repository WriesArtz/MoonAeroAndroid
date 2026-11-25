package com.limelight.aero;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/**
 * A "liquid glass" Frutiger Aero surface.
 *
 *  - frosted: shows the blurred background (sky or wallpaper) that lies behind the view,
 *    clipped to a generously rounded shape
 *  - a light tint, a soft sheen that fades to nothing (no hard gloss band) and an inner glow
 *  - a specular rim, brightest at the top-left and bottom-right and faint along the sides
 *  - diffuse shadow and a soft glow instead of a hard focus ring
 *  - springy motion: it grows when hovered or selected and squashes when pressed, overshooting a
 *    little before settling, driven by the view state (so it works with touch, TV remote and mouse)
 *
 * Everything is drawn inside the drawable's bounds (shadow, glow and the spring's overshoot live in
 * the {@code inset} margin) so it survives parents that clip their children.
 */
final class AeroGlassDrawable extends Drawable {
    private static final float REST = 1f;
    private static final float HOVER = 1.035f;
    private static final float PRESS = 0.955f;

    private final int tint;
    private final float density;     // 0..1, how opaque the glass is
    private final boolean blur;      // show the blurred backdrop
    private final float dp;          // pixels per dp
    private final float radiusDp;    // corner radius; ignored for orbs
    private final float insetDp;     // margin kept free around the body
    private final boolean orb;
    private final boolean shadow;
    private final boolean springy;

    private boolean hovered;
    private boolean pressed;
    private boolean focused;
    private float spring = REST;
    private float glowAmount = 0f;   // 0..1, hover / focus glow
    private ValueAnimator animator;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final RectF body = new RectF();
    private final RectF tmp = new RectF();
    private final Rect srcRect = new Rect();
    private final Path clip = new Path();
    private final int[] location = new int[2];

    private Shader tintShader;
    private Shader sheenShader;
    private Shader rimShader;
    private float radius;

    AeroGlassDrawable(int tint, float density, boolean blur, float dp, float radiusDp, float insetDp,
                      boolean orb, boolean shadow, boolean springy) {
        this.tint = tint;
        this.density = density;
        this.blur = blur;
        this.dp = dp;
        this.radiusDp = radiusDp;
        this.insetDp = insetDp;
        this.orb = orb;
        this.shadow = shadow;
        this.springy = springy;
    }

    /** Opacity of the tint layer. Less is needed when the blurred backdrop already frosts the glass. */
    private float strength() {
        return blur ? 0.10f + 0.40f * density : 0.22f + 0.55f * density;
    }

    // ------------------------------------------------------------------ state / spring

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    protected boolean onStateChange(int[] state) {
        boolean newPressed = false;
        boolean newHovered = false;
        boolean newFocused = false;
        for (int s : state) {
            if (s == android.R.attr.state_pressed) {
                newPressed = true;
            }
            else if (s == android.R.attr.state_hovered) {
                newHovered = true;
            }
            else if (s == android.R.attr.state_selected || s == android.R.attr.state_focused) {
                newFocused = true;
            }
        }
        if (newPressed == pressed && newHovered == hovered && newFocused == focused) {
            return false;
        }
        pressed = newPressed;
        hovered = newHovered;
        focused = newFocused;

        final float targetSpring = !springy ? REST : (pressed ? PRESS : ((hovered || focused) ? HOVER : REST));
        final float targetGlow = (focused || pressed) ? 1f : (hovered ? 0.55f : 0f);
        final float fromSpring = spring;
        final float fromGlow = glowAmount;
        if (animator != null) {
            animator.cancel();
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        // Pressing is quick and tight; letting go is slow and bouncy (that is the "liquid" feel)
        animator.setDuration(pressed ? 110 : 520);
        animator.setInterpolator(pressed ? new android.view.animation.DecelerateInterpolator()
                                         : new OvershootInterpolator(2.2f));
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                float t = (Float) a.getAnimatedValue();
                spring = fromSpring + (targetSpring - fromSpring) * t;
                glowAmount = Math.max(0f, Math.min(1f, fromGlow + (targetGlow - fromGlow) * Math.min(1f, t)));
                invalidateSelf();
            }
        });
        animator.start();
        invalidateSelf();
        return true;
    }

    // ------------------------------------------------------------------ geometry

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

        float a = strength();
        tintShader = new LinearGradient(0, body.top, 0, body.bottom,
                new int[] {
                        AeroColors.withAlpha(AeroColors.lighter(tint, 1.25f), a),
                        AeroColors.withAlpha(tint, a * 0.85f),
                        AeroColors.withAlpha(AeroColors.darker(tint, 1.35f), Math.min(1f, a * 1.05f + 0.04f)),
                },
                new float[] {0f, 0.55f, 1f}, Shader.TileMode.CLAMP);

        // Sheen: fades smoothly to nothing, so there is no visible band edge
        sheenShader = new LinearGradient(0, body.top, 0, body.bottom,
                new int[] {0x33ffffff, 0x00ffffff, 0x00ffffff,
                           AeroColors.withAlpha(AeroColors.lighter(tint, 1.6f), 0.14f)},
                new float[] {0f, 0.45f, 0.85f, 1f}, Shader.TileMode.CLAMP);

        // Specular rim: bright top-left and bottom-right, faint in between
        rimShader = new LinearGradient(body.left, body.top, body.right, body.bottom,
                new int[] {0xffffffff, 0xccffffff, 0x2effffff, 0xb3ffffff, 0xf2ffffff},
                new float[] {0f, 0.18f, 0.5f, 0.82f, 1f}, Shader.TileMode.CLAMP);
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void draw(Canvas canvas) {
        if (body.isEmpty() || tintShader == null) {
            return;
        }
        int save = canvas.save();
        canvas.scale(spring, spring, body.centerX(), body.centerY());

        // Soft diffuse shadow: stacked translucent shapes that fade outwards
        if (shadow) {
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0x13001018);
            for (int i = 4; i >= 0; i--) {
                float grow = (i * 1.6f + 0.5f) * dp;
                tmp.set(body.left - grow, body.top - grow + 2.2f * dp, body.right + grow, body.bottom + grow + 2.2f * dp);
                canvas.drawRoundRect(tmp, radius + grow, radius + grow, paint);
            }
        }

        // Glow around the glass when hovered or selected
        if (glowAmount > 0.01f) {
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
            for (int i = 3; i >= 0; i--) {
                float grow = (1.5f + i * 1.4f) * dp;
                paint.setColor(AeroColors.withAlpha(AeroColors.lighter(tint, 1.6f), (0.12f - i * 0.025f) * glowAmount));
                tmp.set(body.left - grow, body.top - grow, body.right + grow, body.bottom + grow);
                canvas.drawRoundRect(tmp, radius + grow, radius + grow, paint);
            }
        }

        drawBackdrop(canvas);

        // Tint
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(tintShader);
        canvas.drawRoundRect(body, radius, radius, paint);

        // Sheen
        paint.setShader(sheenShader);
        paint.setAlpha(Math.round(255f * (0.5f + 0.5f * density)));
        canvas.drawRoundRect(body, radius, radius, paint);
        paint.setAlpha(255);
        paint.setShader(null);

        // Inner glow: nested hairlines that fade towards the centre
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f * dp);
        for (int i = 0; i < 3; i++) {
            float off = (1f + i * 1.5f) * dp;
            tmp.set(body.left + off, body.top + off, body.right - off, body.bottom - off);
            if (tmp.width() <= 0 || tmp.height() <= 0) {
                break;
            }
            paint.setColor(AeroColors.withAlpha(0xffffffff, 0.11f - i * 0.035f));
            float r = Math.max(0f, radius - off);
            canvas.drawRoundRect(tmp, r, r, paint);
        }

        // Pressed: dim softly
        if (pressed) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0x33000a14);
            canvas.drawRoundRect(body, radius, radius, paint);
        }

        // Specular rim
        float rimWidth = (focused ? 2f : 1.5f) * dp;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(rimWidth);
        paint.setShader(rimShader);
        paint.setAlpha(Math.round(255f * (focused ? 1f : (0.72f + 0.2f * glowAmount))));
        tmp.set(body);
        tmp.inset(rimWidth / 2f, rimWidth / 2f);
        float rr = Math.max(0f, radius - rimWidth / 2f);
        canvas.drawRoundRect(tmp, rr, rr, paint);
        paint.setAlpha(255);
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);

        canvas.restoreToCount(save);
    }

    /** Draws the part of the blurred background that sits behind this view, clipped to the glass shape. */
    private void drawBackdrop(Canvas canvas) {
        Bitmap backdrop = blur ? AeroBackdrop.bitmap() : null;
        if (backdrop == null) {
            return;
        }
        Callback callback = getCallback();
        if (!(callback instanceof View)) {
            return;
        }
        View view = (View) callback;
        view.getLocationInWindow(location);
        float wx = AeroBackdrop.windowWidth();
        float wy = AeroBackdrop.windowHeight();
        float sx = backdrop.getWidth() / wx;
        float sy = backdrop.getHeight() / wy;
        // Account for the spring scale: the view is drawn scaled around the centre of the body
        float left = location[0] + (body.left - getBounds().left);
        float top = location[1] + (body.top - getBounds().top);
        srcRect.set(Math.round(left * sx), Math.round(top * sy),
                Math.round((left + body.width()) * sx), Math.round((top + body.height()) * sy));
        if (srcRect.width() < 1 || srcRect.height() < 1) {
            return;
        }
        int save = canvas.save();
        clip.reset();
        clip.addRoundRect(body, radius, radius, Path.Direction.CW);
        canvas.clipPath(clip);
        canvas.drawBitmap(backdrop, srcRect, body, bitmapPaint);
        canvas.restoreToCount(save);
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
