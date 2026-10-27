package com.zainabfatima.i221064;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.model.ChatSummary;
import com.zainabfatima.i221064.model.User;
import com.zainabfatima.i221064.util.AvatarView;
import com.zainabfatima.i221064.util.Db;
import com.zainabfatima.i221064.util.TimeUtil;
import com.zainabfatima.i221064.util.UserCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Screen 20 - Chats. Real conversations (last message, time, unread badge), a green dot for people
 * who are online right now, and an "active now" strip built from online friends.
 */
public class ChatsActivity extends AppCompatActivity {

    private final List<ChatSummary> summaries = new ArrayList<>();
    private final List<ChatSummary> visible = new ArrayList<>();
    private final Map<String, User> people = new HashMap<>();             // live user records
    private final Map<String, UserCache.Watch> watches = new HashMap<>();
    private final List<String> friendIds = new ArrayList<>();

    private ChatAdapter adapter;
    private LinearLayout activeRow;
    private View activeScroll;
    private TextView empty;
    private String query = "";

    private DatabaseReference chatsRef, friendsRef;
    private ValueEventListener chatsListener, friendsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chats);

        final String me = Db.uid();
        if (me == null) {
            finish();
            return;
        }
        activeRow = findViewById(R.id.activeRow);
        activeScroll = findViewById(R.id.activeScroll);
        empty = findViewById(R.id.emptyChats);

        RecyclerView list = findViewById(R.id.chatList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter();
        list.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCompose).setOnClickListener(v -> pickFriendToMessage());
        findViewById(R.id.navPeople).setOnClickListener(v -> startActivity(new Intent(this, FriendsActivity.class)));
        findViewById(R.id.navStories).setOnClickListener(v -> NavHelper.goHome(this));

        ((EditText) findViewById(R.id.etSearch)).addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                query = s.toString().trim().toLowerCase();
                refilter();
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        chatsRef = Db.userChats(me);
        chatsListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                summaries.clear();
                for (DataSnapshot c : s.getChildren()) {
                    ChatSummary cs = c.getValue(ChatSummary.class);
                    if (cs == null) continue;
                    if (cs.peerId == null) cs.peerId = c.getKey();
                    summaries.add(cs);
                    follow(cs.peerId);
                }
                Collections.sort(summaries, (a, b) -> Long.compare(b.timestamp, a.timestamp));
                refilter();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        };
        chatsRef.addValueEventListener(chatsListener);

        friendsRef = Db.friends(me);
        friendsListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                friendIds.clear();
                for (DataSnapshot c : s.getChildren()) {
                    friendIds.add(c.getKey());
                    follow(c.getKey());
                }
                rebuildActive();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        };
        friendsRef.addValueEventListener(friendsListener);
    }

    /** Subscribes (once) to a person's profile so name, picture and online dot stay current. */
    private void follow(final String uid) {
        if (watches.containsKey(uid)) return;
        watches.put(uid, UserCache.watch(uid, u -> {
            if (u == null) return;
            people.put(uid, u);
            adapter.notifyDataSetChanged();
            rebuildActive();
            refilter();
        }));
    }

    private void refilter() {
        visible.clear();
        for (ChatSummary cs : summaries) {
            User u = people.get(cs.peerId);
            String name = u == null ? "" : u.displayName().toLowerCase();
            if (query.isEmpty() || name.contains(query)) visible.add(cs);
        }
        adapter.notifyDataSetChanged();
        empty.setVisibility(visible.isEmpty() && query.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void rebuildActive() {
        activeRow.removeAllViews();
        int shown = 0;
        LayoutInflater inf = LayoutInflater.from(this);
        for (final String id : friendIds) {
            User u = people.get(id);
            if (u == null || !u.online) continue;
            View v = inf.inflate(R.layout.item_active, activeRow, false);
            ((AvatarView) v.findViewById(R.id.avatar)).bind(u);
            ((TextView) v.findViewById(R.id.label)).setText(firstName(u.displayName()));
            v.setOnClickListener(x -> openChat(id));
            activeRow.addView(v);
            shown++;
        }
        activeScroll.setVisibility(shown == 0 ? View.GONE : View.VISIBLE);
    }

    private static String firstName(String n) {
        int i = n.indexOf(' ');
        return i > 0 ? n.substring(0, i) : n;
    }

    private void pickFriendToMessage() {
        if (friendIds.isEmpty()) {
            NavHelper.toast(this, "Add some friends first to start a conversation");
            return;
        }
        final String[] names = new String[friendIds.size()];
        for (int i = 0; i < names.length; i++) {
            User u = people.get(friendIds.get(i));
            names[i] = u == null ? "…" : u.displayName();
        }
        new AlertDialog.Builder(this)
                .setTitle("New message")
                .setItems(names, (d, which) -> openChat(friendIds.get(which)))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openChat(String peerId) {
        startActivity(new Intent(this, ChatActivity.class).putExtra(ChatActivity.EXTRA_PEER_ID, peerId));
    }

    @Override
    protected void onDestroy() {
        if (chatsRef != null && chatsListener != null) chatsRef.removeEventListener(chatsListener);
        if (friendsRef != null && friendsListener != null) friendsRef.removeEventListener(friendsListener);
        for (UserCache.Watch w : watches.values()) w.stop();
        watches.clear();
        super.onDestroy();
    }

    // -------------------------------------------------------------------- adapter
    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.Holder> {
        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            final ChatSummary cs = visible.get(position);
            User u = people.get(cs.peerId);
            h.avatar.bind(u == null ? null : u);
            h.name.setText(u == null ? "" : u.displayName());
            h.dot.setVisibility(u != null && u.online ? View.VISIBLE : View.GONE);

            boolean unread = cs.unread > 0;
            String me = Db.uid();
            String prefix = me != null && me.equals(cs.lastSenderId) ? "You: " : "";
            String last = cs.lastMessage == null ? "" : cs.lastMessage;
            h.preview.setText(prefix + last + (cs.timestamp > 0 ? " · " + TimeUtil.chatTime(cs.timestamp) : ""));
            h.preview.setTypeface(null, unread ? Typeface.BOLD : Typeface.NORMAL);
            h.preview.setTextColor(unread ? 0xFF1E1E1E : 0xFF6B655C);
            h.trail.setVisibility(unread ? View.VISIBLE : View.INVISIBLE);
            h.itemView.setOnClickListener(v -> openChat(cs.peerId));
        }

        @Override public int getItemCount() { return visible.size(); }

        class Holder extends RecyclerView.ViewHolder {
            final AvatarView avatar;
            final TextView name, preview;
            final View dot, trail;
            Holder(View v) {
                super(v);
                avatar = v.findViewById(R.id.avatar);
                name = v.findViewById(R.id.name);
                preview = v.findViewById(R.id.preview);
                dot = v.findViewById(R.id.dot);
                trail = v.findViewById(R.id.trail);
            }
        }
    }
}
