package com.zainabfatima.i221064;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/** Screen 7 - Create post. */
public class CreatePostActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_post_screen);

        NavHelper.click(this, R.id.backarrow, this::finish);
        NavHelper.click(this, R.id.btnPost, () -> {
            NavHelper.toast(this, "Posted");
            finish();
        });

        NavHelper.click(this, R.id.public_button, () -> NavHelper.toast(this, "Audience: Public"));
        NavHelper.click(this, R.id.album_button, () -> NavHelper.toast(this, "Add to album"));
        NavHelper.click(this, R.id.post_text, () -> NavHelper.toast(this, "Edit post text"));

        // Bottom sheet rows
        NavHelper.click(this, R.id.line1, () -> startActivity(new Intent(this, PictureSendActivity.class))); // Photo/video
        NavHelper.click(this, R.id.line2, () -> NavHelper.toast(this, "Tag people"));
        NavHelper.click(this, R.id.line3, () -> NavHelper.toast(this, "Feeling/activity"));
        NavHelper.click(this, R.id.line4, () -> NavHelper.toast(this, "Check in"));
        NavHelper.click(this, R.id.line5, () -> NavHelper.toast(this, "Live video"));
        NavHelper.click(this, R.id.line6, () -> startActivity(new Intent(this, CameraActivity.class)));
        NavHelper.click(this, R.id.line7, () -> NavHelper.toast(this, "Create event"));
    }
}
