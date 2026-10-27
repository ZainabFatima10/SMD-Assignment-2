package com.zainabfatima.i221064.feed;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zainabfatima.i221064.R;
import com.zainabfatima.i221064.model.Comment;
import com.zainabfatima.i221064.util.AvatarView;
import com.zainabfatima.i221064.util.Db;
import com.zainabfatima.i221064.util.ImageUtil;
import com.zainabfatima.i221064.util.TimeUtil;
import com.zainabfatima.i221064.util.UserCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Comments with their replies underneath; long reply threads collapse behind "View N more replies". */
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.Holder> {

    public interface Actions {
        void onReply(Comment c);
        void onLikeToggle(Comment c, boolean currentlyLiked);
    }

    private static final int TYPE_ROW = 0;

    /** One visible row: a comment, or (when comment==null) nothing; moreFor!=null means a "view more" row. */
    private static class Row {
        Comment comment;
        boolean reply;
        String moreFor;       // top-level id whose replies can be expanded
        int hidden;
    }

    private final Actions actions;
    private final List<Row> rows = new ArrayList<>();
    private final List<Comment> all = new ArrayList<>();
    private final Set<String> expanded = new HashSet<>();
    private Map<String, Set<String>> likes = new HashMap<>();   // commentId -> uids

    public CommentAdapter(Actions a) {
        this.actions = a;
    }

    public void setComments(List<Comment> comments) {
        all.clear();
        all.addAll(comments);
        rebuild();
    }

    public void setLikes(Map<String, Set<String>> likes) {
        this.likes = likes;
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return all.isEmpty();
    }

    private void rebuild() {
        rows.clear();
        Map<String, List<Comment>> replies = new HashMap<>();
        List<Comment> top = new ArrayList<>();
        for (Comment c : all) {
            if (c.parentId == null || c.parentId.isEmpty()) top.add(c);
            else {
                List<Comment> l = replies.get(c.parentId);
                if (l == null) replies.put(c.parentId, l = new ArrayList<>());
                l.add(c);
            }
        }
        Collections.sort(top, (a, b) -> Long.compare(a.timestamp, b.timestamp));
        for (Comment t : top) {
            Row r = new Row();
            r.comment = t;
            rows.add(r);
            List<Comment> rep = replies.get(t.id);
            if (rep == null) continue;
            Collections.sort(rep, (a, b) -> Long.compare(a.timestamp, b.timestamp));
            boolean open = expanded.contains(t.id) || rep.size() <= 1;
            int shown = open ? rep.size() : 1;
            for (int i = 0; i < shown; i++) {
                Row rr = new Row();
                rr.comment = rep.get(i);
                rr.reply = true;
                rows.add(rr);
            }
            if (!open) {
                Row more = new Row();
                more.moreFor = t.id;
                more.hidden = rep.size() - 1;
                rows.add(more);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        final Row row = rows.get(position);
        float d = h.itemView.getResources().getDisplayMetrics().density;

        if (row.moreFor != null) {
            h.commentRow.setVisibility(View.GONE);
            h.more.setVisibility(View.VISIBLE);
            h.more.setPadding(Math.round(62 * d), Math.round(10 * d), Math.round(10 * d), Math.round(10 * d));
            h.more.setText("View " + row.hidden + " more " + (row.hidden == 1 ? "reply" : "replies"));
            h.more.setOnClickListener(v -> {
                expanded.add(row.moreFor);
                rebuild();
            });
            return;
        }
        h.more.setVisibility(View.GONE);
        h.commentRow.setVisibility(View.VISIBLE);
        h.commentRow.setPadding(Math.round((row.reply ? 56 : 10) * d), h.commentRow.getPaddingTop(),
                h.commentRow.getPaddingRight(), h.commentRow.getPaddingBottom());

        final Comment c = row.comment;
        h.avatar.getLayoutParams().width = Math.round((row.reply ? 32 : 40) * d);
        h.avatar.getLayoutParams().height = Math.round((row.reply ? 32 : 40) * d);

        h.author.setText("");
        com.zainabfatima.i221064.model.User u = UserCache.peek(c.authorId);
        if (u != null) {
            h.avatar.bind(u);
            h.author.setText(u.displayName());
        } else {
            h.avatar.bind("?", null, c.authorId);
        }
        h.boundId = c.id;
        UserCache.get(c.authorId, user -> {
            if (user != null && c.id.equals(h.boundId)) {
                h.avatar.bind(user);
                h.author.setText(user.displayName());
            }
        });

        boolean hasText = c.text != null && !c.text.isEmpty();
        h.text.setVisibility(hasText ? View.VISIBLE : View.GONE);
        h.text.setText(hasText ? c.text : "");
        if (c.image != null && !c.image.isEmpty()) {
            h.image.setVisibility(View.VISIBLE);
            ImageUtil.show(h.image, c.image);
        } else {
            h.image.setVisibility(View.GONE);
        }
        h.time.setText(TimeUtil.ago(c.timestamp));

        Set<String> who = likes.get(c.id);
        int n = who == null ? 0 : who.size();
        final boolean liked = who != null && who.contains(Db.uid());
        h.like.setTextColor(liked ? 0xFF0B5F63 : 0xFF8B898E);
        h.like.setText(liked ? "Liked" : "Like");
        h.likeCount.setText(n == 0 ? "" : "♥ " + n);
        h.like.setOnClickListener(v -> actions.onLikeToggle(c, liked));
        h.reply.setOnClickListener(v -> actions.onReply(c));
    }

    @Override public int getItemCount() { return rows.size(); }

    @Override public int getItemViewType(int position) { return TYPE_ROW; }

    static class Holder extends RecyclerView.ViewHolder {
        final View commentRow;
        final AvatarView avatar;
        final TextView author, text, time, like, reply, likeCount, more;
        final ImageView image;
        String boundId;

        Holder(View v) {
            super(v);
            commentRow = v.findViewById(R.id.commentRow);
            avatar = v.findViewById(R.id.commentAvatar);
            author = v.findViewById(R.id.commentAuthor);
            text = v.findViewById(R.id.commentText);
            image = v.findViewById(R.id.commentImage);
            time = v.findViewById(R.id.commentTime);
            like = v.findViewById(R.id.commentLike);
            reply = v.findViewById(R.id.commentReply);
            likeCount = v.findViewById(R.id.commentLikeCount);
            more = v.findViewById(R.id.viewMoreReplies);
        }
    }
}
