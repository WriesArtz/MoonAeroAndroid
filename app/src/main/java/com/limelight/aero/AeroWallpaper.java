package com.limelight.aero;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import java.io.InputStream;
import java.util.Locale;

/** Helpers to classify and decode the user's wallpaper file. All of it may be slow: call off the UI thread. */
final class AeroWallpaper {
    private AeroWallpaper() {}

    static boolean isVideo(Context context, Uri uri) {
        String type = null;
        try {
            type = context.getContentResolver().getType(uri);
        }
        catch (Exception ignored) {
        }
        if (type != null) {
            return type.startsWith("video/");
        }
        String path = String.valueOf(uri).toLowerCase(Locale.ROOT);
        return path.endsWith(".mp4") || path.endsWith(".webm") || path.endsWith(".mkv")
                || path.endsWith(".mov") || path.endsWith(".3gp");
    }

    /**
     * A single frame, decoded at roughly {@code targetSide} pixels on its long side (or a bit more).
     * Returns null if the file cannot be read. GIFs yield their first frame.
     */
    static Bitmap loadStill(Context context, Uri uri, int targetSide) {
        try {
            if (isVideo(context, uri)) {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                try {
                    retriever.setDataSource(context, uri);
                    return retriever.getFrameAtTime(0);
                }
                finally {
                    retriever.release();
                }
            }

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            InputStream in = context.getContentResolver().openInputStream(uri);
            if (in == null) {
                return null;
            }
            try {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            finally {
                in.close();
            }
            int big = Math.max(bounds.outWidth, bounds.outHeight);
            if (big <= 0) {
                return null;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = Math.max(1, big / Math.max(1, targetSide));
            in = context.getContentResolver().openInputStream(uri);
            if (in == null) {
                return null;
            }
            try {
                return BitmapFactory.decodeStream(in, null, options);
            }
            finally {
                in.close();
            }
        }
        catch (Throwable t) {
            // Missing file, revoked permission, unsupported codec, out of memory...
            return null;
        }
    }
}
