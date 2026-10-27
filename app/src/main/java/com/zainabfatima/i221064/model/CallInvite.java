package com.zainabfatima.i221064.model;

import com.google.firebase.database.IgnoreExtraProperties;

/** A ringing call, stored at /calls/{calleeUid}. The caller creates it; the callee answers by changing status. */
@IgnoreExtraProperties
public class CallInvite {
    public static final String RINGING = "ringing";
    public static final String ACCEPTED = "accepted";
    public static final String DECLINED = "declined";
    public static final String ENDED = "ended";

    public String callerId;
    public String callerName;
    public String calleeId;
    public String channel;
    public boolean video;
    public String status;
    public long timestamp;

    public CallInvite() { }
}
