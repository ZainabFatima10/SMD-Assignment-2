package com.zainabfatima.i221064.model;

import com.google.firebase.database.IgnoreExtraProperties;

/** An entry of the Notifications screen, stored at /notifications/{uid}/{id}. */
@IgnoreExtraProperties
public class AppNotification {
    public static final String FRIEND_REQUEST = "friend_request";
    public static final String FRIEND_ACCEPTED = "friend_accepted";
    public static final String REACTION = "reaction";
    public static final String COMMENT = "comment";
    public static final String SCREENSHOT = "screenshot";

    public String id;
    public String type;
    public String fromUid;
    public String text;
    public String refId;
    public long timestamp;
    public boolean read;

    public AppNotification() { }
}
