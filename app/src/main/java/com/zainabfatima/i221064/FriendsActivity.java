package com.zainabfatima.i221064;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.model.User;
import com.zainabfatima.i221064.util.AvatarView;
import com.zainabfatima.i221064.util.Db;
import com.zainabfatima.i221064.util.FriendsHelper;
import com.zainabfatima.i221064.util.RelationTracker;
import com.zainabfatima.i221064.util.UserCache;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 14 - Friends. Shows the real number of pending requests with Confirm / Delete buttons,
 * and either "Your friends" or suggestions ("People you may know") below.
 */
public class FriendsActivity extends AppCompatActivity {

    private static final int COLLAPSED_REQUESTS = 3;

    private RelationTracker relations;
    private TextView chipSuggestions, chipYourFriends, count, seeAll, noRequests, noPeople, peopleHeading;
    private LinearLayout requestsBox, peopleBox;
    private boolean showFriends = false;
    private boolean expanded = false;
    private final List<User> suggestions = new ArrayList<>();
    private boolean suggestionsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.friends_screen);

        NavHelper.click(this, R.id.btn_search, () -> startActivity(new Intent(this, SearchActivity.class)));
        NavHelper.click(this, R.id.messageIcon, () -> startActivity(new Intent(this, ChatsActivity.class)));

        // tab bar (Friends is the selected tab)
        NavHelper.click(this, R.id.tab_home, () -> NavHelper.goHome(this));
        NavHelper.click(this, R.id.tab_people, () -> { });
        NavHelper.click(this, R.id.tab_shop, () -> NavHelper.go(this, MarketplaceActivity.class));
        NavHelper.click(this, R.id.bell_icon, () -> NavHelper.go(this, NotificationsActivity.class));
        NavHelper.click(this, R.id.tab_more, () -> NavHelper.go(this, MenuActivity.class));

        chipSuggestions = findViewById(R.id.suggestions_chip);
        chipYourFriends = findViewById(R.id.your_friends_chip);
        count = findViewById(R.id.requests_count);
        seeAll = findViewById(R.id.see_all_requests);
        noRequests = findViewById(R.id.noRequests);
        noPeople = findViewById(R.id.noPeople);
        peopleHeading = findViewById(R.id.peopleHeading);
        requestsBox = findViewById(R.id.requestsContainer);
        peopleBox = findViewById(R.id.peopleContainer);

        NavHelper.click(this, R.id.suggestions_chip, () -> selectChip(false));
        NavHelper.click(this, R.id.your_friends_chip, () -> selectChip(true));
        seeAll.setOnClickListener(v -> {
            expanded = !expanded;
            render();
        });

        relations = new RelationTracker(this::render);
        relations.start();
        selectChip(false);
    }

    private void selectChip(boolean friends) {
        showFriends = friends;
        chipSuggestions.setBackgroundResource(friends ? R.drawable.beige_chip : R.drawable.lightseagreen_chip);
        chipYourFriends.setBackgroundResource(friends ? R.drawable.lightseagreen_chip : R.drawable.beige_chip);
        chipSuggestions.setTextColor(getColor(friends ? R.color.black : R.color.SeaGreen));
        chipYourFriends.setTextColor(getColor(friends ? R.color.SeaGreen : R.color.black));
        peopleHeading.setText(friends ? "Your friends" : getString(R.string.people_you_may_know));
        if (!friends && !suggestionsLoaded) loadSuggestions();
        render();
    }

    private void loadSuggestions() {
        suggestionsLoaded = true;
        Db.users().limitToFirst(40).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                suggestions.clear();
                for (DataSnapshot c : s.getChildren()) {
                    User u = c.getValue(User.class);
                    if (u == null) continue;
                    if (u.uid == null) u.uid = c.getKey();
                    if (!u.profileComplete) continue;
                    suggestions.add(u);
                }
                render();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        });
    }

    private void render() {
        renderRequests();
        renderPeople();
    }

    // ---------------------------------------------------------------- requests
    private void renderRequests() {
        final List<String> pending = new ArrayList<>(relations.received);
        count.setText(String.valueOf(pending.size()));
        requestsBox.removeAllViews();
        noRequests.setVisibility(pending.isEmpty() ? View.VISIBLE : View.GONE);
        seeAll.setVisibility(pending.size() > COLLAPSED_REQUESTS ? View.VISIBLE : View.GONE);
        seeAll.setText(expanded ? "Show less" : getString(R.string.see_all));

        int limit = expanded ? pending.size() : Math.min(COLLAPSED_REQUESTS, pending.size());
        LayoutInflater inf = LayoutInflater.from(this);
        for (int i = 0; i < limit; i++) {
            final String uid = pending.get(i);
            final View row = inf.inflate(R.layout.item_request, requestsBox, false);
            final AvatarView avatar = row.findViewById(R.id.reqAvatar);
            final TextView name = row.findViewById(R.id.reqName);
            final TextView mutual = row.findViewById(R.id.reqMutual);
            avatar.bind("?", null, uid);
            UserCache.get(uid, u -> {
                if (u == null) return;
                avatar.bind(u);
                name.setText(u.displayName());
            });
            mutual.setText("");
            FriendsHelper.mutualCount(relations.friends, uid, n ->
                    mutual.setText(n == 0 ? "" : n + (n == 1 ? " mutual friend" : " mutual friends")));

            row.setOnClickListener(v -> openProfile(uid));
            row.findViewById(R.id.reqConfirm).setOnClickListener(v -> {
                v.setEnabled(false);
                FriendsHelper.confirm(uid);
            });
            row.findViewById(R.id.reqDelete).setOnClickListener(v -> {
                v.setEnabled(false);
                FriendsHelper.deleteRequest(uid);
            });
            requestsBox.addView(row);
        }
    }

    // ------------------------------------------------------------------ people
    private void renderPeople() {
        peopleBox.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(this);

        if (showFriends) {
            List<String> friends = new ArrayList<>(relations.friends);
            noPeople.setVisibility(friends.isEmpty() ? View.VISIBLE : View.GONE);
            noPeople.setText("You haven't added any friends yet");
            for (final String uid : friends) {
                View row = inf.inflate(R.layout.item_user_row, peopleBox, false);
                final AvatarView avatar = row.findViewById(R.id.userAvatar);
                final TextView name = row.findViewById(R.id.userName);
                final TextView sub = row.findViewById(R.id.userSub);
                final TextView action = row.findViewById(R.id.userAction);
                avatar.bind("?", null, uid);
                UserCache.get(uid, u -> {
                    if (u == null) return;
                    avatar.bind(u);
                    name.setText(u.displayName());
                    sub.setText(u.username == null || u.username.isEmpty() ? "" : "@" + u.username);
                });
                action.setText("Message");
                action.setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)
                        .putExtra(ChatActivity.EXTRA_PEER_ID, uid)));
                row.setOnClickListener(v -> openProfile(uid));
                row.setOnLongClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Unfriend?")
                            .setPositiveButton("Unfriend", (d, w) -> FriendsHelper.unfriend(uid))
                            .setNegativeButton("Cancel", null)
                            .show();
                    return true;
                });
                peopleBox.addView(row);
            }
            return;
        }

        int shown = 0;
        for (final User u : suggestions) {
            final int state = relations.stateOf(u.uid);
            if (state == FriendsHelper.SELF || state == FriendsHelper.FRIENDS || state == FriendsHelper.RECEIVED) continue;
            View row = inf.inflate(R.layout.item_user_row, peopleBox, false);
            ((AvatarView) row.findViewById(R.id.userAvatar)).bind(u);
            ((TextView) row.findViewById(R.id.userName)).setText(u.displayName());
            ((TextView) row.findViewById(R.id.userSub)).setText(u.username == null ? "" : "@" + u.username);
            TextView action = row.findViewById(R.id.userAction);
            action.setText(FriendsHelper.labelFor(state));
            boolean primary = state == FriendsHelper.NONE;
            action.setBackgroundResource(primary ? R.drawable.seagreen_button_10 : R.drawable.beige_button_10);
            action.setTextColor(primary ? 0xFFFFFFFF : 0xFF000000);
            action.setOnClickListener(v -> FriendsHelper.onButtonTapped(state, u.uid));
            row.setOnClickListener(v -> openProfile(u.uid));
            peopleBox.addView(row);
            shown++;
        }
        noPeople.setVisibility(shown == 0 ? View.VISIBLE : View.GONE);
        noPeople.setText(suggestionsLoaded ? "No suggestions right now. Try Search." : "Loading…");
    }

    private void openProfile(String uid) {
        startActivity(new Intent(this, ProfileActivity.class).putExtra(ProfileActivity.EXTRA_UID, uid));
    }

    @Override
    protected void onDestroy() {
        if (relations != null) relations.stop();
        super.onDestroy();
    }
}
