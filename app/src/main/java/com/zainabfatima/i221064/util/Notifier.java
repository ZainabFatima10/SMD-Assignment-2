package com.zainabfatima.i221064.util;

import com.google.firebase.database.DatabaseReference;

import java.util.Map;

/** Writes an entry into somebody's Notifications screen (never to yourself). */
public final class Notifier {

    private Notifier() { }

    public static void send(String toUid, String type, String refId, String text) {
        String me = Db.uid();
        if (me == null || me.equals(toUid)) return;
        DatabaseReference ref = Db.notifications(toUid).push();
        String id = ref.getKey();
        Map<String, Object> n = FriendsHelper.notification(id, type, me, text, refId, Clock.now());
        ref.setValue(n);
    }
}
