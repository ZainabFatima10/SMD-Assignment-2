package com.zainabfatima.i221064.model;

import com.google.firebase.database.IgnoreExtraProperties;

/** One row of the Chats list, stored at /userChats/{uid}/{peerId}. */
@IgnoreExtraProperties
public class ChatSummary {
    public String peerId;
    public String lastMessage;
    public String lastSenderId;
    public long timestamp;
    public long unread;

    public ChatSummary() { }
}
