package com.zainabfatima.i221064;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Screen 21 - Chat. */
public class ChatActivity extends AppCompatActivity {

    private LinearLayout msgs;
    private ScrollView scroll;
    private EditText etMsg;
    private ImageView btnSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        String name = getIntent().getStringExtra("name");
        String initials = getIntent().getStringExtra("initials");
        String color = getIntent().getStringExtra("color");
        if (name == null) { name = "Aisha Khan"; initials = "AK"; color = "#A23B52"; }
        final String firstName = name.split(" ")[0];
        int col = NavHelper.c(color);

        NavHelper.avatar((TextView) findViewById(R.id.hdrAvatar), initials, col);
        NavHelper.avatar((TextView) findViewById(R.id.introAvatar), initials, col);
        NavHelper.avatar((TextView) findViewById(R.id.smallAvatar1), initials, col);
        NavHelper.avatar((TextView) findViewById(R.id.smallAvatar2), initials, col);
        findViewById(R.id.seenDot).setBackgroundTintList(android.content.res.ColorStateList.valueOf(col));
        ((TextView) findViewById(R.id.hdrName)).setText(name);
        ((TextView) findViewById(R.id.introName)).setText(name);
        ((TextView) findViewById(R.id.tvReplied)).setText("You replied to " + firstName);

        msgs = findViewById(R.id.msgs);
        scroll = findViewById(R.id.scroll);
        etMsg = findViewById(R.id.etMsg);
        btnSend = findViewById(R.id.btnSend);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCall).setOnClickListener(v ->
                startActivity(new Intent(this, CallActivity.class)));
        findViewById(R.id.btnVideo).setOnClickListener(v -> NavHelper.toast(this, "Video call"));
        final String chatName = name;
        findViewById(R.id.btnInfo).setOnClickListener(v -> {
            if (chatName.equals("Omar Farooq")) startActivity(new Intent(this, ProfileActivity.class));
            else NavHelper.toast(this, "Chat info");
        });
        findViewById(R.id.btnPlus).setOnClickListener(v -> NavHelper.toast(this, "More actions"));
        findViewById(R.id.btnCamera).setOnClickListener(v -> startActivity(new Intent(this, CameraActivity.class)));
        findViewById(R.id.btnGallery).setOnClickListener(v -> NavHelper.toast(this, "Gallery"));
        findViewById(R.id.btnMic).setOnClickListener(v -> NavHelper.toast(this, "Voice message"));
        findViewById(R.id.btnEmoji).setOnClickListener(v -> NavHelper.toast(this, "Emoji"));

        // Thumb icon turns into a send arrow while the user is typing.
        etMsg.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                btnSend.setImageResource(s.toString().trim().isEmpty() ? R.drawable.ic_thumb : R.drawable.ic_send);
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        btnSend.setOnClickListener(v -> {
            String t = etMsg.getText().toString().trim();
            addOutgoing(t.isEmpty() ? "👍" : t);
            etMsg.setText("");
        });
    }

    private void addOutgoing(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(16);
        tv.setBackgroundResource(R.drawable.bg_bubble_out);
        int h = dp(16), vv = dp(10);
        tv.setPadding(h, vv, h, vv);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.END;
        lp.bottomMargin = dp(6);

        // keep the typing indicator as the last row
        msgs.addView(tv, msgs.getChildCount() - 1, lp);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
