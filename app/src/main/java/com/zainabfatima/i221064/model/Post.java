package com.zainabfatima.i221064.model;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.List;

/** A feed post, stored at /posts/{id}. Images are Base64 JPEG strings. */
@IgnoreExtraProperties
public class Post {
    public String id;
    public String authorId;
    public String text;
    public List<String> images = new ArrayList<>();
    public long timestamp;

    public Post() { }
}
