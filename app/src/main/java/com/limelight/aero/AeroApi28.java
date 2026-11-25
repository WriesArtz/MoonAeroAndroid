package com.limelight.aero;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.graphics.ImageDecoder;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import java.io.IOException;

/** Animated GIF / WebP support. Only touched on Android 9 (API 28) and newer. */
@TargetApi(28)
final class AeroApi28 {
    private AeroApi28() {}

    static Drawable decode(ContentResolver resolver, Uri uri, final int maxSide) throws IOException {
        ImageDecoder.Source source = ImageDecoder.createSource(resolver, uri);
        return ImageDecoder.decodeDrawable(source, new ImageDecoder.OnHeaderDecodedListener() {
            @Override
            public void onHeaderDecoded(ImageDecoder decoder, ImageDecoder.ImageInfo info, ImageDecoder.Source src) {
                int big = Math.max(info.getSize().getWidth(), info.getSize().getHeight());
                if (big > maxSide) {
                    decoder.setTargetSampleSize((big + maxSide - 1) / maxSide);
                }
            }
        });
    }

    /** Starts or pauses the animation if the drawable is animated; returns whether it was. */
    static boolean setPlaying(Drawable drawable, boolean play) {
        if (!(drawable instanceof AnimatedImageDrawable)) {
            return false;
        }
        AnimatedImageDrawable animated = (AnimatedImageDrawable) drawable;
        animated.setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE);
        if (play) {
            animated.start();
        }
        else {
            animated.stop();
        }
        return true;
    }
}
