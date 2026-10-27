package com.zainabfatima.i221064.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;

import androidx.core.content.ContextCompat;

/**
 * Detects that the user took a screenshot while an Activity is on screen.
 * Android 14+ has a real callback (Activity.ScreenCaptureCallback). On older versions we watch
 * MediaStore for a new image whose name/path says "screenshot" (needs the photo permission).
 */
public final class ScreenshotWatcher {

    private final Activity activity;
    private final Runnable onScreenshot;
    private final Handler main = new Handler(Looper.getMainLooper());
    private long lastFired = 0;
    private Object callback;               // Activity.ScreenCaptureCallback on API 34+
    private ContentObserver observer;

    public ScreenshotWatcher(Activity activity, Runnable onScreenshot) {
        this.activity = activity;
        this.onScreenshot = onScreenshot;
    }

    private void fire() {
        long now = System.currentTimeMillis();
        if (now - lastFired < 4000) return;      // one screenshot often triggers several signals
        lastFired = now;
        onScreenshot.run();
    }

    public void start() {
        if (Build.VERSION.SDK_INT >= 34) {
            try {
                Activity.ScreenCaptureCallback cb = this::fire;
                activity.registerScreenCaptureCallback(ContextCompat.getMainExecutor(activity), cb);
                callback = cb;
            } catch (SecurityException ignored) {
                startObserver();
            }
        } else {
            startObserver();
        }
    }

    private boolean hasImageAccess() {
        String p = Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        return ContextCompat.checkSelfPermission(activity, p) == PackageManager.PERMISSION_GRANTED;
    }

    private void startObserver() {
        if (!hasImageAccess()) return;
        observer = new ContentObserver(main) {
            @Override public void onChange(boolean selfChange, Uri uri) {
                if (looksLikeScreenshot(uri)) fire();
            }
        };
        activity.getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer);
    }

    private boolean looksLikeScreenshot(Uri uri) {
        if (uri == null) return false;
        String[] proj = {MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATA};
        try (Cursor c = activity.getContentResolver().query(uri, proj, null, null, null)) {
            if (c == null || !c.moveToFirst()) return false;
            String name = c.getString(0);
            long added = c.getLong(1);
            String path = null;
            try { path = c.getString(2); } catch (Exception ignored) { }
            boolean recent = Math.abs(System.currentTimeMillis() / 1000 - added) < 15;
            String hay = ((name == null ? "" : name) + " " + (path == null ? "" : path)).toLowerCase();
            return recent && hay.contains("screenshot");
        } catch (Exception e) {
            return false;
        }
    }

    public void stop() {
        if (Build.VERSION.SDK_INT >= 34 && callback != null) {
            try {
                activity.unregisterScreenCaptureCallback((Activity.ScreenCaptureCallback) callback);
            } catch (Exception ignored) { }
            callback = null;
        }
        if (observer != null) {
            activity.getContentResolver().unregisterContentObserver(observer);
            observer = null;
        }
    }
}
