package com.zainabfatima.i221064;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.feed.CommentAdapter;
import com.zainabfatima.i221064.feed.PostRepo;
import com.zainabfatima.i221064.model.AppNotification;
import com.zainabfatima.i221064.model.Comment;
import com.zainabfatima.i221064.model.Post;
import com.zainabfatima.i221064.util.Async;
import com.zainabfatima.i221064.util.Clock;
import com.zainabfatima.i221064.util.Db;
import com.zainabfatima.i221064.util.ImageUtil;
import com.zainabfatima.i221064.util.Notifier;
import com.zainabfatima.i221064.util.Reactions;
import com.zainabfatima.i221064.util.UserCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Screen 6 - Comments. Real-time comments and replies, comment likes, image comments, the
 * "Replying to ... / Cancel" bar and a live reaction summary for the post.
 */
public class CommentsActivity extends AppCompatActivity implements CommentAdapter.Actions {

    public static final String EXTRA_POST_ID = "post_id";

    private String postId;
    private Post post;
    private CommentAdapter adapter;
    private RecyclerView list;
    private TextView empty, reactionNumber, replyName;
    private LinearLayout reactionIcons;
    private View replyBar, attachPreview;
    private EditText input;
    private ImageView attachImage;

    private Comment replyingTo;
    private Uri attached;
    private boolean sending;

