package com.zainabfatima.i221064.model;

import com.google.firebase.database.IgnoreExtraProperties;

/** A comment or reply, stored at /comments/{postId}/{id}. parentId is null for top-level comments. */
@IgnoreExtraProperties
public class Comment {
    public String id;
    public String postId;
    public String authorId;
    public String text;
    public String image;      // optional Base64 JPEG
    public String parentId;   // null = top level, otherwise the id of the comment replied to
    public long timestamp;

    public Comment() { }
}
