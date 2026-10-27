package com.zainabfatima.i221064;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.feed.StoriesRepo;
import com.zainabfatima.i221064.feed.StoryPlayer;
import com.zainabfatima.i221064.model.Story;
import com.zainabfatima.i221064.util.AvatarView;
import com.zainabfatima.i221064.util.Clock;
import com.zainabfatima.i221064.util.Db;
import com.zainabfatima.i221064.util.TimeUtil;
import com.zainabfatima.i221064.util.UserCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Screen 12 - Your story: plays your own live stories, shows who saw them and lets you delete them. */
public class YourStoryActivity extends AppCompatActivity implements StoryPlayer.Listener {

    private StoryPlayer player;
    private TextView timeView, seenView;
    private DatabaseReference viewsRef;
    private ValueEventListener viewsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.your_story_screen);

        final String me = Db.uid();
        if (me == null) {
            finish();
            return;
        }
        timeView = findViewById(R.id.story_time);
        seenView = findViewById(R.id.seen_count);
        player = new StoryPlayer(this, this, R.id.saturday_label);

        ((TextView) findViewById(R.id.story_name)).setText(R.string.your_story);
        final AvatarView avatar = findViewById(R.id.story_pfp);
        UserCache.get(me, u -> {
            if (u != null) avatar.bind(u);
        });

        findViewById(R.id.action_highlight).setVisibility(View.GONE);
        findViewById(R.id.action_send).setVisibility(View.GONE);

        NavHelper.click(this, R.id.close_button, this::finish);
        NavHelper.click(this, R.id.action_seen, () -> NavHelper.toast(this, seenView.getText().toString()));
        NavHelper.click(this, R.id.action_create, () -> startActivity(new Intent(this, CameraActivity.class)));
        NavHelper.click(this, R.id.action_more, () -> {
            PopupMenu pm = new PopupMenu(this, findViewById(R.id.action_more));
            pm.getMenu().add(0, 1, 0, "Delete story");
            pm.setOnMenuItemClickListener(item -> {
                confirmDelete();
                return true;
            });
            pm.show();
        });

        Db.stories(me).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                List<Story> list = new ArrayList<>();
                long now = Clock.now();
                for (DataSnapshot c : s.getChildren()) {
                    Story st = c.getValue(Story.class);
                    if (st == null) continue;
                    if (st.id == null) st.id = c.getKey();
                    if (st.ownerId == null) st.ownerId = me;
                    if (st.expiresAt > now) list.add(st);
                    else StoriesRepo.purge(st);
                }
                if (list.isEmpty()) {
                    NavHelper.toast(YourStoryActivity.this, "You have no active stories");
                    finish();
                    return;
                }
                Collections.sort(list, (x, y) -> Long.compare(x.timestamp, y.timestamp));
                player.start(list, 0);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                finish();
            }
        });
    }

    private void confirmDelete() {
        final Story s = player.current();
        if (s == null) return;
        player.pause();
        new AlertDialog.Builder(this)
                .setTitle("Delete this story?")
                .setMessage("It will be removed for everyone.")
                .setPositiveButton("Delete", (d, w) -> {
                    StoriesRepo.purge(s);
                    player.resume();
                    player.removeCurrent();
                })
                .setNegativeButton("Cancel", (d, w) -> player.resume())
                .setOnCancelListener(d -> player.resume())
                .show();
    }

    @Override
    public void onShown(Story story, int index) {
        timeView.setText(TimeUtil.ago(story.timestamp)
                + (Story.AUDIENCE_CLOSE.equals(story.audience) ? " • Close friends" : " • Your friends"));
        seenView.setText("Seen by 0");
        detachViews();
        viewsRef = Db.root().child("storyViews").child(story.id);
        viewsListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                long n = s.getChildrenCount();
                if (s.hasChild(Db.uid())) n--;
                seenView.setText("Seen by " + Math.max(0, n));
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        };
        viewsRef.addValueEventListener(viewsListener);
    }

    @Override
    public void onFinished() {
        finish();
    }

    private void detachViews() {
        if (viewsRef != null && viewsListener != null) viewsRef.removeEventListener(viewsListener);
        viewsRef = null;
        viewsListener = null;
    }

    @Override protected void onPause() {
        super.onPause();
        if (player != null) player.pause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (player != null) player.resume();
    }

    @Override protected void onDestroy() {
        detachViews();
        if (player != null) player.release();
        super.onDestroy();
    }
}
