package com.example.smd_assignment1;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

/** Screen 9 - Camera. The shutter leads to the story editor. */
public class CameraActivity extends AppCompatActivity {

    private boolean flashOn = false;

    // When the story has been shared, the editor reports back and the camera closes too.
    private final ActivityResultLauncher<Intent> editorLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) finish();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_screen);

        NavHelper.click(this, R.id.close_button, this::finish);
        NavHelper.click(this, R.id.settings_button, () -> NavHelper.toast(this, "Camera settings"));
        NavHelper.click(this, R.id.flash_button, () -> {
            flashOn = !flashOn;
            NavHelper.toast(this, flashOn ? "Flash on" : "Flash off");
        });
        NavHelper.click(this, R.id.flip_button, () -> NavHelper.toast(this, "Camera flipped"));
        NavHelper.click(this, R.id.emoji_button, () -> NavHelper.toast(this, "Effects"));

        // take the photo -> story editor
        NavHelper.click(this, R.id.shutter_button, this::openEditor);

        // gallery thumbnail -> photo picker (screen 8)
        NavHelper.click(this, R.id.gallery_thumbnail, () ->
                startActivity(new Intent(this, PictureSendActivity.class)));

        // mode strip
        NavHelper.click(this, R.id.mode_text, this::openEditor);
        NavHelper.click(this, R.id.mode_live, () -> NavHelper.toast(this, "Live"));
        NavHelper.click(this, R.id.mode_story, () -> NavHelper.toast(this, "You're in Story mode"));
        NavHelper.click(this, R.id.mode_post, () -> {
            startActivity(new Intent(this, CreatePostActivity.class));
            finish();
        });
        NavHelper.click(this, R.id.mode_boomerang, () -> NavHelper.toast(this, "Boomerang"));
    }

    private void openEditor() {
        editorLauncher.launch(new Intent(this, StoryEditorActivity.class));
    }
}
