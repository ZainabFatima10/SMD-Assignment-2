package com.zainabfatima.i221064.feed;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.CommentsActivity;
import com.zainabfatima.i221064.LikeActivity;
import com.zainabfatima.i221064.R;
import com.zainabfatima.i221064.model.Post;
import com.zainabfatima.i221064.model.User;
import com.zainabfatima.i221064.util.AvatarView;
import com.zainabfatima.i221064.util.Db;
import com.zainabfatima.i221064.util.ImageUtil;
import com.zainabfatima.i221064.util.Reactions;
import com.zainabfatima.i221064.util.ReactionPicker;
import com.zainabfatima.i221064.util.TimeUtil;
import com.zainabfatima.i221064.util.UserCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Binds one post (item_post.xml) and keeps its reaction summary and comment count live.
 * Used both by the Home feed rows and by the single-post screen.
 */
public class PostView {

    public interface Host {
        void onHide(Post post);
    }

    private final View root;
    private final Context ctx;
    private final Host host;

    private final AvatarView avatar;
    private final TextView author, time, text, reactionCount, commentCount, likeWord, tvMore;
    private final ImageView likeImg, n1, n2, n3;
    private final LinearLayout reactionIcons;
    private final View reactionsBox, postImages, rightColumn, frame3, overlay;

    private Post post;
    private UserCache.Watch authorWatch;
    private DatabaseReference reactionRef, counterRef;
    private ValueEventListener reactionListener, counterListener;

    private final Map<String, Integer> reactionsByUser = new HashMap<>();
    private int myReaction = -1;

    public PostView(View root, Host host) {
        this.root = root;
        this.ctx = root.getContext();
        this.host = host;
        avatar = root.findViewById(R.id.postAvatar);
        author = root.findViewById(R.id.postAuthor);
        time = root.findViewById(R.id.postTime);
        text = root.findViewById(R.id.postText);
        reactionCount = root.findViewById(R.id.tvReactionCount);
        commentCount = root.findViewById(R.id.tvCommentCount);
        likeWord = root.findViewById(R.id.likeword);
        likeImg = root.findViewById(R.id.likeimg);
        reactionIcons = root.findViewById(R.id.reactionIcons);
        reactionsBox = root.findViewById(R.id.reactions);
        postImages = root.findViewById(R.id.postImages);
        rightColumn = root.findViewById(R.id.rightColumn);
        frame3 = root.findViewById(R.id.frame3);
        overlay = root.findViewById(R.id.n4);
        tvMore = root.findViewById(R.id.tvMore);
        n1 = root.findViewById(R.id.n1);
        n2 = root.findViewById(R.id.n2);
        n3 = root.findViewById(R.id.n3);
    }

    public void bind(final Post p) {
        unbind();
        this.post = p;
        reactionsByUser.clear();
        myReaction = -1;

        // ---- author (live, so a new profile picture shows up straight away) ----
        authorWatch = UserCache.watch(p.authorId, u -> {
            if (post == null || u == null) return;
            avatar.bind(u);
            author.setText(u.displayName());
        });
        User cached = UserCache.peek(p.authorId);
        if (cached != null) {
            avatar.bind(cached);
            author.setText(cached.displayName());
        } else {
            avatar.bind("?", null, p.authorId);
            author.setText("");
        }

        time.setText(TimeUtil.ago(p.timestamp) + " •");
        if (p.text == null || p.text.isEmpty()) {
            text.setVisibility(View.GONE);
        } else {
            text.setVisibility(View.VISIBLE);
            text.setText(p.text);
        }
        bindImages(p.images);
        refreshReactionUi();
        commentCount.setText("");

        // ---- real-time reactions ----
        reactionRef = Db.postReactions(p.id);
        reactionListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                reactionsByUser.clear();
                String me = Db.uid();
                myReaction = -1;
                for (DataSnapshot c : s.getChildren()) {
                    Long t = c.getValue(Long.class);
                    if (t == null) continue;
                    reactionsByUser.put(c.getKey(), t.intValue());
                    if (c.getKey().equals(me)) myReaction = t.intValue();
                }
                refreshReactionUi();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        };
        reactionRef.addValueEventListener(reactionListener);

