package com.zainabfatima.i221064;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.model.Story;
import com.zainabfatima.i221064.util.Async;
import com.zainabfatima.i221064.util.AvatarView;
import com.zainabfatima.i221064.util.Clock;
import com.zainabfatima.i221064.util.Db;
import com.zainabfatima.i221064.util.ImageUtil;
import com.zainabfatima.i221064.util.UserCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Screen 10 - Story editor. Shows the captured / picked photo or video, lets the user add a text
 * label and shares it to "Your story" (all friends) or "Close friends". Stories live for 24 hours.
 */
public class StoryEditorActivity extends AppCompatActivity {

    public static final String EXTRA_URI = "uri";
    public static final String EXTRA_VIDEO = "video";

    private Uri mediaUri;
    private boolean isVideo;
    private String label = "";
    private TextView labelView;
    private boolean sharing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.story_editor_screen);

        String u = getIntent().getStringExtra(EXTRA_URI);
        if (u == null) {
            finish();
            return;
        }
        mediaUri = Uri.parse(u);
        isVideo = getIntent().getBooleanExtra(EXTRA_VIDEO, false);
        labelView = findViewById(R.id.saturday_label);

        ImageView image = findViewById(R.id.story_image);
        VideoView video = findViewById(R.id.story_video);
        if (isVideo) {
            image.setVisibility(View.GONE);
            video.setVisibility(View.VISIBLE);
            video.setVideoURI(mediaUri);
            video.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                mp.setVolume(0f, 0f);
                video.start();
            });
        } else {
            Glide.with(this).load(mediaUri).into(image);
        }

        // little avatar on the "Your story" pill
        String uid = Db.uid();
        if (uid != null) {
            UserCache.get(uid, user -> {
                AvatarView av = findViewById(R.id.pillAvatar);
                if (av != null && user != null) av.bind(user);
            });
        }

        NavHelper.click(this, R.id.close_button, this::finish);
        NavHelper.click(this, R.id.tool_tag, () -> NavHelper.toast(this, "Tagging isn't available yet"));
        NavHelper.click(this, R.id.tool_text, this::editLabel);
        NavHelper.click(this, R.id.saturday_label, this::editLabel);
        NavHelper.click(this, R.id.tool_emoji, () -> NavHelper.toast(this, "Stickers aren't available yet"));
        NavHelper.click(this, R.id.tool_music, () -> NavHelper.toast(this, "Music isn't available yet"));
        NavHelper.click(this, R.id.tool_effects, () -> NavHelper.toast(this, "Effects aren't available yet"));

        NavHelper.click(this, R.id.pill_your_story, () -> share(Story.AUDIENCE_STORY));
        NavHelper.click(this, R.id.btn_next, () -> share(Story.AUDIENCE_STORY));
        NavHelper.click(this, R.id.pill_close_friends, this::shareToCloseFriends);
    }

    private void editLabel() {
        final EditText input = new EditText(this);
        input.setText(label);
        input.setSingleLine(false);
        input.setHint("Add text to your story");
        new AlertDialog.Builder(this)
                .setTitle("Text")
                .setView(input)
                .setPositiveButton("Done", (d, w) -> {
                    label = input.getText().toString().trim();
                    labelView.setText(label);
                    labelView.setVisibility(label.isEmpty() ? View.GONE : View.VISIBLE);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ------------------------------------------------------------- close friends
    private void shareToCloseFriends() {
        final String me = Db.uid();
        if (me == null || sharing) return;
        Db.closeFriends(me).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                if (s.getChildrenCount() > 0) share(Story.AUDIENCE_CLOSE);
                else chooseCloseFriends();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        });
    }

    /** First time: let the user pick which friends are "close friends". */
    private void chooseCloseFriends() {
        final String me = Db.uid();
        Db.friends(me).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                final List<String> ids = new ArrayList<>();
                for (DataSnapshot c : s.getChildren()) ids.add(c.getKey());
                if (ids.isEmpty()) {
                    NavHelper.toast(StoryEditorActivity.this, "Add some friends first to use Close friends");
                    return;
                }
                final String[] names = new String[ids.size()];
                final boolean[] checked = new boolean[ids.size()];
                final int[] loaded = {0};
                for (int i = 0; i < ids.size(); i++) {
                    final int idx = i;
                    names[i] = "…";
                    UserCache.get(ids.get(i), user -> {
                        names[idx] = user == null ? ids.get(idx) : user.displayName();
                        if (++loaded[0] == ids.size()) showCloseDialog(ids, names, checked);
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        });
    }

    private void showCloseDialog(final List<String> ids, String[] names, final boolean[] checked) {
        new AlertDialog.Builder(this)
                .setTitle("Choose close friends")
                .setMultiChoiceItems(names, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("Save & share", (d, w) -> {
                    Map<String, Object> up = new HashMap<>();
                    boolean any = false;
                    for (int i = 0; i < ids.size(); i++) {
                        if (checked[i]) {
                            up.put(ids.get(i), true);
                            any = true;
                        }
                    }
                    if (!any) {
                        NavHelper.toast(this, "Pick at least one friend");
                        return;
                    }
                    Db.closeFriends(Db.uid()).updateChildren(up);
                    share(Story.AUDIENCE_CLOSE);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ------------------------------------------------------------------- upload
    private void share(final String audience) {
        final String me = Db.uid();
        if (me == null || sharing) return;
        sharing = true;
        NavHelper.toast(this, "Sharing your story…");

        if (!Story.AUDIENCE_CLOSE.equals(audience)) {
            upload(me, audience, null);
            return;
        }
        Db.closeFriends(me).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                Map<String, Boolean> vis = new HashMap<>();
                for (DataSnapshot c : s.getChildren()) vis.put(c.getKey(), true);
                upload(me, audience, vis);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                sharing = false;
            }
        });
    }

    private void upload(final String me, final String audience, final Map<String, Boolean> visibleTo) {
        final String id = Db.stories(me).push().getKey();
        Async.run(() -> {
            String[] out = new String[2];   // thumb, data
            if (isVideo) {
                long size = ImageUtil.sizeOf(this, mediaUri);
                if (size > ImageUtil.MAX_VIDEO_BYTES) throw new Exception("That video is too long. Keep it under ~10 seconds.");
                Bitmap frame = ImageUtil.videoFrame(this, mediaUri);
                out[0] = frame != null ? ImageUtil.scaledThumb(frame) : "";
                out[1] = ImageUtil.uriToBase64Raw(this, mediaUri);
            } else {
                Bitmap b = ImageUtil.decode(this, mediaUri, ImageUtil.STORY_DIM);
                out[0] = ImageUtil.scaledThumb(b);
                out[1] = ImageUtil.bitmapToBase64(b, 75);
            }
            return out;
        }, (out, err) -> {
            if (out == null) {
                sharing = false;
                NavHelper.toast(this, err != null && err.getMessage() != null ? err.getMessage() : "Couldn't prepare the story");
                return;
            }
            long now = Clock.now();
            Story st = new Story();
            st.id = id;
            st.ownerId = me;
            st.mediaType = isVideo ? Story.VIDEO : Story.IMAGE;
            st.thumb = out[0];
            st.text = label;
            st.audience = audience;
            if (visibleTo != null) st.visibleTo = visibleTo;
            st.timestamp = now;
            st.expiresAt = now + Story.LIFETIME_MS;

            Map<String, Object> media = new HashMap<>();
            media.put("ownerId", me);
            media.put("type", st.mediaType);
            media.put("data", out[1]);

            Map<String, Object> up = new HashMap<>();
            up.put("stories/" + me + "/" + id, st);
            up.put("storyMedia/" + id, media);
            Db.root().updateChildren(up).addOnCompleteListener(t -> {
                if (t.isSuccessful()) {
                    NavHelper.toast(this, "Story shared");
                    startActivity(new Intent(this, YourStoryActivity.class));
                    setResult(Activity.RESULT_OK);
                    finish();
                } else {
                    sharing = false;
                    NavHelper.toast(this, "Couldn't share the story. Check your connection.");
                }
            });
        });
    }
}
