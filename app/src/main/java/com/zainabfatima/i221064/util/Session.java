package com.zainabfatima.i221064.util;

import android.content.Context;

import com.google.firebase.messaging.FirebaseMessaging;
import com.zainabfatima.i221064.CallListener;

/** What has to happen when somebody signs in or out. */
public final class Session {

    private Session() { }

    /** Call after every successful sign in (and when the app starts with a restored session). */
    public static void onLogin(Context c) {
        String uid = Db.uid();
        if (uid == null) return;
        Presence.refresh();
        CallListener.start(c.getApplicationContext());
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            if (token != null && Db.uid() != null) Db.user(Db.uid()).child("fcmToken").setValue(token);
        });
    }

    public static void onLogout() {
        CallListener.stop();
        UserCache.clear();
    }
}
