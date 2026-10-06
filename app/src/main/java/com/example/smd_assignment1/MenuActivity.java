package com.example.smd_assignment1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Screen 19 - Menu. */
public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        NavHelper.setup(this, NavHelper.MENU, "Menu");

        NavHelper.avatar((TextView) findViewById(R.id.avJW), "JW", NavHelper.c("#0B5F61"));

        findViewById(R.id.cardProfile).setOnClickListener(v ->
                startActivity(new Intent(this, MyProfileActivity.class)));

        findViewById(R.id.cardMarket).setOnClickListener(v -> NavHelper.go(this, MarketplaceActivity.class));
        findViewById(R.id.cardMemories).setOnClickListener(v -> NavHelper.toast(this, "Memories"));
        findViewById(R.id.cardSaved).setOnClickListener(v -> NavHelper.toast(this, "Saved"));
        findViewById(R.id.cardGroups).setOnClickListener(v -> NavHelper.toast(this, "Groups"));
        findViewById(R.id.cardFriends).setOnClickListener(v -> NavHelper.go(this, FriendsActivity.class));
        findViewById(R.id.cardEvents).setOnClickListener(v -> NavHelper.toast(this, "Events"));
        findViewById(R.id.btnSeeMore).setOnClickListener(v -> NavHelper.toast(this, "See more"));

        expander(R.id.rowHelp, R.id.subHelp, R.id.arrowHelp);
        expander(R.id.rowSettings, R.id.subSettings, R.id.arrowSettings);
        expander(R.id.rowSmd, R.id.subSmd, R.id.arrowSmd);

        int[] subs = {R.id.subHelp1, R.id.subHelp2, R.id.subSet1, R.id.subSet2, R.id.subSmd1, R.id.subSmd2};
        for (int id : subs) {
            final TextView t = findViewById(id);
            t.setOnClickListener(v -> NavHelper.toast(this, t.getText().toString()));
        }

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            NavHelper.toast(this, "Logged out");
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }

    private void expander(int rowId, int subId, int arrowId) {
        final View sub = findViewById(subId);
        final View arrow = findViewById(arrowId);
        findViewById(rowId).setOnClickListener(v -> {
            boolean open = sub.getVisibility() == View.VISIBLE;
            sub.setVisibility(open ? View.GONE : View.VISIBLE);
            arrow.setRotation(open ? 0f : 180f);
        });
    }
}
