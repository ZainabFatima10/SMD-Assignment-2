package com.zainabfatima.i221064.util;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.model.AppNotification;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Friend-request state machine: Add friend -> Requested -> Friends (all writes are atomic multi-path updates). */
public final class FriendsHelper {

    public static final int NONE = 0;        // "Add friend"
    public static final int REQUESTED = 1;   // I sent a request, waiting ("Requested")
    public static final int RECEIVED = 2;    // they sent me one ("Confirm" / "Delete")
    public static final int FRIENDS = 3;     // "Friends"
    public static final int SELF = 4;

    public interface StateListener {
        void onState(int state);
    }

    /** Live subscription to the relationship between me and one other user. */
    public static final class StateWatch {
        private final DatabaseReference[] refs = new DatabaseReference[3];
        private final ValueEventListener[] listeners = new ValueEventListener[3];
        private final boolean[] flags = new boolean[3];
        private final StateListener cb;

        StateWatch(String me, String other, StateListener cb) {
            this.cb = cb;
            refs[0] = Db.friends(me).child(other);
            refs[1] = Db.sentRequests(me).child(other);
            refs[2] = Db.incomingRequests(me).child(other);
            for (int i = 0; i < 3; i++) {
                final int index = i;
                listeners[i] = new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot s) {
                        flags[index] = s.exists();
                        fire();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) { }
                };
                refs[i].addValueEventListener(listeners[i]);
            }
        }

        private void fire() {
            int state = flags[0] ? FRIENDS : flags[1] ? REQUESTED : flags[2] ? RECEIVED : NONE;
            cb.onState(state);
        }

        public void stop() {
            for (int i = 0; i < 3; i++) refs[i].removeEventListener(listeners[i]);
        }
    }

    public interface CountCallback {
        void onCount(int n);
    }

    private FriendsHelper() { }

    /** How many friends {@code other} has in common with me ({@code myFriends} = my friend uids). */
    public static void mutualCount(final Set<String> myFriends, String other, final CountCallback cb) {
        Db.friends(other).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                int n = 0;
                for (DataSnapshot c : s.getChildren()) if (myFriends.contains(c.getKey())) n++;
                cb.onCount(n);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { cb.onCount(0); }
        });
    }

    public static StateWatch watch(String other, StateListener cb) {
        String me = Db.uid();
        if (me == null || me.equals(other)) {
            cb.onState(SELF);
            return new StateWatch(me == null ? "_" : me, other, s -> { });
        }
        return new StateWatch(me, other, cb);
    }

    public static String labelFor(int state) {
        switch (state) {
            case REQUESTED: return "Requested";
            case FRIENDS: return "Friends";
            case RECEIVED: return "Respond";
            default: return "Add friend";
        }
    }

    /** Add friend. */
    public static void sendRequest(String other) {
        String me = Db.uid();
        if (me == null) return;
        long now = Clock.now();
        Map<String, Object> up = new HashMap<>();
        up.put("friendRequests/" + other + "/" + me, now);
        up.put("sentRequests/" + me + "/" + other, now);
        up.put("notifications/" + other + "/fr_" + me, notification("fr_" + me, AppNotification.FRIEND_REQUEST, me,
                "sent you a friend request.", me, now));
        Db.root().updateChildren(up);
    }

    /** Tapping "Requested" cancels the request. */
    public static void cancelRequest(String other) {
        String me = Db.uid();
        if (me == null) return;
        Map<String, Object> up = new HashMap<>();
        up.put("friendRequests/" + other + "/" + me, null);
        up.put("sentRequests/" + me + "/" + other, null);
        up.put("notifications/" + other + "/fr_" + me, null);
        Db.root().updateChildren(up);
    }

    /** Confirm a request somebody sent me. */
    public static void confirm(String from) {
        String me = Db.uid();
        if (me == null) return;
        long now = Clock.now();
        Map<String, Object> up = new HashMap<>();
        up.put("friends/" + me + "/" + from, now);
        up.put("friends/" + from + "/" + me, now);
        up.put("friendRequests/" + me + "/" + from, null);
        up.put("sentRequests/" + from + "/" + me, null);
        up.put("notifications/" + me + "/fr_" + from, null);
        up.put("notifications/" + from + "/fa_" + me, notification("fa_" + me, AppNotification.FRIEND_ACCEPTED, me,
                "accepted your friend request.", me, now));
        Db.root().updateChildren(up);
    }

    /** Delete a request somebody sent me. */
    public static void deleteRequest(String from) {
        String me = Db.uid();
        if (me == null) return;
        Map<String, Object> up = new HashMap<>();
        up.put("friendRequests/" + me + "/" + from, null);
        up.put("sentRequests/" + from + "/" + me, null);
        up.put("notifications/" + me + "/fr_" + from, null);
        Db.root().updateChildren(up);
    }

    public static void unfriend(String other) {
        String me = Db.uid();
        if (me == null) return;
        Map<String, Object> up = new HashMap<>();
        up.put("friends/" + me + "/" + other, null);
        up.put("friends/" + other + "/" + me, null);
        up.put("closeFriends/" + me + "/" + other, null);
        up.put("closeFriends/" + other + "/" + me, null);
        Db.root().updateChildren(up);
    }

    /** Applies the right action for the button's current state. */
    public static void onButtonTapped(int state, String other) {
        switch (state) {
            case NONE: sendRequest(other); break;
            case REQUESTED: cancelRequest(other); break;
            case RECEIVED: confirm(other); break;
            default: break;
        }
    }

    public static Map<String, Object> notification(String id, String type, String fromUid, String text,
                                                    String refId, long ts) {
        Map<String, Object> n = new HashMap<>();
        n.put("id", id);
        n.put("type", type);
        n.put("fromUid", fromUid);
        n.put("text", text);
        n.put("refId", refId);
        n.put("timestamp", ts);
        n.put("read", false);
        return n;
    }
}
