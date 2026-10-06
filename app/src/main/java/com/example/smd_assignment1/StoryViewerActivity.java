package com.example.smd_assignment1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;

/** Screen 11 - Story viewer (Omar Farooq's story). */
public class StoryViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.story_viewer_screen);

        NavHelper.click(this, R.id.close_button, this::finish);

        // header: tap the avatar or name to visit his profile (screen 17)
        Runnable openProfile = () -> startActivity(new Intent(this, ProfileActivity.class));
        NavHelper.click(this, R.id.story_pfp, openProfile);
        NavHelper.click(this, R.id.story_name, openProfile);

        NavHelper.click(this, R.id.btn_more, () -> {
            PopupMenu pm = new PopupMenu(this, findViewById(R.id.btn_more));
            pm.getMenu().add(0, 1, 0, "View profile");
            pm.getMenu().add(0, 2, 1, "Mute Omar's stories");
            pm.getMenu().add(0, 3, 2, "Report");
            pm.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) openProfile.run();
                else NavHelper.toast(this, item.getTitle().toString());
                return true;
            });
            pm.show();
        });

        // replying to a story opens the conversation
        NavHelper.click(this, R.id.reply_input, () -> {
            Intent i = new Intent(this, ChatActivity.class);
            i.putExtra("name", "Omar Farooq");
            i.putExtra("initials", "OF");
            i.putExtra("color", "#2F6E4E");
            startActivity(i);
        });

        NavHelper.click(this, R.id.react_like, () -> NavHelper.toast(this, "You liked Omar's story"));
        NavHelper.click(this, R.id.react_love, () -> NavHelper.toast(this, "You loved Omar's story"));
        NavHelper.click(this, R.id.react_haha, () -> NavHelper.toast(this, "You reacted with haha"));
    }
}
