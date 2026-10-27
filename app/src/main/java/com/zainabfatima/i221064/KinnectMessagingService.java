package com.zainabfatima.i221064;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.zainabfatima.i221064.util.Db;

import java.util.Map;

/**
 * Receives the data messages sent by the Cloud Function in /functions (new message, screenshot).
 * They are data-only so this service builds the notification itself, whether the app is open or not.
 */
public class KinnectMessagingService extends FirebaseMessagingService {

    /** uid of the chat currently on screen, so we do not notify about a conversation you are reading. */
    public static volatile String openChatPeer = null;

    @Override
    public void onNewToken(@NonNull String token) {
        String uid = Db.uid();
        if (uid != null) Db.user(uid).child("fcmToken").setValue(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Map<String, String> d = message.getData();
        String type = d.get("type");
        String peerId = d.get("peerId");
        String title = d.get("title");
        String body = d.get("body");
        if (title == null) title = "Kinnect";
        if (body == null) body = "";

        if ("call".equals(type)) {
            showIncomingCall(d);
            return;
        }
        if (peerId != null && peerId.equals(openChatPeer)) return;   // already reading it

        Intent open = new Intent(this, ChatActivity.class);
        open.putExtra(ChatActivity.EXTRA_PEER_ID, peerId);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        int requestCode = peerId == null ? 0 : peerId.hashCode();
        PendingIntent pi = PendingIntent.getActivity(this, requestCode, open, flags);

        String channel = "screenshot".equals(type) ? KinnectApp.CHANNEL_ALERTS : KinnectApp.CHANNEL_MESSAGES;
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, channel)
                .setSmallIcon(R.drawable.ic_chat)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        try {
            NotificationManagerCompat.from(this).notify(requestCode, b.build());
        } catch (SecurityException ignored) {
            // POST_NOTIFICATIONS not granted
        }
    }

    /** A ringing call while the app is in the background: full-screen notification that opens the call screen. */
    private void showIncomingCall(Map<String, String> d) {
        Intent i = new Intent(this, IncomingCallActivity.class);
        i.putExtra(IncomingCallActivity.EXTRA_CALLER_ID, d.get("callerId"));
        i.putExtra(IncomingCallActivity.EXTRA_CALLER_NAME, d.get("callerName"));
        i.putExtra(IncomingCallActivity.EXTRA_CHANNEL, d.get("channel"));
        i.putExtra(IncomingCallActivity.EXTRA_VIDEO, "true".equals(d.get("video")));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(this, 7001, i, flags);
        String who = d.get("callerName") == null ? "Someone" : d.get("callerName");
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, KinnectApp.CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_call)
                .setContentTitle(who)
                .setContentText("true".equals(d.get("video")) ? "Incoming video call" : "Incoming voice call")
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .setTimeoutAfter(45_000)
                .setContentIntent(pi)
                .setFullScreenIntent(pi, true);
        try {
            NotificationManagerCompat.from(this).notify(7001, b.build());
        } catch (SecurityException ignored) { }
    }
}