    private DatabaseReference commentsRef, likesRef, reactRef;
    private ValueEventListener commentsListener, likesListener, reactListener;

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() != RESULT_OK || r.getData() == null) return;
                ArrayList<String> uris = r.getData().getStringArrayListExtra(PictureSendActivity.RESULT_URIS);
                if (uris == null || uris.isEmpty()) return;
                attached = Uri.parse(uris.get(0));
                attachPreview.setVisibility(View.VISIBLE);
                Glide.with(this).load(attached).into(attachImage);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.comments_screen);

        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        if (postId == null) {
            finish();
            return;
        }
        list = findViewById(R.id.commentList);
        empty = findViewById(R.id.emptyComments);
        reactionNumber = findViewById(R.id.reaction_number);
        reactionIcons = findViewById(R.id.reactionIcons);
        replyBar = findViewById(R.id.reply_bar);
        replyName = findViewById(R.id.ak);
        attachPreview = findViewById(R.id.attach_preview);
        attachImage = findViewById(R.id.attach_image);
        input = findViewById(R.id.prompt);

        adapter = new CommentAdapter(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        NavHelper.click(this, R.id.backarrow, this::finish);
        NavHelper.click(this, R.id.reactions, () -> startActivity(
                new Intent(this, LikeActivity.class).putExtra(LikeActivity.EXTRA_POST_ID, postId)));
        NavHelper.click(this, R.id.cancel, this::clearReply);
        NavHelper.click(this, R.id.camera, () -> pickImage.launch(new Intent(this, PictureSendActivity.class)
                .putExtra(PictureSendActivity.EXTRA_MULTI, false)));
        NavHelper.click(this, R.id.attach_remove, () -> {
            attached = null;
            attachPreview.setVisibility(View.GONE);
        });
        NavHelper.click(this, R.id.emoji_button, () -> {
            int pos = Math.max(0, input.getSelectionStart());
            input.getText().insert(pos, "😊");
        });
        NavHelper.click(this, R.id.btn_send, this::send);

        Db.posts().child(postId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                post = s.getValue(Post.class);
                if (post != null && post.id == null) post.id = s.getKey();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        });

        listen();
    }

    private void listen() {
        commentsRef = Db.comments(postId);
        commentsListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                List<Comment> out = new ArrayList<>();
                for (DataSnapshot c : s.getChildren()) {
                    Comment cm = c.getValue(Comment.class);
                    if (cm == null) continue;
                    if (cm.id == null) cm.id = c.getKey();
                    out.add(cm);
                }
                adapter.setComments(out);
                empty.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        };
        commentsRef.addValueEventListener(commentsListener);

        likesRef = Db.commentLikes(postId);
        likesListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                Map<String, Set<String>> map = new HashMap<>();
                for (DataSnapshot c : s.getChildren()) {
                    Set<String> who = new HashSet<>();
                    for (DataSnapshot u : c.getChildren()) who.add(u.getKey());
                    map.put(c.getKey(), who);
                }
                adapter.setLikes(map);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        };
        likesRef.addValueEventListener(likesListener);

        reactRef = Db.postReactions(postId);
        reactListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                int[] counts = new int[Reactions.COUNT];
                long total = 0;
                boolean mine = false;
                for (DataSnapshot c : s.getChildren()) {
                    Long t = c.getValue(Long.class);
                    if (t == null) continue;
                    total++;
                    if (t >= 0 && t < Reactions.COUNT) counts[t.intValue()]++;
                    if (c.getKey().equals(Db.uid())) mine = true;
                }
                bindReactions(counts, total, mine);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        };
        reactRef.addValueEventListener(reactListener);
    }

    private void bindReactions(final int[] counts, long total, boolean mine) {
        reactionIcons.removeAllViews();
        if (total == 0) {
            reactionNumber.setText("Be the first to react");
            return;
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < Reactions.COUNT; i++) if (counts[i] > 0) order.add(i);
        order.sort((a, b) -> counts[b] - counts[a]);
        float d = getResources().getDisplayMetrics().density;
        for (int i = 0; i < Math.min(3, order.size()); i++) {
            ImageView iv = new ImageView(this);
            iv.setImageResource(Reactions.icon(order.get(i)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Math.round(22 * d), Math.round(22 * d));
            if (i > 0) lp.setMarginStart(Math.round(-6 * d));
            reactionIcons.addView(iv, lp);
        }
        reactionNumber.setText(mine
                ? (total == 1 ? "You" : "You and " + (total - 1) + (total == 2 ? " other" : " others"))
                : String.valueOf(total));
    }

    // ------------------------------------------------------------ comment actions
    @Override
    public void onReply(Comment c) {
        replyingTo = c;
        replyBar.setVisibility(View.VISIBLE);
        replyName.setText("...");
        UserCache.get(c.authorId, u -> {
            if (replyingTo == c && u != null) replyName.setText(u.displayName());
        });
        input.requestFocus();
    }

    private void clearReply() {
        replyingTo = null;
        replyBar.setVisibility(View.GONE);
    }

    @Override
    public void onLikeToggle(Comment c, boolean currentlyLiked) {
        String me = Db.uid();
        if (me == null) return;
        DatabaseReference ref = Db.commentLikes(postId).child(c.id).child(me);
        if (currentlyLiked) ref.removeValue();
        else {
            ref.setValue(true);
            Notifier.send(c.authorId, AppNotification.REACTION, postId, "liked your comment.");
        }
    }

    // ------------------------------------------------------------------- sending
    private void send() {
        if (sending) return;
        final String body = input.getText().toString().trim();
        if (body.isEmpty() && attached == null) return;
        final String me = Db.uid();
        if (me == null) return;
        if (post == null) {
            NavHelper.toast(this, "Loading the post, try again in a moment");
            return;
        }
        sending = true;

        // replies are always attached to the top-level comment so threads stay one level deep
        final Comment target = replyingTo;
        final String parentId = target == null ? null : (target.parentId == null || target.parentId.isEmpty() ? target.id : target.parentId);
        final Uri img = attached;

        Async.run(() -> img == null ? "" : ImageUtil.uriToBase64(this, img, ImageUtil.CHAT_DIM, 70), (b64, err) -> {
            if (b64 == null) {
                sending = false;
                NavHelper.toast(this, "Couldn't read that image");
                return;
            }
            String id = Db.comments(postId).push().getKey();
            Map<String, Object> c = new HashMap<>();
            c.put("id", id);
            c.put("postId", postId);
            c.put("authorId", me);
            c.put("text", body);
            if (!b64.isEmpty()) c.put("image", b64);
            if (parentId != null) c.put("parentId", parentId);
            c.put("timestamp", Clock.now());
            PostRepo.addComment(post, c, id);
            if (target != null && !target.authorId.equals(post.authorId)) {
                Notifier.send(target.authorId, AppNotification.COMMENT, postId, "replied to your comment.");
            }

            input.setText("");
            attached = null;
            attachPreview.setVisibility(View.GONE);
            clearReply();
            sending = false;
            list.postDelayed(() -> list.scrollToPosition(Math.max(0, adapter.getItemCount() - 1)), 300);
        });
    }

    @Override
    protected void onDestroy() {
        if (commentsRef != null && commentsListener != null) commentsRef.removeEventListener(commentsListener);
        if (likesRef != null && likesListener != null) likesRef.removeEventListener(likesListener);
        if (reactRef != null && reactListener != null) reactRef.removeEventListener(reactListener);
        super.onDestroy();
    }
}
