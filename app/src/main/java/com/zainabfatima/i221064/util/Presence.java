package com.zainabfatima.i221064.util;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.android.gms.tasks.Task;

import java.util.HashMap;
import java.util.Map;

/**
 * Online / offline status. A user is "online" while the app is in the foreground; Firebase's
 * onDisconnect() flips the flag back automatically if the process dies or the network drops.
 */
public final class Presence {

    private static boolean foreground = false;
    private static boolean listening = false;
    private static boolean connected = false;

    private Presence() { }

    public static synchronized void setForeground(boolean fg) {
        foreground = fg;
        if (fg && !listening) {
            listening = true;
            FirebaseDatabase.getInstance().getReference(".info/connected")
                    .addValueEventListener(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot s) {
                            connected = Boolean.TRUE.equals(s.getValue(Boolean.class));
                            if (connected && foreground) apply(true);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError e) { }
                    });
        }
        if (!fg) apply(false);
        else if (connected) apply(true);
    }

    /** Re-applies the current state, e.g. right after a (new) user signed in. */
    public static synchronized void refresh() {
        if (foreground && connected) apply(true);
    }

    private static void apply(boolean online) {
        String uid = Db.uid();
        if (uid == null) return;
        DatabaseReference ref = Db.user(uid);
        if (online) {
            ref.child("online").onDisconnect().setValue(false);
            ref.child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP);
            ref.child("online").setValue(true);
        } else {
            ref.child("online").setValue(false);
            ref.child("lastSeen").setValue(ServerValue.TIMESTAMP);
        }
    }

    /** Called by Log out: mark the user offline and forget this device's push token, then run {@code then}. */
    public static void goOfflineThen(final Runnable then) {
        String uid = Db.uid();
        if (uid == null) {
            then.run();
            return;
        }
        Map<String, Object> m = new HashMap<>();
        m.put("online", false);
        m.put("lastSeen", ServerValue.TIMESTAMP);
        m.put("fcmToken", null);
        Task<Void> t = Db.user(uid).updateChildren(m);
        t.addOnCompleteListener(x -> then.run());
    }
}
