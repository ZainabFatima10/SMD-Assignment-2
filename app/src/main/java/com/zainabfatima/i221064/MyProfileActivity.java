package com.zainabfatima.i221064;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.model.User;
import com.zainabfatima.i221064.util.Async;
import com.zainabfatima.i221064.util.AvatarView;
import com.zainabfatima.i221064.util.Db;
import com.zainabfatima.i221064.util.ImageUtil;
import com.zainabfatima.i221064.util.UserCache;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Screen 15 - Your own profile, backed by /users/{uid}. Pictures can be changed right here. */
public class MyProfileActivity extends AppCompatActivity {

    private TextView[] tabs;
    private String me;
    private UserCache.Watch watch;
    private DatabaseReference friendsRef;
    private ValueEventListener friendsListener;

    private final ActivityResultLauncher<Intent> avatarPicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> handlePick(r.getResultCode(), r.getData(), true));
    private final ActivityResultLauncher<Intent> coverPicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> handlePick(r.getResultCode(), r.getData(), false));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_screen);
        me = Db.uid();
        if (me == null) {
            finish();
            return;
        }

        NavHelper.click(this, R.id.backarrow, this::finish);
        NavHelper.click(this, R.id.btn_search, () -> startActivity(new Intent(this, SearchActivity.class)));

        NavHelper.click(this, R.id.btn_cover_camera, () -> coverPicker.launch(singlePick()));
        NavHelper.click(this, R.id.btn_avatar_camera, () -> avatarPicker.launch(singlePick()));

        NavHelper.click(this, R.id.btn_add_story, () -> startActivity(new Intent(this, CameraActivity.class)));
        NavHelper.click(this, R.id.btn_edit_profile, () -> startActivity(new Intent(this, EditProfileActivity.class)));

        NavHelper.click(this, R.id.btn_more, () -> {
            PopupMenu pm = new PopupMenu(this, findViewById(R.id.btn_more));
            pm.getMenu().add(0, 1, 0, "Edit profile");
            pm.getMenu().add(0, 2, 1, "Menu");
            pm.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) startActivity(new Intent(this, EditProfileActivity.class));
                else startActivity(new Intent(this, MenuActivity.class));
                return true;
            });
            pm.show();
        });

        tabs = new TextView[]{findViewById(R.id.tab_posts), findViewById(R.id.tab_photos),
                findViewById(R.id.tab_videos), findViewById(R.id.tab_groups)};
        final int[] ids = {R.id.tab_posts, R.id.tab_photos, R.id.tab_videos, R.id.tab_groups};
        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            NavHelper.click(this, ids[i], () -> {
                for (int j = 0; j < tabs.length; j++) NavHelper.chip(tabs[j], j == index, false);
            });
        }
        NavHelper.click(this, R.id.find_friends, () -> startActivity(new Intent(this, SearchActivity.class)));

        watch = UserCache.watch(me, this::bind);

        friendsRef = Db.friends(me);
        friendsListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                bindFriends(s);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        };
        friendsRef.addValueEventListener(friendsListener);
    }

    private Intent singlePick() {
        return new Intent(this, PictureSendActivity.class).putExtra(PictureSendActivity.EXTRA_MULTI, false);
    }

    private void handlePick(int resultCode, Intent data, final boolean avatar) {
        if (resultCode != RESULT_OK || data == null) return;
        ArrayList<String> uris = data.getStringArrayListExtra(PictureSendActivity.RESULT_URIS);
        if (uris == null || uris.isEmpty()) return;
        final Uri uri = Uri.parse(uris.get(0));
        Async.run(() -> ImageUtil.uriToBase64(this, uri, avatar ? ImageUtil.PROFILE_DIM : ImageUtil.COVER_DIM, 80),
                (b64, err) -> {
                    if (b64 == null) {
                        NavHelper.toast(this, "Couldn't use that picture");
                        return;
                    }
                    Db.user(me).child(avatar ? "profilePic" : "coverPic").setValue(b64);
                    NavHelper.toast(this, avatar ? "Profile picture updated" : "Cover photo updated");
                });
    }

    private void bind(User u) {
        if (u == null) return;
        ((TextView) findViewById(R.id.my_title)).setText(u.displayName());
        ((TextView) findViewById(R.id.profile_name)).setText(u.displayName());
        TextView bio = findViewById(R.id.profile_bio);
        bio.setText(u.bio == null ? "" : u.bio);
        bio.setVisibility(u.bio == null || u.bio.isEmpty() ? View.INVISIBLE : View.VISIBLE);
        ((AvatarView) findViewById(R.id.my_avatar)).bind(u);
        ImageUtil.show((android.widget.ImageView) findViewById(R.id.cover_image), u.coverPic);

        ((TextView) findViewById(R.id.detail_work_text)).setText(
                u.username == null || u.username.isEmpty() ? "No username yet" : "@" + u.username);
        ((TextView) findViewById(R.id.detail_school_text)).setText(u.email == null ? "" : u.email);
        ((TextView) findViewById(R.id.detail_location_text)).setText(
                u.birthday == null || u.birthday.isEmpty() ? "Birthday not set" : "Born " + u.birthday);
        ((TextView) findViewById(R.id.detail_joined_text)).setText(u.createdAt > 0
                ? "Joined " + new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date(u.createdAt))
                : "");
    }

    private void bindFriends(DataSnapshot s) {
        long n = s.getChildrenCount();
        ((TextView) findViewById(R.id.friends_count)).setText(String.valueOf(n));
        ((TextView) findViewById(R.id.friends_total)).setText(n + (n == 1 ? " friend" : " friends"));

        List<String> ids = new ArrayList<>();
        for (DataSnapshot c : s.getChildren()) ids.add(c.getKey());
        int[] cards = {R.id.friend_lm, R.id.friend_ak, R.id.friend_of};
        int[] texts = {R.id.f1_text, R.id.f2_text, R.id.f3_text};
        for (int i = 0; i < cards.length; i++) {
            View card = findViewById(cards[i]);
            if (i >= ids.size()) {
                card.setVisibility(View.INVISIBLE);
                continue;
            }
            card.setVisibility(View.VISIBLE);
            final String id = ids.get(i);
            final TextView t = findViewById(texts[i]);
            t.setText("");
            UserCache.get(id, u -> t.setText(u == null ? "" : u.initials()));
            card.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)
                    .putExtra(ProfileActivity.EXTRA_UID, id)));
        }
    }

    @Override
    protected void onDestroy() {
        if (watch != null) watch.stop();
        if (friendsRef != null && friendsListener != null) friendsRef.removeEventListener(friendsListener);
        super.onDestroy();
    }
}
