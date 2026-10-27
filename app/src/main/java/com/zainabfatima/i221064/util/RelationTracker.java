package com.zainabfatima.i221064.util;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashSet;
import java.util.Set;

/**
 * Keeps three live sets for the signed-in user - friends, requests I sent, requests I received -
 * so list screens can show the right Add friend / Requested / Confirm / Friends button per row.
 */
public final class RelationTracker {

    public interface Changed {
        void onChanged();
    }

    public final Set<String> friends = new HashSet<>();
    public final Set<String> sent = new HashSet<>();
    public final Set<String> received = new HashSet<>();

    private final DatabaseReference[] refs = new DatabaseReference[3];
    private final ValueEventListener[] listeners = new ValueEventListener[3];
    private final Changed cb;

    public RelationTracker(Changed cb) {
        this.cb = cb;
    }

    public void start() {
        String me = Db.uid();
        if (me == null) return;
        refs[0] = Db.friends(me);
        refs[1] = Db.sentRequests(me);
        refs[2] = Db.incomingRequests(me);
        final java.util.List<Set<String>> targets = java.util.Arrays.asList(friends, sent, received);
        for (int i = 0; i < 3; i++) {
            final Set<String> target = targets.get(i);
            listeners[i] = new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot s) {
                    target.clear();
                    for (DataSnapshot c : s.getChildren()) target.add(c.getKey());
                    cb.onChanged();
                }
                @Override public void onCancelled(@NonNull DatabaseError e) { }
            };
            refs[i].addValueEventListener(listeners[i]);
        }
    }

    public void stop() {
        for (int i = 0; i < 3; i++) {
            if (refs[i] != null && listeners[i] != null) refs[i].removeEventListener(listeners[i]);
        }
    }

    public int stateOf(String uid) {
        if (uid.equals(Db.uid())) return FriendsHelper.SELF;
        if (friends.contains(uid)) return FriendsHelper.FRIENDS;
        if (sent.contains(uid)) return FriendsHelper.REQUESTED;
        if (received.contains(uid)) return FriendsHelper.RECEIVED;
        return FriendsHelper.NONE;
    }
}
