package com.zainabfatima.i221064.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

/** A Kinnect account, stored at /users/{uid}. */
@IgnoreExtraProperties
public class User {
    public String uid;
    public String name;
    public String nameLower;
    public String username;
    public String usernameLower;
    public String email;
    public String bio;
    public String gender;
    public String birthday;
    public String profilePic;   // Base64 JPEG
    public String coverPic;     // Base64 JPEG
    public boolean online;
    public long lastSeen;
    public String fcmToken;
    public boolean profileComplete;
    public long createdAt;

    public User() { }

    @Exclude
    public String displayName() {
        return name == null || name.trim().isEmpty() ? "Kinnect user" : name;
    }

    @Exclude
    public String initials() {
        String n = displayName().trim();
        String[] parts = n.split("\\s+");
        String s = parts[0].substring(0, 1);
        if (parts.length > 1) s += parts[parts.length - 1].substring(0, 1);
        return s.toUpperCase();
    }
}
