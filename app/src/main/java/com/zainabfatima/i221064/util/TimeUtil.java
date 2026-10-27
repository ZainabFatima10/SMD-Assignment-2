package com.zainabfatima.i221064.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class TimeUtil {

    private TimeUtil() { }

    /** 12m, 3h, 2d ... used on posts, comments and notifications. */
    public static String ago(long ts) {
        long diff = Math.max(0, Clock.now() - ts);
        long min = diff / 60000;
        if (min < 1) return "now";
        if (min < 60) return min + "m";
        long h = min / 60;
        if (h < 24) return h + "h";
        long d = h / 24;
        if (d < 7) return d + "d";
        return (d / 7) + "w";
    }

    /** 14:05 */
    public static String clock(long ts) {
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(ts));
    }

    /** Chats list: time today, otherwise the weekday / date. */
    public static String chatTime(long ts) {
        long diff = Clock.now() - ts;
        if (diff < 24L * 3600 * 1000) return clock(ts);
        if (diff < 7L * 24 * 3600 * 1000) return new SimpleDateFormat("EEE", Locale.getDefault()).format(new Date(ts));
        return new SimpleDateFormat("d MMM", Locale.getDefault()).format(new Date(ts));
    }

    public static String lastSeen(long ts) {
        if (ts <= 0) return "Offline";
        String a = ago(ts);
        return a.equals("now") ? "Active just now" : "Active " + a + " ago";
    }
}
