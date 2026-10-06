package com.example.smd_assignment1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;

/** Screen 12 - Your story. */
public class YourStoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.your_story_screen);

        NavHelper.click(this, R.id.close_button, this::finish);

        NavHelper.click(this, R.id.action_seen, () -> NavHelper.toast(this, "Seen by 24 people"));
        NavHelper.click(this, R.id.action_create, () ->
                startActivity(new Intent(this, CameraActivity.class)));
        NavHelper.click(this, R.id.action_highlight, () -> NavHelper.toast(this, "Added to highlights"));
        NavHelper.click(this, R.id.action_send, () -> NavHelper.toast(this, "Send story"));

        NavHelper.click(this, R.id.action_more, () -> {
            PopupMenu pm = new PopupMenu(this, findViewById(R.id.action_more));
            pm.getMenu().add(0, 1, 0, "Save photo");
            pm.getMenu().add(0, 2, 1, "Story settings");
            pm.getMenu().add(0, 3, 2, "Delete story");
            pm.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 3) {
                    NavHelper.toast(this, "Story deleted");
                    finish();
                } else {
                    NavHelper.toast(this, item.getTitle().toString());
                }
                return true;
            });
            pm.show();
        });
    }
}
