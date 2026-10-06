package com.example.smd_assignment1;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Screen 5 - Lina's post with the reaction picker. */
public class LikeActivity extends AppCompatActivity {

    private boolean liked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.like_screen);

        NavHelper.click(this, R.id.backarrow, this::finish);
        NavHelper.click(this, R.id.cancel, this::finish);
        NavHelper.click(this, R.id.horizontal_more, () -> NavHelper.toast(this, "More options"));

        Runnable react = () -> {
            liked = !liked;
            TextView word = findViewById(R.id.likeword);
            word.setTextColor(liked ? NavHelper.c("#0B5F63") : Color.parseColor("#8B898E"));
            NavHelper.toast(this, liked ? "You reacted" : "Reaction removed");
        };
        NavHelper.click(this, R.id.likeword, react);
        NavHelper.click(this, R.id.likeimg, react);

        NavHelper.click(this, R.id.commentword, this::openComments);
        NavHelper.click(this, R.id.commentimg, this::openComments);
        NavHelper.click(this, R.id.reactions, this::openComments);
        NavHelper.click(this, R.id.shareword, () -> NavHelper.toast(this, "Share"));
        NavHelper.click(this, R.id.shareimg, () -> NavHelper.toast(this, "Share"));
    }

    private void openComments() {
        startActivity(new Intent(this, CommentsActivity.class));
    }
}
