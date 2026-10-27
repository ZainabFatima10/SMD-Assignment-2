package com.zainabfatima.i221064;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.feed.PostView;
import com.zainabfatima.i221064.model.Post;
import com.zainabfatima.i221064.util.Db;

/**
 * Screen 5 - A single post with the reaction picker (long-press Like). Reactions and comment
 * count update in real time.
 */
public class LikeActivity extends AppCompatActivity {

    public static final String EXTRA_POST_ID = "post_id";

    private PostView postView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.like_screen);
        NavHelper.click(this, R.id.backarrow, this::finish);

        String postId = getIntent().getStringExtra(EXTRA_POST_ID);
        if (postId == null) {
            finish();
            return;
        }
        View root = findViewById(R.id.postRoot);
        postView = new PostView(root, post -> finish());

        Db.posts().child(postId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                Post p = s.getValue(Post.class);
                if (p == null) {
                    NavHelper.toast(LikeActivity.this, "This post is no longer available");
                    finish();
                    return;
                }
                if (p.id == null) p.id = s.getKey();
                postView.bind(p);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (postView != null) postView.unbind();
        super.onDestroy();
    }
}
