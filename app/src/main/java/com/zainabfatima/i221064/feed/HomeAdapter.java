package com.zainabfatima.i221064.feed;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zainabfatima.i221064.CameraActivity;
import com.zainabfatima.i221064.CreatePostActivity;
import com.zainabfatima.i221064.PictureSendActivity;
import com.zainabfatima.i221064.R;
import com.zainabfatima.i221064.StoryViewerActivity;
import com.zainabfatima.i221064.YourStoryActivity;
import com.zainabfatima.i221064.MyProfileActivity;
import com.zainabfatima.i221064.model.Post;
import com.zainabfatima.i221064.model.Story;
import com.zainabfatima.i221064.model.User;
import com.zainabfatima.i221064.util.AvatarView;
import com.zainabfatima.i221064.util.ImageUtil;
import com.zainabfatima.i221064.util.UserCache;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Home: [composer + stories row] followed by the posts. */
public class HomeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements PostView.Host {

    private static final int TYPE_HEADER = 0, TYPE_POST = 1;

    private final List<Post> posts = new ArrayList<>();
    private final Set<String> hidden = new HashSet<>();
    private final StoriesAdapter storiesAdapter = new StoriesAdapter();
    private User me;
    private HeaderHolder header;

    public void setMe(User u) {
        me = u;
        if (header != null) header.bindMe(u);
        storiesAdapter.setMe(u);
    }

    public void setStories(StoryGroup mine, List<StoryGroup> friends) {
        storiesAdapter.setGroups(mine, friends);
    }

