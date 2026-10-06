package com.example.smd_assignment1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

/** Screen 6 - Comments. */
public class CommentsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.comments_screen);

        NavHelper.click(this, R.id.backarrow, this::finish);
        NavHelper.click(this, R.id.most_relevant_text, () -> NavHelper.toast(this, "Most relevant"));
        NavHelper.click(this, R.id.reactions, () -> startActivity(new Intent(this, LikeActivity.class)));
        NavHelper.click(this, R.id.viewMoreReplies, () -> NavHelper.toast(this, "Loading replies"));

        // "Replying to ..." bar: Reply shows it, Cancel hides it
        final int[] replyBar = {R.id.replyingto, R.id.ak, R.id.cancel};
        Runnable showBar = () -> setBar(replyBar, View.VISIBLE);
        Runnable hideBar = () -> setBar(replyBar, View.GONE);
        NavHelper.click(this, R.id.cancel, hideBar);
        NavHelper.click(this, R.id.statusupdate3, showBar);
        NavHelper.click(this, R.id.statusupdate3RL4, showBar);
        NavHelper.click(this, R.id.statusupdate3RL5, showBar);
        NavHelper.click(this, R.id.statusupdate3RL6, showBar);

        // Like buttons on each comment
        NavHelper.click(this, R.id.statusupdate2, () -> NavHelper.toast(this, "Liked"));
        NavHelper.click(this, R.id.statusupdate2RL4, () -> NavHelper.toast(this, "Liked"));
        NavHelper.click(this, R.id.statusupdate2RL5, () -> NavHelper.toast(this, "Liked"));
        NavHelper.click(this, R.id.statusupdate2RL6, () -> NavHelper.toast(this, "Liked"));

        // Composer
        NavHelper.click(this, R.id.camera, () -> startActivity(new Intent(this, PictureSendActivity.class)));
        NavHelper.click(this, R.id.emoji_button, () -> NavHelper.toast(this, "Emoji"));
        NavHelper.click(this, R.id.prompt, () -> NavHelper.toast(this, "Write a reply"));
    }

    private void setBar(int[] ids, int visibility) {
        for (int id : ids) {
            View v = findViewById(id);
            if (v != null) v.setVisibility(visibility);
        }
    }
}
