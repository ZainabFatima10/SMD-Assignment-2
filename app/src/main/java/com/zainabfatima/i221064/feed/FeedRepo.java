package com.zainabfatima.i221064.feed;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.model.Post;
import com.zainabfatima.i221064.util.Db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Home feed: posts of the signed-in user and their friends, newest first, updated in real time.
 * One query per author (posts are indexed by authorId) so we never download strangers' posts.
 */
public class FeedRepo {

    public interface Listener {
        void onPosts(List<Post> posts);
    }

    private static final int PER_AUTHOR = 25;

    private final Listener listener;
    private final Map<String, Query> queries = new HashMap<>();
    private final Map<String, ValueEventListener> listeners = new HashMap<>();
    private final Map<String, List<Post>> byAuthor = new HashMap<>();
    private ValueEventListener friendsListener;
    private com.google.firebase.database.DatabaseReference friendsRef;

    public FeedRepo(Listener l) {
        this.listener = l;
    }

    public void start() {
        final String me = Db.uid();
        if (me == null) return;
        follow(me);
        friendsRef = Db.friends(me);
        friendsListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                Set<String> wanted = new HashSet<>();
                wanted.add(me);
                for (DataSnapshot c : s.getChildren()) wanted.add(c.getKey());
                for (String a : wanted) follow(a);
                for (String a : new ArrayList<>(queries.keySet())) {
                    if (!wanted.contains(a)) unfollow(a);
                }
                deliver();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        };
        friendsRef.addValueEventListener(friendsListener);
    }

    public void stop() {
        if (friendsRef != null && friendsListener != null) friendsRef.removeEventListener(friendsListener);
        for (String a : new ArrayList<>(queries.keySet())) unfollow(a);
        byAuthor.clear();
    }

    private void follow(final String author) {
        if (queries.containsKey(author)) return;
        Query q = Db.posts().orderByChild("authorId").equalTo(author).limitToLast(PER_AUTHOR);
        ValueEventListener l = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                List<Post> list = new ArrayList<>();
                for (DataSnapshot c : s.getChildren()) {
                    Post p = c.getValue(Post.class);
                    if (p == null) continue;
                    if (p.id == null) p.id = c.getKey();
                    if (p.images == null) p.images = new ArrayList<>();
                    list.add(p);
                }
                byAuthor.put(author, list);
                deliver();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        };
        queries.put(author, q);
        listeners.put(author, l);
        q.addValueEventListener(l);
    }

    private void unfollow(String author) {
        Query q = queries.remove(author);
        ValueEventListener l = listeners.remove(author);
        if (q != null && l != null) q.removeEventListener(l);
        byAuthor.remove(author);
    }

    private void deliver() {
        List<Post> all = new ArrayList<>();
        for (List<Post> l : byAuthor.values()) all.addAll(l);
        Collections.sort(all, (a, b) -> Long.compare(b.timestamp, a.timestamp));   // newest first
        listener.onPosts(all);
    }
}
