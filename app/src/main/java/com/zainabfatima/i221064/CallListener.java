package com.zainabfatima.i221064;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.model.CallInvite;
import com.zainabfatima.i221064.util.Clock;
import com.zainabfatima.i221064.util.Db;

/** While the app is open, watches /calls/{me} and opens the incoming-call screen when somebody rings. */
public final class CallListener {

    private static DatabaseReference ref;
    private static ValueEventListener listener;
    private static String ringingChannel;

    private CallListener() { }

    public static synchronized void start(final Context app) {
        final String uid = Db.uid();
        if (uid == null || listener != null) return;
        ref = Db.call(uid);
        listener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                CallInvite c = s.getValue(CallInvite.class);
                if (c == null || !CallInvite.RINGING.equals(c.status)) {
                    ringingChannel = null;
                    return;
                }
                // ignore invites that were left behind more than a minute ago
                if (Clock.now() - c.timestamp > 60_000) return;
                if (c.channel != null && c.channel.equals(ringingChannel)) return;
                ringingChannel = c.channel;
                Intent i = new Intent(app, IncomingCallActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra(IncomingCallActivity.EXTRA_CALLER_ID, c.callerId);
                i.putExtra(IncomingCallActivity.EXTRA_CALLER_NAME, c.callerName);
                i.putExtra(IncomingCallActivity.EXTRA_CHANNEL, c.channel);
                i.putExtra(IncomingCallActivity.EXTRA_VIDEO, c.video);
                app.startActivity(i);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        };
        ref.addValueEventListener(listener);
    }

    public static synchronized void stop() {
        if (ref != null && listener != null) ref.removeEventListener(listener);
        ref = null;
        listener = null;
        ringingChannel = null;
    }
}
