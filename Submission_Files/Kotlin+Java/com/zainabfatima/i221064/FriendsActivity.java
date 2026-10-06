package com.zainabfatima.i221064;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Screen 14 - Friends (tab 2 of the main tab bar). */
public class FriendsActivity extends AppCompatActivity {

    private int pending = 12;
    private TextView chipSuggestions, chipYourFriends;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.friends_screen);

        // ---- header ----
        NavHelper.click(this, R.id.btn_search, () -> startActivity(new Intent(this, SearchActivity.class)));
        NavHelper.click(this, R.id.messageIcon, () -> startActivity(new Intent(this, ChatsActivity.class)));

        // ---- tab bar (Friends is the selected tab) ----
        NavHelper.click(this, R.id.tab_home, () -> NavHelper.goHome(this));
        NavHelper.click(this, R.id.tab_people, () -> NavHelper.toast(this, "You're on Friends"));
        NavHelper.click(this, R.id.tab_shop, () -> NavHelper.go(this, MarketplaceActivity.class));
        NavHelper.click(this, R.id.bell_icon, () -> NavHelper.go(this, NotificationsActivity.class));
        NavHelper.click(this, R.id.tab_more, () -> NavHelper.go(this, MenuActivity.class));

        // ---- chips ----
        chipSuggestions = findViewById(R.id.suggestions_chip);
        chipYourFriends = findViewById(R.id.your_friends_chip);
        NavHelper.click(this, R.id.suggestions_chip, () -> selectChip(true));
        NavHelper.click(this, R.id.your_friends_chip, () -> selectChip(false));

        NavHelper.click(this, R.id.see_all_requests, () -> NavHelper.toast(this, "All friend requests"));

        // ---- requests ----
        int[] rows = {R.id.request1, R.id.request2, R.id.request3, R.id.request4};
        int[] confirm = {R.id.confirm1, R.id.confirm2, R.id.confirm3, R.id.confirm4};
        int[] delete = {R.id.delete1, R.id.delete2, R.id.delete3, R.id.delete4};
        String[] names = {"Sara Iqbal", "Bilal Ahmed", "Noor Fatima", "Daniyal Shah"};
        for (int i = 0; i < 4; i++) {
            final String name = names[i];
            final int c = confirm[i], d = delete[i];
            NavHelper.click(this, rows[i], () -> startActivity(new Intent(this, ProfileActivity.class)));
            NavHelper.click(this, c, () -> resolve(c, d, true, name));
            NavHelper.click(this, d, () -> resolve(c, d, false, name));
        }
    }

    private void selectChip(boolean suggestions) {
        chipSuggestions.setBackgroundResource(suggestions ? R.drawable.lightseagreen_chip : R.drawable.beige_chip);
        chipYourFriends.setBackgroundResource(suggestions ? R.drawable.beige_chip : R.drawable.lightseagreen_chip);
        chipSuggestions.setTextColor(getColor(suggestions ? R.color.SeaGreen : R.color.black));
        chipYourFriends.setTextColor(getColor(suggestions ? R.color.black : R.color.SeaGreen));
    }

    /** Confirm / Delete: keep the row (so the layout doesn't jump) but retire the other button. */
    private void resolve(int confirmId, int deleteId, boolean accepted, String name) {
        TextView confirm = findViewById(confirmId);
        TextView delete = findViewById(deleteId);
        if (!confirm.isEnabled()) return;           // already handled

        if (accepted) {
            confirm.setText("Friends");
            confirm.setEnabled(false);
            delete.setVisibility(View.INVISIBLE);
            NavHelper.toast(this, "You and " + name + " are now friends");
        } else {
            delete.setText("Removed");
            delete.setEnabled(false);
            confirm.setVisibility(View.INVISIBLE);
            confirm.setEnabled(false);
            NavHelper.toast(this, "Request from " + name + " deleted");
        }
        pending = Math.max(0, pending - 1);
        ((TextView) findViewById(R.id.requests_count)).setText(String.valueOf(pending));
    }
}