    public void setPosts(List<Post> newPosts) {
        final List<Post> next = new ArrayList<>();
        for (Post p : newPosts) if (!hidden.contains(p.id)) next.add(p);
        final List<Post> old = new ArrayList<>(posts);
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return old.size(); }
            @Override public int getNewListSize() { return next.size(); }
            @Override public boolean areItemsTheSame(int o, int n) { return old.get(o).id.equals(next.get(n).id); }
            @Override public boolean areContentsTheSame(int o, int n) {
                Post a = old.get(o), b = next.get(n);
                return safe(a.text).equals(safe(b.text)) && a.images.size() == b.images.size()
                        && a.timestamp == b.timestamp;
            }
        });
        posts.clear();
        posts.addAll(next);
        // positions are shifted by one because of the header row
        diff.dispatchUpdatesTo(new androidx.recyclerview.widget.ListUpdateCallback() {
            @Override public void onInserted(int position, int count) { notifyItemRangeInserted(position + 1, count); }
            @Override public void onRemoved(int position, int count) { notifyItemRangeRemoved(position + 1, count); }
            @Override public void onMoved(int from, int to) { notifyItemMoved(from + 1, to + 1); }
            @Override public void onChanged(int position, int count, Object payload) { notifyItemRangeChanged(position + 1, count, payload); }
        });
        if (header != null) header.empty.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    @Override
    public void onHide(Post post) {
        hidden.add(post.id);
        int index = posts.indexOf(post);
        if (index >= 0) {
            posts.remove(index);
            notifyItemRemoved(index + 1);
        }
    }

    @Override public int getItemViewType(int position) { return position == 0 ? TYPE_HEADER : TYPE_POST; }
    @Override public int getItemCount() { return posts.size() + 1; }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            header = new HeaderHolder(inf.inflate(R.layout.item_home_header, parent, false));
            header.bindMe(me);
            header.empty.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
            return header;
        }
        return new PostHolder(inf.inflate(R.layout.item_post, parent, false), this);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof PostHolder) ((PostHolder) holder).view.bind(posts.get(position - 1));
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof PostHolder) ((PostHolder) holder).view.unbind();
    }

    // ------------------------------------------------------------------ holders
    static class PostHolder extends RecyclerView.ViewHolder {
        final PostView view;
        PostHolder(View v, PostView.Host host) {
            super(v);
            view = new PostView(v, host);
        }
    }

    class HeaderHolder extends RecyclerView.ViewHolder {
        final AvatarView avatar;
        final TextView prompt, empty;

        HeaderHolder(View v) {
            super(v);
            avatar = v.findViewById(R.id.seagreenpfp);
            prompt = v.findViewById(R.id.search_bar);
            empty = v.findViewById(R.id.tvEmptyFeed);
            final android.content.Context c = v.getContext();
            prompt.setOnClickListener(x -> c.startActivity(new Intent(c, CreatePostActivity.class)));
            v.findViewById(R.id.btnImagePost).setOnClickListener(x -> {
                Intent i = new Intent(c, CreatePostActivity.class);
                i.putExtra(CreatePostActivity.EXTRA_OPEN_PICKER, true);
                c.startActivity(i);
            });
            avatar.setOnClickListener(x -> c.startActivity(new Intent(c, MyProfileActivity.class)));

            RecyclerView row = v.findViewById(R.id.storiesRow);
            row.setLayoutManager(new LinearLayoutManager(c, LinearLayoutManager.HORIZONTAL, false));
            row.setAdapter(storiesAdapter);
            row.setNestedScrollingEnabled(false);
        }

        void bindMe(User u) {
            if (u == null) return;
            avatar.bind(u);
            String first = u.displayName().split(" ")[0];
            prompt.setText("What's on your mind, " + first + "?");
        }
    }

    // -------------------------------------------------------------- stories row
    /** "Create story" first, then your own story (if any), then friends with live stories. */
    static class StoriesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int T_CREATE = 0, T_STORY = 1;
        private final List<StoryGroup> groups = new ArrayList<>();
        private User me;
        private StoryGroup mine;

        void setMe(User u) {
            me = u;
            notifyDataSetChanged();
        }

        void setGroups(StoryGroup mineGroup, List<StoryGroup> friends) {
            mine = mineGroup;
            groups.clear();
            if (mineGroup != null) groups.add(mineGroup);
            groups.addAll(friends);
            notifyDataSetChanged();
        }

        @Override public int getItemViewType(int position) { return position == 0 ? T_CREATE : T_STORY; }
        @Override public int getItemCount() { return groups.size() + 1; }

        @NonNull @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            if (viewType == T_CREATE) return new CreateHolder(inf.inflate(R.layout.item_story_create, parent, false));
            return new StoryHolder(inf.inflate(R.layout.item_story, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
            if (h instanceof CreateHolder) {
                ((CreateHolder) h).avatar.bind(me);
                return;
            }
            final StoryGroup g = groups.get(position - 1);
            final StoryHolder sh = (StoryHolder) h;
            final android.content.Context c = sh.itemView.getContext();
            final boolean isMine = g == mine;
            Story last = g.last();
            ImageUtil.show(sh.thumb, last == null ? null : last.thumb);
            sh.name.setText(isMine ? "Your story" : "");
            if (isMine) {
                sh.avatar.bind(me);
            } else {
                User cached = UserCache.peek(g.ownerId);
                if (cached != null) {
                    sh.avatar.bind(cached);
                    sh.name.setText(cached.displayName());
                } else {
                    sh.avatar.bind("?", null, g.ownerId);
                }
                UserCache.get(g.ownerId, u -> {
                    if (u == null || sh.getBindingAdapterPosition() < 0) return;
                    sh.avatar.bind(u);
                    sh.name.setText(u.displayName());
                });
            }
            sh.itemView.setOnClickListener(v -> {
                Intent i = new Intent(c, isMine ? YourStoryActivity.class : StoryViewerActivity.class);
                i.putExtra(StoryViewerActivity.EXTRA_OWNER_ID, g.ownerId);
                c.startActivity(i);
            });
        }

        static class CreateHolder extends RecyclerView.ViewHolder {
            final AvatarView avatar;
            CreateHolder(View v) {
                super(v);
                avatar = v.findViewById(R.id.createAvatar);
                v.setOnClickListener(x -> {
                    android.content.Context c = x.getContext();
                    c.startActivity(new Intent(c, CameraActivity.class));
                });
            }
        }

        static class StoryHolder extends RecyclerView.ViewHolder {
            final android.widget.ImageView thumb;
            final AvatarView avatar;
            final TextView name;
            StoryHolder(View v) {
                super(v);
                thumb = v.findViewById(R.id.storyThumb);
                avatar = v.findViewById(R.id.storyAvatar);
                name = v.findViewById(R.id.storyName);
            }
        }
    }
}
