package com.limelight.aero;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;

/**
 * Slowly rising soap bubbles drawn over the Aero backdrop. Purely decorative:
 * it never takes input or focus, only animates while the window is visible,
 * and freezes if the user turned animations off in the system settings.
 */
final class AeroBubbleView extends View {
    private static final int BUBBLE_COUNT = 14;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint spark = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final float dp;
    private boolean animate = true;

    AeroBubbleView(Context context) {
        super(context);
        dp = context.getResources().getDisplayMetrics().density;

        setWillNotDraw(false);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        fill.setStyle(Paint.Style.FILL);
        fill.setColor(0x12ffffff);                 // white at 7%
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(dp);
        rim.setColor(0x8cffffff);                  // white at 55%
        spark.setStyle(Paint.Style.FILL);
        spark.setColor(0x8cffffff);
    }

    private boolean shouldRun() {
        return animate && isShown() && getWindowVisibility() == VISIBLE;
    }

    private void refreshAnimationScale() {
        try {
            animate = Settings.Global.getFloat(getContext().getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
        } catch (RuntimeException e) {
            animate = true;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        refreshAnimationScale();
        postInvalidateOnAnimation();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            refreshAnimationScale();
            postInvalidateOnAnimation();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        // A frozen clock keeps the bubbles in a pleasant static arrangement
        long now = animate ? SystemClock.uptimeMillis() : 20000L;

        for (int i = 0; i < BUBBLE_COUNT; i++) {
            float size = (22 + (i * 37) % 70) * dp;
            float startX = ((i * 211) % 100) / 100f * w;
            long riseMs = 14000L + (i * 997L) % 12000L;
            long delayMs = (i * 733L) % 6000L;
            long swayMs = 3500L + (i * 313L) % 2500L;

            // Rise from just below the bottom edge to just above the top
            float progress = ((now + riseMs - delayMs) % riseMs) / (float) riseMs;
            float y = h + size - progress * (h + 2f * size);

            // Gentle left-right sway of +-28dp
            double phase = (now % swayMs) / (double) swayMs * 2.0 * Math.PI;
            float x = startX + (float) Math.sin(phase + i) * 28f * dp;

            float cx = x + size / 2f;
            float cy = y + size / 2f;
            canvas.drawCircle(cx, cy, size / 2f, fill);
            canvas.drawCircle(cx, cy, size / 2f - dp / 2f, rim);

            // Specular highlight
            int save = canvas.save();
            canvas.rotate(-30f, x + size * 0.38f, y + size * 0.22f);
            oval.set(x + size * 0.18f, y + size * 0.12f, x + size * 0.58f, y + size * 0.32f);
            canvas.drawOval(oval, spark);
            canvas.restoreToCount(save);
        }

        if (shouldRun()) {
            postInvalidateOnAnimation();
        }
    }
}
