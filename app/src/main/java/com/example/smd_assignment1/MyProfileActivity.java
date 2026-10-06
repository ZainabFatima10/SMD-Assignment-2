package com.example.smd_assignment1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Screen 15 - Your own profile (Jacob West). Screen 17 is someone else's profile. */
public class MyProfileActivity extends AppCompatActivity {

    private TextView[] tabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_screen);

        NavHelper.click(this, R.id.backarrow, this::finish);
        NavHelper.click(this, R.id.btn_search, () -> startActivity(new Intent(this, SearchActivity.class)));

        NavHelper.click(this, R.id.btn_cover_camera, () ->
                startActivity(new Intent(this, PictureSendActivity.class)));       // change cover photo
        NavHelper.click(this, R.id.btn_avatar_camera, () ->
                startActivity(new Intent(this, PictureSendActivity.class)));       // change profile picture

        NavHelper.click(this, R.id.btn_add_story, () -> startActivity(new Intent(this, CameraActivity.class)));
        NavHelper.click(this, R.id.btn_edit_profile, () -> startActivity(new Intent(this, EditProfileActivity.class)));

        NavHelper.click(this, R.id.btn_more, () -> {
            PopupMenu pm = new PopupMenu(this, findViewById(R.id.btn_more));
            pm.getMenu().add(0, 1, 0, "Edit profile");
            pm.getMenu().add(0, 2, 1, "View as");
            pm.getMenu().add(0, 3, 2, "Archive");
            pm.getMenu().add(0, 4, 3, "Menu");
            pm.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case 1: startActivity(new Intent(this, EditProfileActivity.class)); break;
                    case 4: startActivity(new Intent(this, MenuActivity.class)); break;
                    default: NavHelper.toast(this, item.getTitle().toString());
                }
                return true;
            });
            pm.show();
        });

        // ---- Posts / Photos / Videos / Groups ----
        tabs = new TextView[]{findViewById(R.id.tab_posts), findViewById(R.id.tab_photos),
                findViewById(R.id.tab_videos), findViewById(R.id.tab_groups)};
        final int[] ids = {R.id.tab_posts, R.id.tab_photos, R.id.tab_videos, R.id.tab_groups};
        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            NavHelper.click(this, ids[i], () -> {
                for (int j = 0; j < tabs.length; j++) NavHelper.chip(tabs[j], j == index, false);
            });
        }

        // ---- friends ----
        NavHelper.click(this, R.id.find_friends, () -> startActivity(new Intent(this, FriendsActivity.class)));
        NavHelper.click(this, R.id.friend_lm, () -> NavHelper.toast(this, "Lina Marsh"));
        NavHelper.click(this, R.id.friend_ak, () -> NavHelper.toast(this, "Aisha Khan"));
        NavHelper.click(this, R.id.friend_of, () -> startActivity(new Intent(this, ProfileActivity.class)));
    }
}
