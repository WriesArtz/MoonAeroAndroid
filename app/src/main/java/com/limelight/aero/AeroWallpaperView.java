package com.limelight.aero;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * The wallpaper layer: a still image, an animated GIF / WebP (Android 9+; older versions show the
 * first frame) or a looping silent video, cropped to fill the screen and darkened by the dim setting.
 * It pauses while its window is not focused, so it costs nothing while streaming, and if the file
 * cannot be shown it stays transparent, leaving the sky visible.
 */
final class AeroWallpaperView extends FrameLayout implements TextureView.SurfaceTextureListener {
    private static final int MAX_IMAGE_SIDE = 2048;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ImageView image;
    private final View dim;

    private Uri uri;
    private int load = 0;
    private Drawable drawable;
    private TextureView texture;
    private MediaPlayer player;
    private boolean playerReady;
    private int videoWidth;
    private int videoHeight;

    AeroWallpaperView(Context context) {
        super(context);
        image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        addView(image, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dim = new View(context);
        addView(dim, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    void setSource(Uri newUri, float dimAmount) {
        dim.setBackgroundColor(Color.argb(Math.round(Math.max(0f, Math.min(1f, dimAmount)) * 255f), 0, 0, 0));
        if (newUri.equals(uri)) {
            return;
        }
        uri = newUri;
        reload();
    }

    private void reload() {
        release();
        if (uri == null) {
            return;
        }
        final int mine = ++load;
        final Context context = getContext().getApplicationContext();
        final Uri source = uri;
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean video = AeroWallpaper.isVideo(context, source);
                Drawable decoded = null;
                if (!video) {
                    try {
                        if (Build.VERSION.SDK_INT >= 28) {
                            decoded = AeroApi28.decode(context.getContentResolver(), source, MAX_IMAGE_SIDE);
                        }
                        else {
                            Bitmap bitmap = AeroWallpaper.loadStill(context, source, MAX_IMAGE_SIDE);
                            if (bitmap != null) {
                                decoded = new BitmapDrawable(context.getResources(), bitmap);
                            }
                        }
                    }
                    catch (Throwable t) {
                        decoded = null;
                    }
                }
                final Drawable result = decoded;
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mine != load) {
                            return;
                        }
                        if (video) {
                            startVideo();
                        }
                        else if (result != null) {
                            drawable = result;
                            image.setImageDrawable(result);
                            updatePlaying();
                        }
                    }
                });
            }
        }, "aero-wallpaper").start();
    }

    // ------------------------------------------------------------------ video

    private void startVideo() {
        if (texture != null) {
            return;
        }
        texture = new TextureView(getContext());
        texture.setSurfaceTextureListener(this);
        addView(texture, 1, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        if (uri == null || player != null) {
            return;
        }
        try {
            player = new MediaPlayer();
            player.setSurface(new Surface(surfaceTexture));
            player.setDataSource(getContext(), uri);
            player.setLooping(true);
            player.setVolume(0f, 0f);
            player.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                    videoWidth = width;
                    videoHeight = height;
                    applyCrop();
                }
            });
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    playerReady = true;
                    updatePlaying();
                }
            });
            player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    // Unsupported codec or file gone: fall back to the sky
                    release();
                    return true;
                }
            });
            player.prepareAsync();
        }
        catch (Throwable t) {
            release();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        applyCrop();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (player != null) {
            try {
                player.release();
            }
            catch (Throwable ignored) {
            }
            player = null;
            playerReady = false;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    /** TextureView stretches the video to fill itself; undo that and crop instead. */
    private void applyCrop() {
        if (texture == null || videoWidth <= 0 || videoHeight <= 0 || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        float viewW = getWidth();
        float viewH = getHeight();
        float scale = Math.max(viewW / videoWidth, viewH / videoHeight);
        Matrix matrix = new Matrix();
        matrix.setScale(videoWidth * scale / viewW, videoHeight * scale / viewH, viewW / 2f, viewH / 2f);
        texture.setTransform(matrix);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        applyCrop();
    }

    // ------------------------------------------------------------------ lifecycle

    private void updatePlaying() {
        boolean run = hasWindowFocus() && isAttachedToWindow();
        if (player != null && playerReady) {
            try {
                if (run && !player.isPlaying()) {
                    player.start();
                }
                else if (!run && player.isPlaying()) {
                    player.pause();
                }
            }
            catch (Throwable ignored) {
            }
        }
        if (drawable != null && Build.VERSION.SDK_INT >= 28) {
            AeroApi28.setPlaying(drawable, run);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        updatePlaying();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (uri != null && drawable == null && texture == null) {
            reload();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        release();
    }

    private void release() {
        load++;
        if (player != null) {
            try {
                player.release();
            }
            catch (Throwable ignored) {
            }
            player = null;
        }
        playerReady = false;
        if (texture != null) {
            texture.setSurfaceTextureListener(null);
            removeView(texture);
            texture = null;
        }
        if (drawable != null && Build.VERSION.SDK_INT >= 28) {
            AeroApi28.setPlaying(drawable, false);
        }
        drawable = null;
        image.setImageDrawable(null);
    }
}
