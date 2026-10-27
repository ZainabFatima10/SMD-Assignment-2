package com.zainabfatima.i221064.model;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Story metadata, stored at /stories/{ownerId}/{id}. The (heavy) media itself lives at
 * /storyMedia/{id} so the Home row only has to download the small thumbnail.
 */
@IgnoreExtraProperties
public class Story {
    public static final String IMAGE = "image";
    public static final String VIDEO = "video";
    public static final String AUDIENCE_STORY = "story";
    public static final String AUDIENCE_CLOSE = "close";
    public static final long LIFETIME_MS = 24L * 60 * 60 * 1000;

    public String id;
    public String ownerId;
    public String mediaType;
    public String thumb;      // Base64 JPEG thumbnail
    public String text;       // label added in the editor
    public String audience;   // "story" (all friends) or "close"
    public Map<String, Boolean> visibleTo = new HashMap<>();   // only filled for close-friends stories
    public long timestamp;
    public long expiresAt;

    public Story() { }
}
