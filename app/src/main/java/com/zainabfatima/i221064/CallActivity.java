package com.zainabfatima.i221064;

import android.Manifest;
import android.content.res.ColorStateList;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.model.CallInvite;
import com.zainabfatima.i221064.model.User;
import com.zainabfatima.i221064.util.AvatarView;
import com.zainabfatima.i221064.util.Clock;
import com.zainabfatima.i221064.util.Db;
import com.zainabfatima.i221064.util.UserCache;

import java.util.Locale;
import java.util.Map;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;

/**
 * Screen 22 - Voice / video call over Agora RTC.
 * Signalling goes through /calls/{calleeUid}: the caller creates the invite (status "ringing"),
 * the callee's IncomingCallActivity flips it to "accepted" or "declined", and either side sets
 * "ended" when hanging up. Both sides then meet in the Agora channel named in the invite.
 */
public class CallActivity extends AppCompatActivity {

    public static final String EXTRA_PEER_ID = "peer_id";
    public static final String EXTRA_VIDEO = "video";
    public static final String EXTRA_OUTGOING = "outgoing";
    public static final String EXTRA_CHANNEL = "channel";

    private static final long RING_TIMEOUT_MS = 45_000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private String me, peerId, channel, calleeId;
    private boolean video, outgoing;
    private boolean joined, connected, ended, muted, speakerOn, inviteSeen;
    private int seconds = 0;

