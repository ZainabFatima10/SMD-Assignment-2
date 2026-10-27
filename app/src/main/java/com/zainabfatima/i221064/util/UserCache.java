package com.zainabfatima.i221064.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.model.User;

import java.util.HashMap;
import java.util.Map;

/** Looks users up by uid, remembering what it already fetched. */
public final class UserCache {

    public interface Callback {
        void onUser(@Nullable User user);
    }

    /** A live subscription (profile edits, online dot). Call {@link #stop()} when the row goes away. */
    public static final class Watch {
        private final DatabaseReference ref;
        private final ValueEventListener listener;

        Watch(DatabaseReference ref, ValueEventListener l) {
            this.ref = ref;
            this.listener = l;
        }

        public void stop() {
            ref.removeEventListener(listener);
        }
    }

    private static final Map<String, User> CACHE = new HashMap<>();

    private UserCache() { }

    @Nullable
    public static User peek(String uid) {
        return CACHE.get(uid);
    }

    public static void put(User u) {
        if (u != null && u.uid != null) CACHE.put(u.uid, u);
    }

    public static void clear() {
        CACHE.clear();
    }

    /** One-off read; answers instantly from the cache when it can, then refreshes it. */
    public static void get(final String uid, final Callback cb) {
        User cached = CACHE.get(uid);
        if (cached != null) {
            cb.onUser(cached);
            return;
        }
        Db.user(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                User u = s.getValue(User.class);
                if (u != null) {
                    if (u.uid == null) u.uid = uid;
                    CACHE.put(uid, u);
                }
                cb.onUser(u);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                cb.onUser(null);
            }
        });
    }

    /** Real-time subscription to one user. */
    public static Watch watch(final String uid, final Callback cb) {
        DatabaseReference ref = Db.user(uid);
        ValueEventListener l = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                User u = s.getValue(User.class);
                if (u != null) {
                    if (u.uid == null) u.uid = uid;
                    CACHE.put(uid, u);
                }
                cb.onUser(u);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        };
        ref.addValueEventListener(l);
        return new Watch(ref, l);
    }
}
