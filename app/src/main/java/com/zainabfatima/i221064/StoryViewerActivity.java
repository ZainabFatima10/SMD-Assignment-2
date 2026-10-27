package com.zainabfatima.i221064;

import android.content.Intent;
import android.os.Bundle;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.chat.ChatRepo;
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

/** Screen 11 - A friend's story: plays all of their live stories and records that you saw them. */
public class StoryViewerActivity extends AppCompatActivity implements StoryPlayer.Listener {

    public static final String EXTRA_OWNER_ID = "owner_id";

    private String ownerId;
    private StoryPlayer player;
    private TextView nameView, timeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.story_viewer_screen);

        ownerId = getIntent().getStringExtra(EXTRA_OWNER_ID);
        if (ownerId == null || Db.uid() == null) {
            finish();
            return;
        }
        nameView = findViewById(R.id.story_name);
        timeView = findViewById(R.id.story_time);
        player = new StoryPlayer(this, this, R.id.karachi_label);

        final AvatarView avatar = findViewById(R.id.story_pfp);
        UserCache.get(ownerId, u -> {
            if (u == null) return;
            avatar.bind(u);
            nameView.setText(u.displayName());
        });

        NavHelper.click(this, R.id.close_button, this::finish);
        Runnable openProfile = () -> startActivity(new Intent(this, ProfileActivity.class)
                .putExtra(ProfileActivity.EXTRA_UID, ownerId));
        NavHelper.click(this, R.id.story_pfp, openProfile);
        NavHelper.click(this, R.id.story_name, openProfile);
        NavHelper.click(this, R.id.btn_more, () -> {
            PopupMenu pm = new PopupMenu(this, findViewById(R.id.btn_more));
            pm.getMenu().add(0, 1, 0, "View profile");
            pm.setOnMenuItemClickListener(item -> {
                openProfile.run();
                return true;
            });
            pm.show();
        });

        // replying to a story opens the conversation with its owner
        NavHelper.click(this, R.id.reply_input, () -> {
            player.pause();
            startActivity(new Intent(this, ChatActivity.class).putExtra(ChatActivity.EXTRA_PEER_ID, ownerId));
        });
        NavHelper.click(this, R.id.react_like, () -> react("👍"));
        NavHelper.click(this, R.id.react_love, () -> react("❤️"));
        NavHelper.click(this, R.id.react_haha, () -> react("😂"));

        loadStories();
    }

    private void react(String emoji) {
        ChatRepo.sendText(ownerId, emoji + " Reacted to your story");
        NavHelper.toast(this, "Reaction sent");
    }

    private void loadStories() {
        final String me = Db.uid();
        Db.stories(ownerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                List<Story> list = new ArrayList<>();
                long now = Clock.now();
                for (DataSnapshot c : s.getChildren()) {
                    Story st = c.getValue(Story.class);
                    if (st == null) continue;
                    if (st.id == null) st.id = c.getKey();
                    if (st.ownerId == null) st.ownerId = ownerId;
                    if (StoriesRepo.isVisible(st, me, now)) list.add(st);
                }
                if (list.isEmpty()) {
                    NavHelper.toast(StoryViewerActivity.this, "This story is no longer available");
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

    @Override
    public void onShown(Story story, int index) {
        timeView.setText(TimeUtil.ago(story.timestamp)
                + (Story.AUDIENCE_CLOSE.equals(story.audience) ? " • Close friends" : ""));
        Db.root().child("storyViews").child(story.id).child(Db.uid()).setValue(ServerValue.TIMESTAMP);
    }

    @Override
    public void onFinished() {
        finish();
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
        if (player != null) player.release();
        super.onDestroy();
    }
}
