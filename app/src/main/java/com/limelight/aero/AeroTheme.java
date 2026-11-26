package com.limelight.aero;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.TypedValue;
import android.view.View;
import android.graphics.Outline;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
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
    private static final float PC_TILE_RADIUS = 26f;
    private static final float APP_TILE_RADIUS = 20f;
    private static final float TILE_INSET = 7f;       // room for the shadow, glow and spring overshoot
    private static final float ORB_INSET = 6f;
    private static final float ORB_ICON_PADDING = 13f;
    private static final float PANE_RADIUS = 24f;

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
                setWallpaper(activity, content, null);
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

        setWallpaper(activity, content, palette);
        setBubbles(activity, content, palette.bubbles);
        AeroBackdrop.ensure(activity, palette, new Runnable() {
            @Override
            public void run() {
                redrawGlass(decor);
            }
        });

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

    /** Adds, updates or removes the wallpaper layer (image / GIF / video). It sits behind the bubbles. */
    private static void setWallpaper(Activity activity, ViewGroup content, AeroPalette palette) {
        if (content == null) {
            return;
        }
        View existing = content.findViewById(R.id.aero_wallpaper_view);
        boolean wanted = palette != null && palette.wallpaper.length() > 0;
        if (!wanted) {
            if (existing != null) {
                content.removeView(existing);
            }
            return;
        }
        AeroWallpaperView view;
        if (existing instanceof AeroWallpaperView) {
            view = (AeroWallpaperView) existing;
        }
        else {
            view = new AeroWallpaperView(activity);
            view.setId(R.id.aero_wallpaper_view);
            content.addView(view, 0, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        view.setSource(Uri.parse(palette.wallpaper), palette.wallpaperDim);
    }

    /** Glass views cache their drawing, so ask the themed ones to repaint (the blurred backdrop changed). */
    private static void redrawGlass(View view) {
        if (view.getTag(R.id.aero_style_signature) != null) {
            view.invalidate();
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                redrawGlass(group.getChildAt(i));
            }
        }
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
        button.setBackground(glass(palette.accent, palette, dp, 0f, ORB_INSET, true, true, true));
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
                tile.setScaleX(1f);
                tile.setScaleY(1f);
                styleArtwork(tile, null, 0f, 0f);
                tile.setTag(R.id.aero_style_signature, null);
            }
            return;
        }

        int signature = palette.signature() * 31 + (largeTile ? 1 : 0);
        if (tagged != null && (Integer) tagged == signature) {
            return;
        }
        float dp = context.getResources().getDisplayMetrics().density;
        tile.setScaleX(1f);
        tile.setScaleY(1f);
        tile.setBackground(glass(palette.accent, palette, dp,
                largeTile ? PC_TILE_RADIUS : APP_TILE_RADIUS, TILE_INSET, false, true, true));
        // App tiles are opaque box art: round it and put the glass finish on top of it
        styleArtwork(tile, largeTile ? null : palette, dp, APP_TILE_RADIUS - 3f);
        tile.setTag(R.id.aero_style_signature, signature);
    }

    /** Keeps the layout listener and overlay of one tile together so they can be removed again. */
    private static final class TileOverlay implements View.OnLayoutChangeListener {
        final View art;
        final AeroGlossOverlay overlay;

        TileOverlay(View art, AeroGlossOverlay overlay) {
            this.art = art;
            this.overlay = overlay;
        }

        @Override
        public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
            overlay.setBounds(art.getLeft(), art.getTop(), art.getRight(), art.getBottom());
        }
    }

    /**
     * Rounds the artwork of an app tile and adds the gloss overlay; with a null palette it undoes both.
     */
    private static void styleArtwork(View tile, AeroPalette palette, float dp, final float radiusDp) {
        Object old = tile.getTag(R.id.aero_tile_overlay);
        if (old instanceof TileOverlay) {
            tile.removeOnLayoutChangeListener((TileOverlay) old);
            tile.getOverlay().remove(((TileOverlay) old).overlay);
            tile.setTag(R.id.aero_tile_overlay, null);
        }
        View art = tile.findViewById(R.id.grid_image);
        if (!(art instanceof ImageView)) {
            return;
        }
        if (palette == null) {
            art.setClipToOutline(false);
            art.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            return;
        }
        final float radius = radiusDp * dp;
        art.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        art.setClipToOutline(true);

        AeroGlossOverlay gloss = new AeroGlossOverlay(palette.accent, radius, dp);
        TileOverlay holder = new TileOverlay(art, gloss);
        tile.getOverlay().add(gloss);
        tile.addOnLayoutChangeListener(holder);
        tile.setTag(R.id.aero_tile_overlay, holder);
        if (art.getWidth() > 0) {
            gloss.setBounds(art.getLeft(), art.getTop(), art.getRight(), art.getBottom());
        }
    }

    /** The stock list selector would paint a grey square under our glass, so hide it while themed. */
    private static void styleGridSelector(AbsListView grid, boolean themed) {
        Object original = grid.getTag(R.id.aero_original_selector);
        if (themed) {
            if (original == null) {
                grid.setTag(R.id.aero_original_selector, grid.getSelector());
                grid.setSelector(new ColorDrawable(Color.TRANSPARENT));
                // The frosted backdrop depends on where a tile is on screen, so repaint tiles while scrolling
                grid.setOnScrollListener(new AbsListView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(AbsListView view, int scrollState) {
                    }

                    @Override
                    public void onScroll(AbsListView view, int first, int visible, int total) {
                        for (int i = 0; i < view.getChildCount(); i++) {
                            view.getChildAt(i).invalidate();
                        }
                    }
                });
            }
        }
        else if (original instanceof Drawable) {
            grid.setSelector((Drawable) original);
            grid.setOnScrollListener(null);
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
                Math.max(0.6f, palette.glass), palette.blur > 0f, dp, PANE_RADIUS, 6f, false, false, false));
        pane.setTag(R.id.aero_style_signature, signature);
    }

    // ------------------------------------------------------------------ Helpers

    /** Builds one glass background; it follows the view state (pressed / selected / focused / hovered) itself. */
    private static Drawable glass(int tint, AeroPalette palette, float dp, float radiusDp,
                                  float insetDp, boolean orb, boolean shadow, boolean springy) {
        return new AeroGlassDrawable(tint, palette.glass, palette.blur > 0f, dp, radiusDp, insetDp,
                orb, shadow, springy);
    }
}
