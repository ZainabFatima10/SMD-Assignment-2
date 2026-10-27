package com.zainabfatima.i221064.feed;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.model.Story;
import com.zainabfatima.i221064.util.Clock;
import com.zainabfatima.i221064.util.Db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Live stories for the Home row: yours plus your friends'. A story is only shown while it is less
 * than 24 hours old (and, for "Close friends", only to the people it was shared with). Expired stories
 * of the signed-in user are also deleted from the database.
 */
public class StoriesRepo {

    public interface Listener {
        void onStories(@Nullable StoryGroup mine, List<StoryGroup> friends);
    }

    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, DatabaseReference> refs = new HashMap<>();
    private final Map<String, ValueEventListener> listeners = new HashMap<>();
    private final Map<String, List<Story>> raw = new HashMap<>();
    private ValueEventListener friendsListener;
    private DatabaseReference friendsRef;
    private String me;
    private boolean running = false;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            deliver();               // drops stories that just reached 24 hours
            handler.postDelayed(this, 30_000);
        }
    };

    public StoriesRepo(Listener l) {
        this.listener = l;
    }

    public void start() {
        me = Db.uid();
        if (me == null) return;
        running = true;
        follow(me);
        friendsRef = Db.friends(me);
        friendsListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                Set<String> wanted = new HashSet<>();
                wanted.add(me);
                for (DataSnapshot c : s.getChildren()) wanted.add(c.getKey());
                for (String a : wanted) follow(a);
                for (String a : new ArrayList<>(refs.keySet())) if (!wanted.contains(a)) unfollow(a);
                deliver();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        };
        friendsRef.addValueEventListener(friendsListener);
        handler.postDelayed(tick, 30_000);
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(tick);
        if (friendsRef != null && friendsListener != null) friendsRef.removeEventListener(friendsListener);
        for (String a : new ArrayList<>(refs.keySet())) unfollow(a);
        raw.clear();
    }

    private void follow(final String owner) {
        if (refs.containsKey(owner)) return;
        DatabaseReference ref = Db.stories(owner);
        ValueEventListener l = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                List<Story> list = new ArrayList<>();
                for (DataSnapshot c : s.getChildren()) {
                    Story st = c.getValue(Story.class);
                    if (st == null) continue;
                    if (st.id == null) st.id = c.getKey();
                    if (st.ownerId == null) st.ownerId = owner;
                    list.add(st);
                }
                raw.put(owner, list);
                deliver();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        };
        refs.put(owner, ref);
        listeners.put(owner, l);
        ref.addValueEventListener(l);
    }

    private void unfollow(String owner) {
        DatabaseReference ref = refs.remove(owner);
        ValueEventListener l = listeners.remove(owner);
        if (ref != null && l != null) ref.removeEventListener(l);
        raw.remove(owner);
    }

    /** Is this story visible to me right now? */
    public static boolean isVisible(Story s, String me, long now) {
        if (s.expiresAt <= now) return false;                          // older than 24 hours
        if (me != null && me.equals(s.ownerId)) return true;
        if (Story.AUDIENCE_CLOSE.equals(s.audience)) {
            return s.visibleTo != null && s.visibleTo.containsKey(me);
        }
        return true;
    }

    private void deliver() {
        long now = Clock.now();
        StoryGroup mine = null;
        List<StoryGroup> friends = new ArrayList<>();
        for (Map.Entry<String, List<Story>> e : raw.entrySet()) {
            StoryGroup g = new StoryGroup(e.getKey());
            for (Story s : e.getValue()) {
                if (s.expiresAt <= now && e.getKey().equals(me)) {
                    purge(s);                                           // clean up my own expired stories
                    continue;
                }
                if (isVisible(s, me, now)) g.stories.add(s);
            }
            if (g.stories.isEmpty()) continue;
            Collections.sort(g.stories, (a, b) -> Long.compare(a.timestamp, b.timestamp));
            if (e.getKey().equals(me)) mine = g;
            else friends.add(g);
        }
        Collections.sort(friends, (a, b) -> Long.compare(b.latest(), a.latest()));
        listener.onStories(mine, friends);
    }

    /** Removes a story and its media from the database. */
    public static void purge(Story s) {
        if (s == null || s.id == null || s.ownerId == null) return;
        Map<String, Object> up = new HashMap<>();
        up.put("stories/" + s.ownerId + "/" + s.id, null);
        up.put("storyMedia/" + s.id, null);
        up.put("storyViews/" + s.id, null);
        Db.root().updateChildren(up);
    }
}
