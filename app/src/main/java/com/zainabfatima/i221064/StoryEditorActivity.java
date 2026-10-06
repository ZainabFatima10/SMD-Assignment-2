package com.zainabfatima.i221064;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/** Screen 10 - Story editor. Sharing leads to "Your story". */
public class StoryEditorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.story_editor_screen);

        NavHelper.click(this, R.id.close_button, this::finish);

        NavHelper.click(this, R.id.tool_tag, () -> NavHelper.toast(this, "Tag people"));
        NavHelper.click(this, R.id.tool_text, () -> NavHelper.toast(this, "Add text"));
        NavHelper.click(this, R.id.tool_emoji, () -> NavHelper.toast(this, "Stickers"));
        NavHelper.click(this, R.id.tool_music, () -> NavHelper.toast(this, "Add music"));
        NavHelper.click(this, R.id.tool_effects, () -> NavHelper.toast(this, "Effects"));
        NavHelper.click(this, R.id.saturday_label, () -> NavHelper.toast(this, "Edit text"));

        // any of the three share controls publishes the story
        NavHelper.click(this, R.id.pill_your_story, this::share);
        NavHelper.click(this, R.id.pill_close_friends, this::share);
        NavHelper.click(this, R.id.btn_next, this::share);
    }

    private void share() {
        NavHelper.toast(this, "Story shared");
        startActivity(new Intent(this, YourStoryActivity.class));
        setResult(Activity.RESULT_OK);   // lets the camera close behind us
        finish();
    }
}
