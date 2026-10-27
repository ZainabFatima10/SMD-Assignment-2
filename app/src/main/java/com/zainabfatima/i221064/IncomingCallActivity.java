package com.zainabfatima.i221064;

import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.model.CallInvite;
import com.zainabfatima.i221064.util.AvatarView;
import com.zainabfatima.i221064.util.Db;
import com.zainabfatima.i221064.util.UserCache;

/** Shown when somebody calls you: accept to join the Agora channel, or decline. */
public class IncomingCallActivity extends AppCompatActivity {

    public static final String EXTRA_CALLER_ID = "caller_id";
    public static final String EXTRA_CALLER_NAME = "caller_name";
    public static final String EXTRA_CHANNEL = "channel";
    public static final String EXTRA_VIDEO = "video";

    private String me, callerId, channel;
    private boolean video, answered;
    private Ringtone ringtone;
    private DatabaseReference ref;
    private ValueEventListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        setContentView(R.layout.activity_incoming_call);

        me = Db.uid();
        callerId = getIntent().getStringExtra(EXTRA_CALLER_ID);
        channel = getIntent().getStringExtra(EXTRA_CHANNEL);
        video = getIntent().getBooleanExtra(EXTRA_VIDEO, false);
        if (me == null || callerId == null || channel == null) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.incomingType)).setText(video ? "Incoming video call" : "Incoming voice call");
        String fallback = getIntent().getStringExtra(EXTRA_CALLER_NAME);
        ((TextView) findViewById(R.id.incomingName)).setText(fallback == null ? "" : fallback);
        final AvatarView avatar = findViewById(R.id.incomingAvatar);
        UserCache.get(callerId, u -> {
            if (u == null) return;
            avatar.bind(u);
            ((TextView) findViewById(R.id.incomingName)).setText(u.displayName());
        });

        findViewById(R.id.btnDecline).setOnClickListener(v -> {
            answered = true;
            Db.call(me).child("status").setValue(CallInvite.DECLINED);
            finish();
        });
        findViewById(R.id.btnAccept).setOnClickListener(v -> {
            answered = true;
            Db.call(me).child("status").setValue(CallInvite.ACCEPTED);
            startActivity(new Intent(this, CallActivity.class)
                    .putExtra(CallActivity.EXTRA_PEER_ID, callerId)
                    .putExtra(CallActivity.EXTRA_CHANNEL, channel)
                    .putExtra(CallActivity.EXTRA_VIDEO, video)
                    .putExtra(CallActivity.EXTRA_OUTGOING, false));
            finish();
        });

        // stop ringing if the caller gives up
        ref = Db.call(me);
        listener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                CallInvite c = s.getValue(CallInvite.class);
                if (!answered && (c == null || !channel.equals(c.channel) || !CallInvite.RINGING.equals(c.status))) {
                    finish();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        };
        ref.addValueEventListener(listener);

        Uri tone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        ringtone = RingtoneManager.getRingtone(this, tone);
        if (ringtone != null) ringtone.play();
        handler.postDelayed(() -> {
            if (!answered) finish();
        }, 45_000);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (ringtone != null && ringtone.isPlaying()) ringtone.stop();
        if (ref != null && listener != null) ref.removeEventListener(listener);
        super.onDestroy();
    }
}
