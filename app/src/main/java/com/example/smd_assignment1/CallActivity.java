package com.example.smd_assignment1;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

/** Screen 22 - Voice call. */
public class CallActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int seconds = 3 * 60 + 12;
    private TextView tvTimer;
    private boolean muted = false, speakerOn = false;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            seconds++;
            tvTimer.setText(String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60));
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        tvTimer = findViewById(R.id.tvTimer);

        findViewById(R.id.btnEnd).setOnClickListener(v -> finish());
        findViewById(R.id.btnNotes).setOnClickListener(v -> NavHelper.toast(this, "Call notes"));

        final ImageView mute = findViewById(R.id.btnMute);
        mute.setOnClickListener(v -> {
            muted = !muted;
            mute.setBackgroundTintList(ColorStateList.valueOf(muted ? 0xFFFFFFFF : 0x33FFFFFF));
            mute.setColorFilter(muted ? 0xFF0B5F61 : 0xFFFFFFFF);
            NavHelper.toast(this, muted ? "Muted" : "Unmuted");
        });

        final ImageView speaker = findViewById(R.id.btnSpeaker);
        speaker.setOnClickListener(v -> {
            speakerOn = !speakerOn;
            speaker.setBackgroundTintList(ColorStateList.valueOf(speakerOn ? 0xFFFFFFFF : 0x33FFFFFF));
            speaker.setColorFilter(speakerOn ? 0xFF0B5F61 : 0xFFFFFFFF);
            NavHelper.toast(this, speakerOn ? "Speaker on" : "Speaker off");
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(tick, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }
}
