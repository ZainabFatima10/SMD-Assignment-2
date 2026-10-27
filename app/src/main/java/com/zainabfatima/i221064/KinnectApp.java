package com.zainabfatima.i221064;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.zainabfatima.i221064.util.Clock;
import com.zainabfatima.i221064.util.Presence;
import com.zainabfatima.i221064.util.Session;

public class KinnectApp extends Application implements Application.ActivityLifecycleCallbacks {

    public static final String CHANNEL_MESSAGES = "messages";
    public static final String CHANNEL_ALERTS = "alerts";

    private int started = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Clock.start();
        createChannels();
        registerActivityLifecycleCallbacks(this);
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel messages = new NotificationChannel(CHANNEL_MESSAGES, "Messages",
                NotificationManager.IMPORTANCE_HIGH);
        messages.setDescription("New chat messages");
        NotificationChannel alerts = new NotificationChannel(CHANNEL_ALERTS, "Alerts",
                NotificationManager.IMPORTANCE_DEFAULT);
        alerts.setDescription("Screenshots, calls and friend activity");
        nm.createNotificationChannel(messages);
        nm.createNotificationChannel(alerts);
    }

    // ---- foreground tracking: "online" while any activity is visible ----
    @Override public void onActivityStarted(@NonNull Activity a) {
        if (++started == 1) {
            Presence.setForeground(true);
            Session.onLogin(this);
        }
    }

    @Override public void onActivityStopped(@NonNull Activity a) {
        if (--started == 0) {
            Presence.setForeground(false);
        }
    }

    @Override public void onActivityCreated(@NonNull Activity a, @Nullable Bundle b) { }
    @Override public void onActivityResumed(@NonNull Activity a) { }
    @Override public void onActivityPaused(@NonNull Activity a) { }
    @Override public void onActivitySaveInstanceState(@NonNull Activity a, @NonNull Bundle b) { }
    @Override public void onActivityDestroyed(@NonNull Activity a) { }
}