        // ---- real-time comment count ----
        counterRef = Db.root().child("postCounters").child(p.id).child("comments");
        counterListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                Long n = s.getValue(Long.class);
                long count = n == null ? 0 : Math.max(0, n);
                commentCount.setText(count == 0 ? "" : count + (count == 1 ? " comment" : " comments"));
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        };
        counterRef.addValueEventListener(counterListener);

        wireClicks(p);
    }

    public void unbind() {
        if (authorWatch != null) authorWatch.stop();
        if (reactionRef != null && reactionListener != null) reactionRef.removeEventListener(reactionListener);
        if (counterRef != null && counterListener != null) counterRef.removeEventListener(counterListener);
        authorWatch = null;
        reactionRef = null;
        counterRef = null;
        post = null;
    }

    // ------------------------------------------------------------------ images
    private void bindImages(List<String> images) {
        int n = images == null ? 0 : images.size();
        if (n == 0) {
            postImages.setVisibility(View.GONE);
            return;
        }
        postImages.setVisibility(View.VISIBLE);
        ImageUtil.show(n1, images.get(0));
        if (n == 1) {
            rightColumn.setVisibility(View.GONE);
            return;
        }
        rightColumn.setVisibility(View.VISIBLE);
        ImageUtil.show(n2, images.get(1));
        if (n == 2) {
            frame3.setVisibility(View.GONE);
            return;
        }
        frame3.setVisibility(View.VISIBLE);
        ImageUtil.show(n3, images.get(2));
        if (n > 3) {                        // the "+N" overlay of screen 04
            overlay.setVisibility(View.VISIBLE);
            tvMore.setText("+" + (n - 3));
        } else {
            overlay.setVisibility(View.GONE);
        }
    }

    // --------------------------------------------------------------- reactions
    private void refreshReactionUi() {
        int total = reactionsByUser.size();
        int[] counts = new int[Reactions.COUNT];
        for (int t : reactionsByUser.values()) if (t >= 0 && t < Reactions.COUNT) counts[t]++;

        reactionIcons.removeAllViews();
        if (total == 0) {
            reactionsBox.setVisibility(View.INVISIBLE);
        } else {
            reactionsBox.setVisibility(View.VISIBLE);
            // up to three most used reactions
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < Reactions.COUNT; i++) if (counts[i] > 0) order.add(i);
            order.sort((a, b) -> counts[b] - counts[a]);
            float d = ctx.getResources().getDisplayMetrics().density;
            for (int i = 0; i < Math.min(3, order.size()); i++) {
                ImageView iv = new ImageView(ctx);
                iv.setImageResource(Reactions.icon(order.get(i)));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Math.round(20 * d), Math.round(20 * d));
                if (i > 0) lp.setMarginStart(Math.round(-5 * d));
                reactionIcons.addView(iv, lp);
            }
            if (myReaction >= 0) {
                reactionCount.setText(total == 1 ? "You" : "You and " + (total - 1) + (total == 2 ? " other" : " others"));
            } else {
                reactionCount.setText(String.valueOf(total));
            }
        }

        if (myReaction >= 0) {
            likeWord.setText(Reactions.name(myReaction));
            likeWord.setTextColor(Reactions.color(myReaction));
            likeImg.setImageResource(Reactions.icon(myReaction));
            likeImg.clearColorFilter();
        } else {
            likeWord.setText(R.string.like);
            likeWord.setTextColor(0xFF8B898E);
            likeImg.setImageResource(R.drawable.like);
        }
    }

    // ------------------------------------------------------------------ clicks
    private void wireClicks(final Post p) {
        final View likeButton = root.findViewById(R.id.likeButton);

        // tap: like / remove; long-press: reaction picker (screen 05)
        likeButton.setOnClickListener(v -> {
            if (myReaction >= 0) PostRepo.removeReaction(p.id);
            else PostRepo.react(p, Reactions.LIKE, false);
        });
        likeButton.setOnLongClickListener(v -> {
            ReactionPicker.show(likeButton, type -> {
                if (type == myReaction) PostRepo.removeReaction(p.id);     // pick the same one again to remove it
                else PostRepo.react(p, type, myReaction >= 0);
            });
            return true;
        });

        root.findViewById(R.id.commentButton).setOnClickListener(v -> openComments(p));
        commentCount.setOnClickListener(v -> openComments(p));
        reactionsBox.setOnClickListener(v -> showWhoReacted());
        root.findViewById(R.id.shareButton).setOnClickListener(v -> ShareHelper.sharePostToChat(ctx, p));

        View.OnClickListener openPost = v -> {
            Intent i = new Intent(ctx, LikeActivity.class);
            i.putExtra(LikeActivity.EXTRA_POST_ID, p.id);
            ctx.startActivity(i);
        };
        n1.setOnClickListener(openPost);
        n2.setOnClickListener(openPost);
        n3.setOnClickListener(openPost);
        overlay.setOnClickListener(openPost);
        text.setOnClickListener(openPost);

        root.findViewById(R.id.cancel).setOnClickListener(v -> {
            if (host != null) host.onHide(p);
        });
        final View more = root.findViewById(R.id.horizontal_more);
        more.setOnClickListener(v -> {
            PopupMenu pm = new PopupMenu(ctx, more);
            pm.getMenu().add(0, 1, 0, "Share to chat");
            boolean mine = p.authorId != null && p.authorId.equals(Db.uid());
            if (mine) pm.getMenu().add(0, 2, 1, "Delete post");
            pm.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) ShareHelper.sharePostToChat(ctx, p);
                else if (item.getItemId() == 2) confirmDelete(p);
                return true;
            });
            pm.show();
        });
    }

    private void openComments(Post p) {
        Intent i = new Intent(ctx, CommentsActivity.class);
        i.putExtra(CommentsActivity.EXTRA_POST_ID, p.id);
        ctx.startActivity(i);
    }

    private void confirmDelete(final Post p) {
        new AlertDialog.Builder(ctx)
                .setTitle("Delete post?")
                .setMessage("This will also remove its reactions and comments.")
                .setPositiveButton("Delete", (d, w) -> PostRepo.delete(p.id))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Tapping the reaction summary lists who reacted and how. */
    private void showWhoReacted() {
        if (reactionsByUser.isEmpty() || !(ctx instanceof Activity)) return;
        final List<String> uids = new ArrayList<>(reactionsByUser.keySet());
        final String[] rows = new String[uids.size()];
        final int[] pending = {uids.size()};
        final Map<String, Integer> snapshot = new HashMap<>(reactionsByUser);
        for (int i = 0; i < uids.size(); i++) {
            final int index = i;
            final String uid = uids.get(i);
            UserCache.get(uid, u -> {
                String name = u == null ? "Kinnect user" : u.displayName();
                rows[index] = name + "  •  " + Reactions.name(snapshot.get(uid));
                if (--pending[0] == 0) {
                    new AlertDialog.Builder(ctx).setTitle("Reactions").setItems(rows, null)
                            .setPositiveButton("Close", null).show();
                }
            });
        }
    }
}
