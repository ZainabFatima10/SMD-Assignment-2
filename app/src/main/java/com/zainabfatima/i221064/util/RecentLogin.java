package com.zainabfatima.i221064.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.zainabfatima.i221064.model.User;

/** Remembers the last account that signed in on this device (the "Recent login" card). */
public final class RecentLogin {

    private static final String PREFS = "kinnect_prefs";

    public static final class Account {
        public String uid, name, email, pic;
    }

    private RecentLogin() { }

    public static void save(Context c, User u, String email) {
        if (u == null) return;
        String pic = null;
        if (u.profilePic != null && !u.profilePic.isEmpty() && u.profilePic.length() < 60_000) pic = u.profilePic;
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("recent_uid", u.uid)
                .putString("recent_name", u.displayName())
                .putString("recent_email", email != null ? email : u.email)
                .putString("recent_pic", pic)
                .apply();
    }

    @Nullable
    public static Account load(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String uid = p.getString("recent_uid", null);
        if (uid == null) return null;
        Account a = new Account();
        a.uid = uid;
        a.name = p.getString("recent_name", "");
        a.email = p.getString("recent_email", "");
        a.pic = p.getString("recent_pic", null);
        return a;
    }
}
