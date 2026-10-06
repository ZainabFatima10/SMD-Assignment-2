package com.zainabfatima.i221064;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Screen 18 - Notifications (rows are declared in activity_notifications.xml). */
public class NotificationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        NavHelper.setup(this, NavHelper.NOTIF, "Notifications");

        bold(R.id.n1_text, "<b>Aisha Khan</b> and <b>32 others</b> reacted<br>to your photo.");
        bold(R.id.n2_text, "<b>Sara Iqbal</b> sent you a friend<br>request.");
        bold(R.id.n3_text, "<b>Zain Raza</b> replied to your comment:<br>“next time invite the rest of us”");
        bold(R.id.n4_text, "New post in <b>SMD Class of 2026</b>:<br>“Assignment 1 is out. Start early.”");
        bold(R.id.n5_text, "<b>Hamza Ali</b> tagged you in a post.");
        bold(R.id.n6_text, "<b>Lina Marsh</b> liked your story.");

        final View actionRow = findViewById(R.id.actions);
        findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            actionRow.setVisibility(View.GONE);
            NavHelper.toast(this, "Friend request confirmed");
        });
        findViewById(R.id.btnDelete).setOnClickListener(v -> {
            actionRow.setVisibility(View.GONE);
            NavHelper.toast(this, "Request deleted");
        });

        int[] rows = {R.id.n1, R.id.n2, R.id.n3, R.id.n4, R.id.n5, R.id.n6};
        int[] more = {R.id.n1_more, R.id.n2_more, R.id.n3_more, R.id.n4_more, R.id.n5_more, R.id.n6_more};
        for (int i = 0; i < rows.length; i++) {
            final int index = i;
            findViewById(rows[i]).setOnClickListener(v -> {
                if (index == 1) startActivity(new Intent(this, FriendsActivity.class)); // friend request
                else if (index == 4) startActivity(new Intent(this, ProfileActivity.class));
                else NavHelper.toast(this, "Opening notification");
            });
            findViewById(more[i]).setOnClickListener(v -> NavHelper.toast(this, "More options"));
        }

        findViewById(R.id.btnPrevious).setOnClickListener(v ->
                NavHelper.toast(this, "No previous notifications"));
    }

    private void bold(int id, String html) {
        ((TextView) findViewById(id)).setText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));
    }
}
