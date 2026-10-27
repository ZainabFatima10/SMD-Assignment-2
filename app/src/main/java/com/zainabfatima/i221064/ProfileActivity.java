package com.zainabfatima.i221064;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.model.Post;
import com.zainabfatima.i221064.model.User;
import com.zainabfatima.i221064.util.AvatarView;
import com.zainabfatima.i221064.util.Db;
import com.zainabfatima.i221064.util.FriendsHelper;
import com.zainabfatima.i221064.util.ImageUtil;
import com.zainabfatima.i221064.util.UserCache;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Screen 17 - Someone else's profile: picture, cover, bio, friend count, mutual friends,
 * the Add friend -> Requested -> Friends button and a Message button.
 */
public class ProfileActivity extends AppCompatActivity {

    public static final String EXTRA_UID = "uid";

    private String uid, me;
    private User user;
    private int state = FriendsHelper.NONE;
    private UserCache.Watch userWatch;
    private FriendsHelper.StateWatch stateWatch;
    private ValueEventListener friendsListener;
    private com.google.firebase.database.DatabaseReference friendsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        me = Db.uid();
        uid = getIntent().getStringExtra(EXTRA_UID);
        if (me == null || uid == null) {
            finish();
            return;
        }
        if (uid.equals(me)) {          // looking at yourself: show the editable profile instead
            startActivity(new Intent(this, MyProfileActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_profile);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSearch).setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        findViewById(R.id.btnMessage).setOnClickListener(v -> startActivity(
                new Intent(this, ChatActivity.class).putExtra(ChatActivity.EXTRA_PEER_ID, uid)));
        findViewById(R.id.btnAdd).setOnClickListener(v -> onAddTapped());
        findViewById(R.id.btnMore).setOnClickListener(this::showMore);
        ((View) findViewById(R.id.tvLives).getParent()).setVisibility(View.GONE);

        userWatch = UserCache.watch(uid, u -> {
            if (u == null) return;
            user = u;
            bindUser(u);
        });
        stateWatch = FriendsHelper.watch(uid, s -> {
            state = s;
            bindState();
        });
        loadCounts();
        loadPhotos();
    }

