package com.example.smd_assignment1;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Screen 17 - Other profile (launcher activity). */
public class ProfileActivity extends AppCompatActivity {

    private boolean requested = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ((TextView) findViewById(R.id.tvCounts)).setText(
                Html.fromHtml("<b>1,204</b> friends · <b>38</b> mutual", Html.FROM_HTML_MODE_LEGACY));
        ((TextView) findViewById(R.id.tvWork)).setText(
                Html.fromHtml("Photographer at <b>Studio Nocturne</b>", Html.FROM_HTML_MODE_LEGACY));
        ((TextView) findViewById(R.id.tvLives)).setText(
                Html.fromHtml("Lives in <b>Karachi, Pakistan</b>", Html.FROM_HTML_MODE_LEGACY));
        ((TextView) findViewById(R.id.tvMutual)).setText(Html.fromHtml(
                "Friends with <b>Aisha Khan</b>, <b>Lina Marsh</b> and <b>36 others</b>",
                Html.FROM_HTML_MODE_LEGACY));

        NavHelper.avatar((TextView) findViewById(R.id.mf1), "AK", NavHelper.c("#A23B52"));
        NavHelper.avatar((TextView) findViewById(R.id.mf2), "LM", NavHelper.c("#7B4F7F"));
        NavHelper.avatar((TextView) findViewById(R.id.mf3), "ZR", NavHelper.c("#3F6E8C"));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSearch).setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));

        findViewById(R.id.btnAdd).setOnClickListener(v -> {
            requested = !requested;
            ((TextView) findViewById(R.id.tvAdd)).setText(requested ? "Request sent" : "Add friend");
            NavHelper.toast(this, requested ? "Friend request sent" : "Request cancelled");
        });

        findViewById(R.id.btnMessage).setOnClickListener(v -> {
            Intent i = new Intent(this, ChatActivity.class);
            i.putExtra("name", "Omar Farooq");
            i.putExtra("initials", "OF");
            i.putExtra("color", "#2F6E4E");
            startActivity(i);
        });

        findViewById(R.id.btnMore).setOnClickListener(this::showMore);
        findViewById(R.id.btnSeeAll).setOnClickListener(v -> NavHelper.toast(this, "All photos"));
        findViewById(R.id.ph1).setOnClickListener(v -> NavHelper.toast(this, "Photo 1"));
        findViewById(R.id.ph2).setOnClickListener(v -> NavHelper.toast(this, "Photo 2"));
        findViewById(R.id.ph3).setOnClickListener(v -> NavHelper.toast(this, "Photo 3"));
    }

    /** The "..." button doubles as the hub that reaches the other screens. */
    private void showMore(View anchor) {
        PopupMenu pm = new PopupMenu(this, anchor);
        pm.getMenu().add(0, 1, 0, "Notifications");
        pm.getMenu().add(0, 2, 1, "Menu");
        pm.getMenu().add(0, 3, 2, "Marketplace");
        pm.getMenu().add(0, 4, 3, "Chats");
        pm.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: startActivity(new Intent(this, NotificationsActivity.class)); break;
                case 2: startActivity(new Intent(this, MenuActivity.class)); break;
                case 3: startActivity(new Intent(this, MarketplaceActivity.class)); break;
                case 4: startActivity(new Intent(this, ChatsActivity.class)); break;
            }
            return true;
        });
        pm.show();
    }
}
