package com.limelight.aero;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.limelight.R;

/**
 * Entry points of the Frutiger Aero theme. Every call is idempotent and cheap,
 * and when the theme is switched off the original look is restored.
 *
 * The theme only touches the menu screens (PC list, app list, settings, add-PC);
 * the streaming activity never calls into this class.
 */
public final class AeroTheme {
    private AeroTheme() {}

    // Layout constants, in dp
    private static final float PC_TILE_RADIUS = 18f;
    private static final float APP_TILE_RADIUS = 12f;
    private static final float TILE_INSET = 5f;       // room for the shadow and focus ring
    private static final float ORB_INSET = 4f;
    private static final float ORB_ICON_PADDING = 13f;
    private static final float PANE_RADIUS = 16f;

    private static final int[] ORB_BUTTON_IDS = {
            R.id.settingsButton, R.id.helpButton, R.id.manuallyAddPc
    };

    // ------------------------------------------------------------------ Activity

    /**
     * Paints the sky backdrop, adds the floating bubbles and turns the toolbar
     * buttons into glass orbs. Safe to call repeatedly (e.g. from onResume).
     *
     * @return true if the look changed since the last call (theme toggled or re-coloured),
     *         so callers can refresh their grids
     */
    public static boolean apply(Activity activity) {
        AeroPalette palette = AeroPalette.read(activity);
        Window window = activity.getWindow();
        View decor = window.getDecorView();
        ViewGroup content = (ViewGroup) activity.findViewById(android.R.id.content);
        float dp = activity.getResources().getDisplayMetrics().density;

        Object previous = decor.getTag(R.id.aero_style_signature);

        if (!palette.enabled) {
            if (previous != null) {
                // Theme was on: put everything back the way the stock app has it
                Drawable original = decor.getTag(R.id.aero_original_background) instanceof Drawable
                        ? (Drawable) decor.getTag(R.id.aero_original_background)
                        : resolveWindowBackground(activity);
                window.setBackgroundDrawable(original);
                decor.setTag(R.id.aero_style_signature, null);
                decor.setTag(R.id.aero_original_background, null);
                setBubbles(activity, content, false);
                for (int id : ORB_BUTTON_IDS) {
                    unstyleOrb(activity.findViewById(id));
                }
            }
            return previous != null;
        }

        int signature = palette.signature();
        if (previous == null) {
            // First time: remember the stock window background so it can be restored later
            decor.setTag(R.id.aero_original_background, resolveWindowBackground(activity));
        }
        boolean changed = previous == null || (Integer) previous != signature;
        if (changed) {
            window.setBackgroundDrawable(new AeroBackgroundDrawable(palette));
            decor.setTag(R.id.aero_style_signature, signature);
        }

        setBubbles(activity, content, palette.bubbles);

        for (int id : ORB_BUTTON_IDS) {
            View v = activity.findViewById(id);
            if (v != null) {
                styleOrb(v, palette, dp, signature);
            }
        }
        return changed;
    }

