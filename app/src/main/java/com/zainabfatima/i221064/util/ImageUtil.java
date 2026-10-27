package com.zainabfatima.i221064.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Base64;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;

/**
 * Images are stored as Base64 JPEG strings inside the Realtime Database (no paid Storage bucket is
 * needed). Everything is down-scaled and compressed first so the nodes stay small.
 */
public final class ImageUtil {

    public static final int PROFILE_DIM = 400;
    public static final int COVER_DIM = 900;
    public static final int POST_DIM = 900;
    public static final int STORY_DIM = 900;
    public static final int CHAT_DIM = 800;
    public static final int THUMB_DIM = 240;

    /** Biggest video (bytes) we are willing to Base64 into the database. */
    public static final long MAX_VIDEO_BYTES = 4L * 1024 * 1024;

    private ImageUtil() { }

    /** Reads a Uri, scales it so the longest side is {@code maxDim}, and returns it as Base64 JPEG. */
    public static String uriToBase64(Context c, Uri uri, int maxDim, int quality) throws IOException {
        Bitmap b = decode(c, uri, maxDim);
        try {
            return bitmapToBase64(b, quality);
        } finally {
            b.recycle();
        }
    }

    public static String bitmapToBase64(Bitmap b, int quality) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.JPEG, quality, bos);
        return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
    }

    public static Bitmap decode(Context c, Uri uri, int maxDim) throws IOException {
        ContentResolver cr = c.getContentResolver();
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        try (InputStream in = cr.openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, o);
        }
        int sample = 1;
        int longest = Math.max(o.outWidth, o.outHeight);
        while (longest / sample > maxDim * 2) sample *= 2;

        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = sample;
        Bitmap bmp;
        try (InputStream in = cr.openInputStream(uri)) {
            bmp = BitmapFactory.decodeStream(in, null, o2);
        }
        if (bmp == null) throw new IOException("Could not read image");

        // honour the camera's EXIF rotation
        int rotation = 0;
        try (InputStream in = cr.openInputStream(uri)) {
            if (in != null) {
                ExifInterface exif = new ExifInterface(in);
                int orient = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                if (orient == ExifInterface.ORIENTATION_ROTATE_90) rotation = 90;
                else if (orient == ExifInterface.ORIENTATION_ROTATE_180) rotation = 180;
                else if (orient == ExifInterface.ORIENTATION_ROTATE_270) rotation = 270;
            }
        } catch (Exception ignored) { }

        float scale = Math.min(1f, (float) maxDim / Math.max(bmp.getWidth(), bmp.getHeight()));
        if (scale < 1f || rotation != 0) {
            Matrix m = new Matrix();
            m.postScale(scale, scale);
            m.postRotate(rotation);
            Bitmap scaled = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
            if (scaled != bmp) bmp.recycle();
            bmp = scaled;
        }
        return bmp;
    }

    /** Middle frame of a video as a bitmap (used for the story thumbnail). */
    @Nullable
    public static Bitmap videoFrame(Context c, Uri uri) {
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(c, uri);
            return r.getFrameAtTime(500_000);
        } catch (Exception e) {
            return null;
        } finally {
            try { r.release(); } catch (Exception ignored) { }
        }
    }

    public static String scaledThumb(Bitmap src) {
        float scale = Math.min(1f, (float) THUMB_DIM / Math.max(src.getWidth(), src.getHeight()));
        Bitmap t = scale < 1f
                ? Bitmap.createScaledBitmap(src, Math.max(1, Math.round(src.getWidth() * scale)),
                Math.max(1, Math.round(src.getHeight() * scale)), true)
                : src;
        return bitmapToBase64(t, 60);
    }

    /** Size of the content behind a Uri, or -1. */
    public static long sizeOf(Context c, Uri uri) {
        try (android.content.res.AssetFileDescriptor fd = c.getContentResolver().openAssetFileDescriptor(uri, "r")) {
            return fd == null ? -1 : fd.getLength();
        } catch (Exception e) {
            return -1;
        }
    }

    public static String uriToBase64Raw(Context c, Uri uri) throws IOException {
        try (InputStream in = c.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("Cannot open " + uri);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[16 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
        }
    }

    /** Writes Base64 bytes to a cache file (video playback). */
    public static File base64ToFile(Context c, String b64, String name) throws IOException {
        File f = new File(c.getCacheDir(), name);
        byte[] data = Base64.decode(b64, Base64.DEFAULT);
        try (OutputStream os = new FileOutputStream(f)) {
            os.write(data);
        }
        return f;
    }

    /** Shows a Base64 JPEG in an ImageView; clears the view when there is nothing to show. */
    public static void show(ImageView iv, @Nullable String b64) {
        if (b64 == null || b64.isEmpty()) {
            Glide.with(iv).clear(iv);
            iv.setImageDrawable(null);
            return;
        }
        try {
            byte[] data = Base64.decode(b64, Base64.DEFAULT);
            Glide.with(iv).load(data).into(iv);
        } catch (IllegalArgumentException e) {
            iv.setImageDrawable(null);
        }
    }
}