    private RtcEngine engine;
    private TextView tvTimer;
    private FrameLayout remoteContainer, localContainer;
    private DatabaseReference inviteRef;
    private ValueEventListener inviteListener;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            seconds++;
            tvTimer.setText(String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60));
            handler.postDelayed(this, 1000);
        }
    };

    private final Runnable ringTimeout = () -> {
        if (!connected) {
            NavHelper.toast(this, "No answer");
            hangUp(CallInvite.ENDED);
        }
    };

    private final ActivityResultLauncher<String[]> permissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), (Map<String, Boolean> r) -> {
                if (hasAudio()) begin();
                else {
                    NavHelper.toast(this, "Microphone permission is needed for calls");
                    hangUp(CallInvite.ENDED);
                }
            });

    private final IRtcEngineEventHandler eventHandler = new IRtcEngineEventHandler() {
        @Override public void onJoinChannelSuccess(String ch, int uid, int elapsed) {
            joined = true;
        }

        @Override public void onUserJoined(final int uid, int elapsed) {
            runOnUiThread(() -> onRemoteJoined(uid));
        }

        @Override public void onUserOffline(int uid, int reason) {
            runOnUiThread(() -> {
                if (!ended) {
                    NavHelper.toast(CallActivity.this, "Call ended");
                    hangUp(CallInvite.ENDED);
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        me = Db.uid();
        peerId = getIntent().getStringExtra(EXTRA_PEER_ID);
        video = getIntent().getBooleanExtra(EXTRA_VIDEO, false);
        outgoing = getIntent().getBooleanExtra(EXTRA_OUTGOING, false);
        channel = getIntent().getStringExtra(EXTRA_CHANNEL);
        if (me == null || peerId == null) {
            finish();
            return;
        }
        calleeId = outgoing ? peerId : me;
        speakerOn = video;

        tvTimer = findViewById(R.id.tvTimer);
        remoteContainer = findViewById(R.id.remoteVideo);
        localContainer = findViewById(R.id.localVideo);
        tvTimer.setText(outgoing ? "Calling…" : "Connecting…");

        final AvatarView avatar = findViewById(R.id.callAvatar);
        UserCache.get(peerId, (User u) -> {
            if (u == null) return;
            avatar.bind(u);
            ((TextView) findViewById(R.id.callName)).setText(u.displayName());
        });

        findViewById(R.id.btnEnd).setOnClickListener(v -> hangUp(CallInvite.ENDED));

        final ImageView switchCam = findViewById(R.id.btnNotes);
        if (video) {
            switchCam.setImageResource(R.drawable.ic_camera);
            switchCam.setOnClickListener(v -> {
                if (engine != null) engine.switchCamera();
            });
        } else {
            switchCam.setVisibility(View.GONE);
        }

        final ImageView mute = findViewById(R.id.btnMute);
        mute.setOnClickListener(v -> {
            muted = !muted;
            if (engine != null) engine.muteLocalAudioStream(muted);
            mute.setBackgroundTintList(ColorStateList.valueOf(muted ? 0xFFFFFFFF : 0x33FFFFFF));
            mute.setColorFilter(muted ? 0xFF0B5F61 : 0xFFFFFFFF);
        });

        final ImageView speaker = findViewById(R.id.btnSpeaker);
        paintSpeaker(speaker);
        speaker.setOnClickListener(v -> {
            speakerOn = !speakerOn;
            if (engine != null) engine.setEnableSpeakerphone(speakerOn);
            paintSpeaker(speaker);
        });

        if (BuildConfig.AGORA_APP_ID.isEmpty()) {
            NavHelper.toast(this, "Add AGORA_APP_ID to local.properties to enable calls");
            finish();
            return;
        }

        if (hasAudio() && (!video || hasCamera())) begin();
        else if (video) permissions.launch(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA});
        else permissions.launch(new String[]{Manifest.permission.RECORD_AUDIO});
    }

    private void paintSpeaker(ImageView speaker) {
        speaker.setBackgroundTintList(ColorStateList.valueOf(speakerOn ? 0xFFFFFFFF : 0x33FFFFFF));
        speaker.setColorFilter(speakerOn ? 0xFF0B5F61 : 0xFFFFFFFF);
    }

    private boolean hasAudio() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasCamera() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    /** Permissions are in place: ring the other person (caller) and join the media channel. */
    private void begin() {
        if (outgoing) {
            channel = Db.chatId(me, peerId) + "_" + System.currentTimeMillis();
            User mine = UserCache.peek(me);
            CallInvite invite = new CallInvite();
            invite.callerId = me;
            invite.callerName = mine == null ? "" : mine.displayName();
            invite.calleeId = peerId;
            invite.channel = channel;
            invite.video = video;
            invite.status = CallInvite.RINGING;
            invite.timestamp = Clock.now();
            Db.call(peerId).setValue(invite);
            handler.postDelayed(ringTimeout, RING_TIMEOUT_MS);
        }
        watchInvite();
        startAgora();
    }

    private void watchInvite() {
        inviteRef = Db.call(calleeId);
        inviteListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                if (ended) return;
                CallInvite c = s.getValue(CallInvite.class);
                if (c == null) {
                    if (!inviteSeen) return;          // our own write has not arrived yet
                    closeLocal();                     // the invite was removed: call is over
                    return;
                }
                inviteSeen = true;
                if (!channel.equals(c.channel)) {     // replaced by a newer call
                    closeLocal();
                    return;
                }
                if (CallInvite.DECLINED.equals(c.status)) {
                    NavHelper.toast(CallActivity.this, "Call declined");
                    closeLocal();
                } else if (CallInvite.ENDED.equals(c.status)) {
                    closeLocal();
                } else if (CallInvite.ACCEPTED.equals(c.status)) {
                    handler.removeCallbacks(ringTimeout);
                    if (outgoing) tvTimer.setText("Connecting…");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        };
        inviteRef.addValueEventListener(inviteListener);
    }

    // -------------------------------------------------------------------- Agora
    private void startAgora() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getApplicationContext();
            config.mAppId = BuildConfig.AGORA_APP_ID;
            config.mEventHandler = eventHandler;
            engine = RtcEngine.create(config);
        } catch (Exception e) {
            NavHelper.toast(this, "Couldn't start the call engine");
            hangUp(CallInvite.ENDED);
            return;
        }
        engine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);
        engine.enableAudio();
        engine.setEnableSpeakerphone(speakerOn);
        if (video) {
            engine.enableVideo();
            SurfaceView local = new SurfaceView(getBaseContext());
            local.setZOrderMediaOverlay(true);
            localContainer.removeAllViews();
            localContainer.addView(local);
            localContainer.setVisibility(View.VISIBLE);
            engine.setupLocalVideo(new VideoCanvas(local, VideoCanvas.RENDER_MODE_HIDDEN, 0));
            engine.startPreview();
        }
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
        options.publishMicrophoneTrack = true;
        options.publishCameraTrack = video;
        options.autoSubscribeAudio = true;
        options.autoSubscribeVideo = video;
        // Testing-mode Agora projects need no token; pass a token here if the project requires one.
        engine.joinChannel(null, channel, 0, options);
    }

    private void onRemoteJoined(int uid) {
        if (connected) return;
        connected = true;
        handler.removeCallbacks(ringTimeout);
        if (video && engine != null) {
            SurfaceView remote = new SurfaceView(getBaseContext());
            remoteContainer.removeAllViews();
            remoteContainer.addView(remote);
            remoteContainer.setVisibility(View.VISIBLE);
            engine.setupRemoteVideo(new VideoCanvas(remote, VideoCanvas.RENDER_MODE_HIDDEN, uid));
            findViewById(R.id.callAvatar).setVisibility(View.GONE);
            findViewById(R.id.halo1).setVisibility(View.GONE);
            findViewById(R.id.halo2).setVisibility(View.GONE);
        }
        seconds = 0;
        tvTimer.setText("00:00");
        handler.postDelayed(tick, 1000);
    }

    // ---------------------------------------------------------------- hang up
    /** Tell the other side (via the invite) and close this screen. */
    private void hangUp(String status) {
        if (ended) return;
        if (channel != null) {
            // only touch the invite if it is still ours
            Db.call(calleeId).child("status").setValue(status);
        }
        closeLocal();
    }

    private void closeLocal() {
        if (ended) return;
        ended = true;
        handler.removeCallbacksAndMessages(null);
        if (inviteRef != null && inviteListener != null) inviteRef.removeEventListener(inviteListener);
        releaseEngine();
        // the caller tidies the invite up shortly after both sides have seen "ended"
        if (outgoing) {
            final String callee = calleeId;
            new Handler(Looper.getMainLooper()).postDelayed(() -> Db.call(callee).removeValue(), 3000);
        }
        finish();
    }

    private void releaseEngine() {
        if (engine != null) {
            try {
                engine.leaveChannel();
                if (video) engine.stopPreview();
            } catch (Exception ignored) { }
            RtcEngine.destroy();
            engine = null;
        }
    }

    @Override
    protected void onDestroy() {
        if (!ended) {
            // user left through the system UI: end the call for the other person too
            ended = true;
            if (channel != null) Db.call(calleeId).child("status").setValue(CallInvite.ENDED);
            handler.removeCallbacksAndMessages(null);
            if (inviteRef != null && inviteListener != null) inviteRef.removeEventListener(inviteListener);
            releaseEngine();
        }
        super.onDestroy();
    }
}