    private static Drawable resolveWindowBackground(Context context) {
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.windowBackground, tv, true)) {
            if (tv.resourceId != 0) {
                return context.getDrawable(tv.resourceId);
            }
            if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return new ColorDrawable(tv.data);
            }
        }
        return new ColorDrawable(0xff303030);
    }

    private static void setBubbles(Activity activity, ViewGroup content, boolean wanted) {
        if (content == null) {
            return;
        }
        View existing = content.findViewById(R.id.aero_bubble_view);
        if (wanted && existing == null) {
            AeroBubbleView bubbles = new AeroBubbleView(activity);
            bubbles.setId(R.id.aero_bubble_view);
            // Behind the real UI, in front of the window background
            content.addView(bubbles, 0, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        else if (!wanted && existing != null) {
            content.removeView(existing);
        }
    }

    // ------------------------------------------------------------------ Glass orbs (toolbar buttons)

    private static void styleOrb(View button, AeroPalette palette, float dp, int signature) {
        Object tagged = button.getTag(R.id.aero_style_signature);
        if (tagged != null && (Integer) tagged == signature) {
            return;
        }
        if (button.getTag(R.id.aero_original_background) == null) {
            // Remember the stock look the first time we touch this button
            button.setTag(R.id.aero_original_background, button.getBackground());
            button.setTag(R.id.aero_original_padding, new int[] {
                    button.getPaddingLeft(), button.getPaddingTop(),
                    button.getPaddingRight(), button.getPaddingBottom()});
        }
        button.setBackground(selector(palette.accent, palette.glass, dp, 0f, ORB_INSET, true, true));
        int pad = Math.round(ORB_ICON_PADDING * dp);
        button.setPadding(pad, pad, pad, pad);
        button.setTag(R.id.aero_style_signature, signature);
    }

    private static void unstyleOrb(View button) {
        if (button == null || button.getTag(R.id.aero_style_signature) == null) {
            return;
        }
        Object background = button.getTag(R.id.aero_original_background);
        if (background instanceof Drawable) {
            button.setBackground(((Drawable) background).getConstantState() != null
                    ? ((Drawable) background).getConstantState().newDrawable()
                    : (Drawable) background);
        }
        Object padding = button.getTag(R.id.aero_original_padding);
        if (padding instanceof int[]) {
            int[] p = (int[]) padding;
            button.setPadding(p[0], p[1], p[2], p[3]);
        }
        button.setTag(R.id.aero_style_signature, null);
        button.setTag(R.id.aero_original_background, null);
        button.setTag(R.id.aero_original_padding, null);
    }

    // ------------------------------------------------------------------ Grid tiles

    /**
     * Gives a PC / app grid tile a glass background (or removes it when the
     * theme is off). Called from {@code GenericGridAdapter.getView()}.
     */
    public static void styleTile(View tile, ViewGroup parent, boolean largeTile) {
        Context context = tile.getContext();
        AeroPalette palette = AeroPalette.read(context);
        Object tagged = tile.getTag(R.id.aero_style_signature);

        if (parent instanceof AbsListView) {
            styleGridSelector((AbsListView) parent, palette.enabled);
        }

        if (!palette.enabled) {
            if (tagged != null) {
                tile.setBackground(null);
                tile.setTag(R.id.aero_style_signature, null);
            }
            return;
        }

        int signature = palette.signature() * 31 + (largeTile ? 1 : 0);
        if (tagged != null && (Integer) tagged == signature) {
            return;
        }
        float dp = context.getResources().getDisplayMetrics().density;
        tile.setBackground(selector(palette.accent, palette.glass, dp,
                largeTile ? PC_TILE_RADIUS : APP_TILE_RADIUS, TILE_INSET, false, true));
        tile.setTag(R.id.aero_style_signature, signature);
    }

    /** The stock list selector would paint a grey square under our glass, so hide it while themed. */
    private static void styleGridSelector(AbsListView grid, boolean themed) {
        Object original = grid.getTag(R.id.aero_original_selector);
        if (themed) {
            if (original == null) {
                grid.setTag(R.id.aero_original_selector, grid.getSelector());
                grid.setSelector(new ColorDrawable(Color.TRANSPARENT));
            }
        }
        else if (original instanceof Drawable) {
            grid.setSelector((Drawable) original);
            grid.setTag(R.id.aero_original_selector, null);
        }
    }

    // ------------------------------------------------------------------ Settings pane

    /** Wraps the settings list in a pane of dark glass (or removes it when the theme is off). */
    public static void stylePane(View pane) {
        if (pane == null) {
            return;
        }
        Context context = pane.getContext();
        AeroPalette palette = AeroPalette.read(context);
        Object tagged = pane.getTag(R.id.aero_style_signature);

        if (!palette.enabled) {
            if (tagged != null) {
                Object original = pane.getTag(R.id.aero_original_background);
                pane.setBackground(original instanceof Drawable ? (Drawable) original : null);
                pane.setTag(R.id.aero_style_signature, null);
                pane.setTag(R.id.aero_original_background, null);
            }
            return;
        }

        int signature = palette.signature();
        if (tagged != null && (Integer) tagged == signature) {
            return;
        }
        if (tagged == null) {
            pane.setTag(R.id.aero_original_background, pane.getBackground());
        }
        float dp = context.getResources().getDisplayMetrics().density;
        pane.setBackground(new AeroGlassDrawable(AeroColors.darker(palette.mid, 1.35f),
                Math.max(0.6f, palette.glass), dp, PANE_RADIUS, 6f, false, false, false, false, false));
        pane.setTag(R.id.aero_style_signature, signature);
    }

    // ------------------------------------------------------------------ Helpers

    /** Builds the glass background with its pressed / selected / focused / hovered variants. */
    private static StateListDrawable selector(int tint, float density, float dp, float radiusDp,
                                              float insetDp, boolean orb, boolean shadow) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_pressed},
                new AeroGlassDrawable(tint, density, dp, radiusDp, insetDp, orb, shadow, false, true, false));
        states.addState(new int[] {android.R.attr.state_selected},
                new AeroGlassDrawable(tint, density, dp, radiusDp, insetDp, orb, shadow, false, false, true));
        states.addState(new int[] {android.R.attr.state_focused},
                new AeroGlassDrawable(tint, density, dp, radiusDp, insetDp, orb, shadow, false, false, true));
        states.addState(new int[] {android.R.attr.state_hovered},
                new AeroGlassDrawable(tint, density, dp, radiusDp, insetDp, orb, shadow, true, false, false));
        states.addState(new int[] {},
                new AeroGlassDrawable(tint, density, dp, radiusDp, insetDp, orb, shadow, false, false, false));
        return states;
    }
}