    private void bindUser(User u) {
        ((TextView) findViewById(R.id.profileTitle)).setText(u.displayName());
        ((TextView) findViewById(R.id.profileName)).setText(u.displayName());
        ((AvatarView) findViewById(R.id.avatar)).bind(u);
        ImageView cover = findViewById(R.id.cover);
        ImageUtil.show(cover, u.coverPic);
        TextView bio = findViewById(R.id.profileBio);
        bio.setText(u.bio == null ? "" : u.bio);
        bio.setVisibility(u.bio == null || u.bio.isEmpty() ? View.GONE : View.VISIBLE);
        ((TextView) findViewById(R.id.tvWork)).setText(
                Html.fromHtml(u.username == null || u.username.isEmpty() ? "" : "<b>@" + u.username + "</b>",
                        Html.FROM_HTML_MODE_LEGACY));
        TextView joined = findViewById(R.id.tvJoined);
        if (u.createdAt > 0) {
            joined.setText("Joined " + new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date(u.createdAt)));
        } else {
            ((View) joined.getParent()).setVisibility(View.GONE);
        }
    }

    private void bindState() {
        TextView label = findViewById(R.id.tvAdd);
        View button = findViewById(R.id.btnAdd);
        ImageView icon = findViewById(R.id.icAdd);
        boolean primary = state == FriendsHelper.NONE || state == FriendsHelper.RECEIVED;
        label.setText(state == FriendsHelper.RECEIVED ? "Confirm request" : FriendsHelper.labelFor(state));
        label.setTextColor(primary ? 0xFFFFFFFF : 0xFF1E1E1E);
        icon.setColorFilter(primary ? 0xFFFFFFFF : 0xFF1E1E1E);
        button.setBackgroundResource(primary ? R.drawable.bg_teal12 : R.drawable.bg_cream12);
    }

    private void onAddTapped() {
        if (state == FriendsHelper.FRIENDS) {
            new AlertDialog.Builder(this)
                    .setTitle("Unfriend " + (user == null ? "" : user.displayName()) + "?")
                    .setPositiveButton("Unfriend", (d, w) -> FriendsHelper.unfriend(uid))
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            FriendsHelper.onButtonTapped(state, uid);
        }
    }

    /** "N friends · M mutual" - both numbers are computed from the database. */
    private void loadCounts() {
        final TextView counts = findViewById(R.id.tvCounts);
        final TextView mutualText = findViewById(R.id.tvMutual);
        final View mutualRow = findViewById(R.id.mutualRow);
        mutualRow.setVisibility(View.GONE);

        friendsRef = Db.friends(uid);
        friendsListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot theirs) {
                final long total = theirs.getChildrenCount();
                final Set<String> theirIds = new HashSet<>();
                for (DataSnapshot c : theirs.getChildren()) theirIds.add(c.getKey());
                Db.friends(me).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot mine) {
                        int mutual = 0;
                        for (DataSnapshot c : mine.getChildren()) if (theirIds.contains(c.getKey())) mutual++;
                        counts.setText(Html.fromHtml("<b>" + total + "</b> " + (total == 1 ? "friend" : "friends")
                                + " · <b>" + mutual + "</b> mutual", Html.FROM_HTML_MODE_LEGACY));
                        if (mutual > 0) {
                            mutualRow.setVisibility(View.VISIBLE);
                            mutualText.setText(mutual + (mutual == 1 ? " mutual friend" : " mutual friends"));
                        } else {
                            mutualRow.setVisibility(View.GONE);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) { }
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        };
        friendsRef.addValueEventListener(friendsListener);
    }

    /** Up to three pictures from this person's posts. */
    private void loadPhotos() {
        Query q = Db.posts().orderByChild("authorId").equalTo(uid).limitToLast(15);
        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                List<String> pics = new ArrayList<>();
                List<String> postIds = new ArrayList<>();
                for (DataSnapshot c : s.getChildren()) {
                    Post p = c.getValue(Post.class);
                    if (p == null || p.images == null) continue;
                    for (String img : p.images) {
                        pics.add(img);
                        postIds.add(c.getKey());
                    }
                }
                if (pics.isEmpty()) return;
                findViewById(R.id.photosSection).setVisibility(View.VISIBLE);
                int[] ids = {R.id.ph1, R.id.ph2, R.id.ph3};
                for (int i = 0; i < ids.length; i++) {
                    ImageView iv = findViewById(ids[i]);
                    int idx = pics.size() - 1 - i;      // newest first
                    if (idx < 0) {
                        iv.setVisibility(View.INVISIBLE);
                        continue;
                    }
                    iv.setVisibility(View.VISIBLE);
                    ImageUtil.show(iv, pics.get(idx));
                    final String postId = postIds.get(idx);
                    iv.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, LikeActivity.class)
                            .putExtra(LikeActivity.EXTRA_POST_ID, postId)));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        });
    }

    private void showMore(View anchor) {
        PopupMenu pm = new PopupMenu(this, anchor);
        pm.getMenu().add(0, 1, 0, "Message");
        if (state == FriendsHelper.FRIENDS) pm.getMenu().add(0, 2, 1, "Unfriend");
        if (state == FriendsHelper.RECEIVED) pm.getMenu().add(0, 3, 2, "Delete request");
        pm.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: findViewById(R.id.btnMessage).performClick(); break;
                case 2: onAddTapped(); break;
                case 3: FriendsHelper.deleteRequest(uid); break;
            }
            return true;
        });
        pm.show();
    }

    @Override
    protected void onDestroy() {
        if (userWatch != null) userWatch.stop();
        if (stateWatch != null) stateWatch.stop();
        if (friendsRef != null && friendsListener != null) friendsRef.removeEventListener(friendsListener);
        super.onDestroy();
    }
}
