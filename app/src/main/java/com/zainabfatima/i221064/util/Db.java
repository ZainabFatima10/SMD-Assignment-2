package com.zainabfatima.i221064.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/** One place that knows every path of the Realtime Database (see database.rules.json). */
public final class Db {

    private Db() { }

    public static FirebaseAuth auth() {
        return FirebaseAuth.getInstance();
    }

    /** uid of the signed-in user, or null. */
    @Nullable
    public static String uid() {
        FirebaseUser u = auth().getCurrentUser();
        return u == null ? null : u.getUid();
    }

    public static boolean isLoggedIn() {
        return auth().getCurrentUser() != null;
    }

    public static DatabaseReference root() {
        return FirebaseDatabase.getInstance().getReference();
    }

    public static DatabaseReference users() { return root().child("users"); }
    public static DatabaseReference user(@NonNull String uid) { return users().child(uid); }
    /** usernames/{lowercase username} -> uid. Guarantees usernames are unique. */
    public static DatabaseReference usernames() { return root().child("usernames"); }

    public static DatabaseReference friends(@NonNull String uid) { return root().child("friends").child(uid); }
    /** friendRequests/{receiver}/{sender}: requests waiting for {receiver}. */
    public static DatabaseReference incomingRequests(@NonNull String uid) { return root().child("friendRequests").child(uid); }
    /** sentRequests/{sender}/{receiver}: requests {sender} is still waiting on. */
    public static DatabaseReference sentRequests(@NonNull String uid) { return root().child("sentRequests").child(uid); }
    public static DatabaseReference closeFriends(@NonNull String uid) { return root().child("closeFriends").child(uid); }

    public static DatabaseReference posts() { return root().child("posts"); }
    public static DatabaseReference postReactions(@NonNull String postId) { return root().child("postReactions").child(postId); }
    public static DatabaseReference comments(@NonNull String postId) { return root().child("comments").child(postId); }
    public static DatabaseReference commentLikes(@NonNull String postId) { return root().child("commentLikes").child(postId); }

    public static DatabaseReference stories(@NonNull String ownerId) { return root().child("stories").child(ownerId); }
    public static DatabaseReference storyMedia(@NonNull String storyId) { return root().child("storyMedia").child(storyId); }

    public static DatabaseReference messages(@NonNull String chatId) { return root().child("chats").child(chatId).child("messages"); }
    public static DatabaseReference userChats(@NonNull String uid) { return root().child("userChats").child(uid); }
    public static DatabaseReference notifications(@NonNull String uid) { return root().child("notifications").child(uid); }
    public static DatabaseReference call(@NonNull String calleeUid) { return root().child("calls").child(calleeUid); }

    /** Both participants always compute the same chat id. */
    public static String chatId(@NonNull String a, @NonNull String b) {
        return a.compareTo(b) < 0 ? a + "_" + b : b + "_" + a;
    }
}
