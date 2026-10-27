package com.zainabfatima.i221064;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.feed.FeedRepo;
import com.zainabfatima.i221064.feed.HomeAdapter;
import com.zainabfatima.i221064.feed.StoriesRepo;
import com.zainabfatima.i221064.model.ChatSummary;
import com.zainabfatima.i221064.model.User;
import com.zainabfatima.i221064.util.Db;
import com.zainabfatima.i221064.util.Session;
import com.zainabfatima.i221064.util.UserCache;

/**
 * Screen 4 - Home. A RecyclerView whose header is the composer + stories row ("Create story" first)
 * and whose rows are the posts of you and your friends, newest first.
 */
public class StartSocialMediaActivity extends AppCompatActivity {

    private final HomeAdapter adapter = new HomeAdapter();
    private FeedRepo feed;
    private StoriesRepo stories;
    private UserCache.Watch meWatch;
    private DatabaseReference chatsRef;
    private ValueEventListener chatsListener;

    private final ActivityResultLauncher<String> notifPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Db.isLoggedIn()) {      // e.g. session expired: back to Log in
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return;
        }
        setContentView(R.layout.start_social_media_screen);
        Session.onLogin(this);

        // ---- header + tab bar ----
        NavHelper.click(this, R.id.messageIcon, () -> open(ChatsActivity.class));
        NavHelper.click(this, R.id.btnSearch, () -> open(SearchActivity.class));
        NavHelper.click(this, R.id.tabHome, () -> NavHelper.toast(this, "You're on Home"));
        NavHelper.click(this, R.id.tabPeople, () -> open(FriendsActivity.class));
        NavHelper.click(this, R.id.tabShop, () -> open(MarketplaceActivity.class));
        NavHelper.click(this, R.id.tabNotif, () -> open(NotificationsActivity.class));
        NavHelper.click(this, R.id.tabMore, () -> open(MenuActivity.class));

        RecyclerView feedView = findViewById(R.id.feed);
        feedView.setLayoutManager(new LinearLayoutManager(this));
        feedView.setAdapter(adapter);
        feedView.setItemAnimator(null);

        feed = new FeedRepo(posts -> adapter.setPosts(posts));
        stories = new StoriesRepo((mine, friends) -> adapter.setStories(mine, friends));

        requestNotificationPermission();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!Db.isLoggedIn() || feed == null) return;
        final String uid = Db.uid();

        // a first-time user (no username yet) must finish profile setup before seeing Home
        meWatch = UserCache.watch(uid, u -> {
            if (u == null) return;
            if (!u.profileComplete) {
                Intent i = new Intent(this, EditProfileActivity.class);
                i.putExtra(EditProfileActivity.EXTRA_SETUP, true);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
                return;
            }
            adapter.setMe(u);
        });
        feed.start();
        stories.start();

        // unread badge on the messenger icon: the sum of unread over all conversations
        chatsRef = Db.userChats(uid);
        chatsListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                long unread = 0;
                for (DataSnapshot c : s.getChildren()) {
                    ChatSummary cs = c.getValue(ChatSummary.class);
                    if (cs != null) unread += cs.unread;
                }
                TextView badge = findViewById(R.id.tvUnreadBadge);
                badge.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
                badge.setText(unread > 99 ? "99+" : String.valueOf(unread));
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        };
        chatsRef.addValueEventListener(chatsListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (meWatch != null) meWatch.stop();
        meWatch = null;
        if (feed != null) feed.stop();
        if (stories != null) stories.stop();
        if (chatsRef != null && chatsListener != null) chatsRef.removeEventListener(chatsListener);
        chatsRef = null;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void open(Class<?> target) {
        startActivity(new Intent(this, target));
    }
}
