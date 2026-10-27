package com.zainabfatima.i221064.model;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

/** A chat message, stored at /chats/{chatId}/messages/{id}. */
@IgnoreExtraProperties
public class Message {
    public static final String TEXT = "text";
    public static final String IMAGE = "image";
    public static final String POST = "post";
    public static final String SYSTEM = "system";

    public static final long EDIT_WINDOW_MS = 5L * 60 * 1000;

    public String id;
    public String senderId;
    public String type;
    public String text;
    public String image;          // Base64 JPEG for image messages
    public String postId;         // for shared posts
    public String postPreview;    // short text of the shared post
    public String replyToId;
    public String replyToText;
    public String replyToSender;  // display name of the author of the replied message
    public boolean edited;
    public Map<String, Boolean> hearts = new HashMap<>();
    public long timestamp;

    public Message() { }
}
