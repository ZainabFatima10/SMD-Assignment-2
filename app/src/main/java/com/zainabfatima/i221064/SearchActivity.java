package com.zainabfatima.i221064;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Screen 13 - Search. */
public class SearchActivity extends AppCompatActivity {

    private TextView[] chips;
    private final boolean[] requested = new boolean[2];
    private boolean joined = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_screen);

        NavHelper.click(this, R.id.backarrow, this::finish);
        NavHelper.click(this, R.id.btn_clear, () -> NavHelper.toast(this, "Search cleared"));

        // ---- filter chips ----
        chips = new TextView[]{
                findViewById(R.id.chip_all), findViewById(R.id.chip_people), findViewById(R.id.chip_posts),
                findViewById(R.id.chip_groups), findViewById(R.id.chip_marketplace)};
        final int[] chipIds = {R.id.chip_all, R.id.chip_people, R.id.chip_posts, R.id.chip_groups, R.id.chip_marketplace};
        for (int i = 0; i < chips.length; i++) {
            final int index = i;
            NavHelper.click(this, chipIds[i], () -> selectChip(index));
        }

        // ---- people ----
        NavHelper.click(this, R.id.person1, () -> startActivity(new Intent(this, ProfileActivity.class)));
        NavHelper.click(this, R.id.person2, () -> startActivity(new Intent(this, ProfileActivity.class)));
        NavHelper.click(this, R.id.person3, () -> startActivity(new Intent(this, ProfileActivity.class)));
        NavHelper.click(this, R.id.add_person2, () -> toggleRequest(0, R.id.add_person2, "Omar Siddiqui"));
        NavHelper.click(this, R.id.add_person3, () -> toggleRequest(1, R.id.add_person3, "Omar Tariq"));
        NavHelper.click(this, R.id.see_all_people, () -> selectChip(1));

        // ---- groups ----
        NavHelper.click(this, R.id.group1, () -> NavHelper.toast(this, "Omar's Photography Circle"));
        NavHelper.click(this, R.id.join_button, () -> {
            joined = !joined;
            ((TextView) findViewById(R.id.join_button)).setText(joined ? "Joined" : "Join");
            NavHelper.toast(this, joined ? "You joined the group" : "You left the group");
        });

        // ---- recent searches ----
        recent(R.id.recent1, R.id.recent1_text, R.id.recent1_remove);
        recent(R.id.recent2, R.id.recent2_text, R.id.recent2_remove);
        recent(R.id.recent3, R.id.recent3_text, R.id.recent3_remove);
    }

    private void selectChip(int selected) {
        for (int i = 0; i < chips.length; i++) NavHelper.chip(chips[i], i == selected, true);
        if (selected == 4) startActivity(new Intent(this, MarketplaceActivity.class));
    }

    private void toggleRequest(int slot, int viewId, String name) {
        requested[slot] = !requested[slot];
        findViewById(viewId).setAlpha(requested[slot] ? 0.45f : 1f);
        NavHelper.toast(this, requested[slot] ? "Friend request sent to " + name : "Request cancelled");
    }

    private void recent(int rowId, int textId, int removeId) {
        final View row = findViewById(rowId);
        final String q = ((TextView) findViewById(textId)).getText().toString();
        NavHelper.click(this, rowId, () -> NavHelper.toast(this, "Searching for \"" + q + "\""));
        NavHelper.click(this, removeId, () -> row.setVisibility(View.INVISIBLE));
    }
}
