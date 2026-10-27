package com.zainabfatima.i221064.feed;

import com.google.firebase.database.ServerValue;
import com.zainabfatima.i221064.model.AppNotification;
import com.zainabfatima.i221064.model.Post;
import com.zainabfatima.i221064.util.Clock;
import com.zainabfatima.i221064.util.Db;
import com.zainabfatima.i221064.util.Notifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Writes for posts, reactions and comment counters. */
public final class PostRepo {

    private PostRepo() { }

    /** Creates a post and returns its id. */
    public static String create(String text, List<String> images) {
        String uid = Db.uid();
        if (uid == null) return null;
        String id = Db.posts().push().getKey();
        Post p = new Post();
        p.id = id;
        p.authorId = uid;
        p.text = text == null ? "" : text;
        if (images != null) p.images = images;
        p.timestamp = Clock.now();
        Db.posts().child(id).setValue(p);
        return id;
    }

    public static void delete(String postId) {
        Map<String, Object> up = new HashMap<>();
        up.put("posts/" + postId, null);
        up.put("postReactions/" + postId, null);
        up.put("comments/" + postId, null);
        up.put("commentLikes/" + postId, null);
        up.put("postCounters/" + postId, null);
        Db.root().updateChildren(up);
    }

    /** One reaction per user per post: setting a new type replaces the old one. */
    public static void react(Post post, int type, boolean hadReaction) {
        String uid = Db.uid();
        if (uid == null) return;
        Db.postReactions(post.id).child(uid).setValue(type);
        if (!hadReaction) {
            Notifier.send(post.authorId, AppNotification.REACTION, post.id, "reacted to your post.");
        }
    }

    public static void removeReaction(String postId) {
        String uid = Db.uid();
        if (uid == null) return;
        Db.postReactions(postId).child(uid).removeValue();
    }

    /** Adds the comment and bumps the counter in one atomic write. */
    public static void addComment(Post post, Map<String, Object> comment, String commentId) {
        Map<String, Object> up = new HashMap<>();
        up.put("comments/" + post.id + "/" + commentId, comment);
        up.put("postCounters/" + post.id + "/comments", ServerValue.increment(1));
        Db.root().updateChildren(up);
        Notifier.send(post.authorId, AppNotification.COMMENT, post.id, "commented on your post.");
    }
}
