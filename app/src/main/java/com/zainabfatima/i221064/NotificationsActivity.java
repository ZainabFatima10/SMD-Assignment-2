package com.zainabfatima.i221064;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.model.AppNotification;
import com.zainabfatima.i221064.util.AvatarView;
import com.zainabfatima.i221064.util.Db;
import com.zainabfatima.i221064.util.FriendsHelper;
import com.zainabfatima.i221064.util.TimeUtil;
import com.zainabfatima.i221064.util.UserCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Screen 18 - Notifications. A live list: friend requests (with Confirm / Delete), accepted requests,
 * reactions, comments and screenshot alerts.
 */
public class NotificationsActivity extends AppCompatActivity {

    private final List<AppNotification> items = new ArrayList<>();
    private Adapter adapter;
    private View empty;
    private Query query;
    private ValueEventListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        NavHelper.setup(this, NavHelper.NOTIF, "Notifications");

        final String me = Db.uid();
        if (me == null) {
            finish();
            return;
        }
        empty = findViewById(R.id.emptyNotifs);
        RecyclerView list = findViewById(R.id.notifList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Adapter();
        list.setAdapter(adapter);

        query = Db.notifications(me).orderByChild("timestamp").limitToLast(60);
        listener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                items.clear();
                for (DataSnapshot c : s.getChildren()) {
                    AppNotification n = c.getValue(AppNotification.class);
                    if (n == null) continue;
                    if (n.id == null) n.id = c.getKey();
                    items.add(n);
                }
                Collections.sort(items, (a, b) -> Long.compare(b.timestamp, a.timestamp));
                adapter.notifyDataSetChanged();
                empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        };
        query.addValueEventListener(listener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // everything on screen counts as seen
        String me = Db.uid();
        if (me == null) return;
        Map<String, Object> up = new HashMap<>();
        for (AppNotification n : items) if (!n.read) up.put(n.id + "/read", true);
        if (!up.isEmpty()) Db.notifications(me).updateChildren(up);
    }

    @Override
    protected void onDestroy() {
        if (query != null && listener != null) query.removeEventListener(listener);
        super.onDestroy();
    }

    private void open(AppNotification n) {
        if (n.type == null) return;
        switch (n.type) {
            case AppNotification.FRIEND_REQUEST:
            case AppNotification.FRIEND_ACCEPTED:
                startActivity(new Intent(this, ProfileActivity.class).putExtra(ProfileActivity.EXTRA_UID, n.fromUid));
                break;
            case AppNotification.REACTION:
            case AppNotification.COMMENT:
                if (n.refId != null) {
                    startActivity(new Intent(this, LikeActivity.class).putExtra(LikeActivity.EXTRA_POST_ID, n.refId));
                }
                break;
            case AppNotification.SCREENSHOT:
                startActivity(new Intent(this, ChatActivity.class).putExtra(ChatActivity.EXTRA_PEER_ID, n.fromUid));
                break;
            default:
                break;
        }
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull final Holder h, int position) {
            final AppNotification n = items.get(position);
            h.itemView.setBackgroundColor(n.read ? 0x00000000 : 0xFFE0EEEE);
            h.boundId = n.id;
            h.avatar.bind("?", null, n.fromUid == null ? "?" : n.fromUid);
            h.text.setText(n.text == null ? "" : n.text);
            UserCache.get(n.fromUid, u -> {
                if (u == null || !n.id.equals(h.boundId)) return;
                h.avatar.bind(u);
                h.text.setText(Html.fromHtml("<b>" + Html.escapeHtml(u.displayName()) + "</b> "
                        + Html.escapeHtml(n.text == null ? "" : n.text), Html.FROM_HTML_MODE_LEGACY));
            });
            h.time.setText(TimeUtil.ago(n.timestamp));

            int icon;
            int color;
            if (AppNotification.REACTION.equals(n.type)) { icon = R.drawable.ic_heart; color = 0xFFD13A55; }
            else if (AppNotification.COMMENT.equals(n.type)) { icon = R.drawable.ic_comment; color = 0xFF0B5F61; }
            else if (AppNotification.SCREENSHOT.equals(n.type)) { icon = R.drawable.ic_camera; color = 0xFFBC4A28; }
            else { icon = R.drawable.ol_person_add; color = 0xFF0B5F61; }
            h.badge.setImageResource(icon);
            h.badge.setColorFilter(0xFFFFFFFF);
            h.badge.setBackgroundTintList(ColorStateList.valueOf(color));

            boolean request = AppNotification.FRIEND_REQUEST.equals(n.type);
            h.actions.setVisibility(request ? View.VISIBLE : View.GONE);
            h.confirm.setOnClickListener(v -> FriendsHelper.confirm(n.fromUid));
            h.delete.setOnClickListener(v -> FriendsHelper.deleteRequest(n.fromUid));

            h.itemView.setOnClickListener(v -> open(n));
            h.more.setOnClickListener(v -> {
                PopupMenu pm = new PopupMenu(NotificationsActivity.this, h.more);
                pm.getMenu().add(0, 1, 0, "Remove this notification");
                pm.setOnMenuItemClickListener(item -> {
                    Db.notifications(Db.uid()).child(n.id).removeValue();
                    return true;
                });
                pm.show();
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class Holder extends RecyclerView.ViewHolder {
            final AvatarView avatar;
            final ImageView badge, more;
            final TextView text, time, confirm, delete;
            final View actions;
            String boundId;

            Holder(View v) {
                super(v);
                avatar = v.findViewById(R.id.avatar);
                badge = v.findViewById(R.id.badge);
                more = v.findViewById(R.id.more);
                text = v.findViewById(R.id.text);
                time = v.findViewById(R.id.time);
                actions = v.findViewById(R.id.actions);
                confirm = v.findViewById(R.id.btnConfirm);
                delete = v.findViewById(R.id.btnDelete);
            }
        }
    }
}
