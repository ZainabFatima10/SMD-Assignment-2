package com.zainabfatima.i221064;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.chat.ChatRepo;
import com.zainabfatima.i221064.model.Message;
import com.zainabfatima.i221064.model.User;
import com.zainabfatima.i221064.util.Async;
import com.zainabfatima.i221064.util.AvatarView;
import com.zainabfatima.i221064.util.Db;
import com.zainabfatima.i221064.util.ImageUtil;
import com.zainabfatima.i221064.util.ScreenshotWatcher;
import com.zainabfatima.i221064.util.TimeUtil;
import com.zainabfatima.i221064.util.UserCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Screen 21 - Chat. Real-time text / image / shared-post messages, replies, double-tap hearts,
 * edit & delete of your own messages within 5 minutes, "Active now" presence, voice & video call
 * buttons and screenshot detection.
 */
public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_PEER_ID = "peer_id";

    private String me, peerId;
    private User peer;
    private final List<Message> messages = new ArrayList<>();
    private MessageAdapter adapter;
    private RecyclerView list;
    private EditText etMsg;
    private ImageView btnSend;
    private View banner;
    private TextView bannerText, hdrStatus;
    private View onlineDot;
    private AvatarView hdrAvatar;

    private Message replyingTo, editing;
    private boolean resumed;
    private int lastCount = 0;

    private Query query;
    private ValueEventListener listener;
    private UserCache.Watch peerWatch;
    private ScreenshotWatcher screenshots;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() != RESULT_OK || r.getData() == null) return;
                ArrayList<String> uris = r.getData().getStringArrayListExtra(PictureSendActivity.RESULT_URIS);
                if (uris != null) for (String u : uris) sendImage(Uri.parse(u));
            });

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() != RESULT_OK || r.getData() == null) return;
                String u = r.getData().getStringExtra(CameraActivity.RESULT_URI);
                if (u != null) sendImage(Uri.parse(u));
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        me = Db.uid();
        peerId = getIntent().getStringExtra(EXTRA_PEER_ID);
        if (me == null || peerId == null) {
            finish();
            return;
        }

        hdrAvatar = findViewById(R.id.hdrAvatar);
        hdrStatus = findViewById(R.id.hdrStatus);
        onlineDot = findViewById(R.id.hdrOnlineDot);
        etMsg = findViewById(R.id.etMsg);
        btnSend = findViewById(R.id.btnSend);
        banner = findViewById(R.id.banner);
        bannerText = findViewById(R.id.bannerText);

        list = findViewById(R.id.messageList);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        list.setLayoutManager(lm);
        adapter = new MessageAdapter();
        list.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCall).setOnClickListener(v -> startCall(false));
        findViewById(R.id.btnVideo).setOnClickListener(v -> startCall(true));
        findViewById(R.id.btnInfo).setOnClickListener(v -> startActivity(
                new Intent(this, ProfileActivity.class).putExtra(ProfileActivity.EXTRA_UID, peerId)));
        findViewById(R.id.btnCamera).setOnClickListener(v -> cameraLauncher.launch(
                new Intent(this, CameraActivity.class).putExtra(CameraActivity.EXTRA_RESULT_MODE, true)));
        findViewById(R.id.btnGallery).setOnClickListener(v -> galleryLauncher.launch(
                new Intent(this, PictureSendActivity.class).putExtra(PictureSendActivity.EXTRA_MULTI, true)
                        .putExtra(PictureSendActivity.EXTRA_MAX, 5)));
        findViewById(R.id.btnEmoji).setOnClickListener(v -> {
            int pos = Math.max(0, etMsg.getSelectionStart());
            etMsg.getText().insert(pos, "😊");
        });
        findViewById(R.id.bannerCancel).setOnClickListener(v -> clearBanner());

        etMsg.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                btnSend.setImageResource(s.toString().trim().isEmpty() && editing == null
                        ? R.drawable.ol_thumb : R.drawable.ic_send);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        btnSend.setOnClickListener(v -> onSend());

        peerWatch = UserCache.watch(peerId, u -> {
            if (u == null) return;
            peer = u;
            hdrAvatar.bind(u);
            ((TextView) findViewById(R.id.hdrName)).setText(u.displayName());
            onlineDot.setVisibility(u.online ? View.VISIBLE : View.GONE);
            hdrStatus.setText(u.online ? "Active now" : TimeUtil.lastSeen(u.lastSeen));
            adapter.notifyDataSetChanged();
        });

        query = Db.messages(Db.chatId(me, peerId)).limitToLast(300);
        listener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                messages.clear();
                for (DataSnapshot c : s.getChildren()) {
                    Message m = c.getValue(Message.class);
                    if (m == null) continue;
                    if (m.id == null) m.id = c.getKey();
                    messages.add(m);
                }
                Collections.sort(messages, (a, b) -> Long.compare(a.timestamp, b.timestamp));
                adapter.notifyDataSetChanged();
                findViewById(R.id.emptyChat).setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
                if (messages.size() > lastCount) list.scrollToPosition(messages.size() - 1);
                lastCount = messages.size();
                if (resumed) ChatRepo.markRead(peerId);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        };
        query.addValueEventListener(listener);

        // someone in this chat taking a screenshot
        screenshots = new ScreenshotWatcher(this, () -> {
            User mine = UserCache.peek(me);
            ChatRepo.reportScreenshot(peerId, mine == null ? "Someone" : mine.displayName());
        });
    }

    private void startCall(boolean video) {
        startActivity(new Intent(this, CallActivity.class)
                .putExtra(CallActivity.EXTRA_PEER_ID, peerId)
                .putExtra(CallActivity.EXTRA_VIDEO, video)
                .putExtra(CallActivity.EXTRA_OUTGOING, true));
    }

    // ---------------------------------------------------------------- sending
    private void onSend() {
        String t = etMsg.getText().toString().trim();
        if (editing != null) {
            if (t.isEmpty()) {
                NavHelper.toast(this, "A message can't be empty. Delete it instead.");
                return;
            }
            boolean isLast = !messages.isEmpty() && messages.get(messages.size() - 1).id.equals(editing.id);
            if (!ChatRepo.edit(peerId, editing, t, isLast)) {
                NavHelper.toast(this, "You can only edit a message within 5 minutes of sending it");
            }
            clearBanner();
            etMsg.setText("");
            return;
        }
        if (t.isEmpty()) t = "👍";
        ChatRepo.sendText(peerId, t, replyingTo, replyingTo == null ? null : nameOf(replyingTo.senderId));
        clearBanner();
        etMsg.setText("");
    }

    private void sendImage(final Uri uri) {
        final Message reply = replyingTo;
        final String replyName = reply == null ? null : nameOf(reply.senderId);
        Async.run(() -> ImageUtil.uriToBase64(this, uri, ImageUtil.CHAT_DIM, 70), (b64, err) -> {
            if (b64 == null) {
                NavHelper.toast(this, "Couldn't send that picture");
                return;
            }
            ChatRepo.sendImage(peerId, b64, reply, replyName);
            clearBanner();
        });
    }

    private String nameOf(String uid) {
        if (me.equals(uid)) return "You";
        return peer == null ? "" : peer.displayName();
    }

    private void clearBanner() {
        replyingTo = null;
        editing = null;
        banner.setVisibility(View.GONE);
    }

    private void startReply(Message m) {
        clearBanner();
        replyingTo = m;
        bannerText.setText("Replying to " + nameOf(m.senderId) + ": " + ChatRepo.previewOf(m));
        banner.setVisibility(View.VISIBLE);
        etMsg.requestFocus();
    }

    private void startEdit(Message m) {
        clearBanner();
        editing = m;
        bannerText.setText("Editing message");
        banner.setVisibility(View.VISIBLE);
        etMsg.setText(m.text);
        etMsg.setSelection(etMsg.getText().length());
        etMsg.requestFocus();
        btnSend.setImageResource(R.drawable.ic_send);
    }

    private void confirmDelete(final Message m) {
        int idx = messages.indexOf(m);
        final boolean isLast = idx == messages.size() - 1;
        final String prev = idx > 0 ? ChatRepo.previewOf(messages.get(idx - 1)) : "";
        new AlertDialog.Builder(this)
                .setTitle("Delete message?")
                .setMessage("It will be removed for both of you.")
                .setPositiveButton("Delete", (d, w) -> {
                    if (!ChatRepo.delete(peerId, m, isLast, prev)) {
                        NavHelper.toast(this, "You can only delete a message within 5 minutes of sending it");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMessageMenu(final Message m) {
        if (Message.SYSTEM.equals(m.type)) return;
        final List<String> labels = new ArrayList<>();
        final List<Runnable> actions = new ArrayList<>();
        labels.add("Reply");
        actions.add(() -> startReply(m));
        final boolean hearted = m.hearts != null && m.hearts.containsKey(me);
        labels.add(hearted ? "Remove heart" : "Heart");
        actions.add(() -> ChatRepo.toggleHeart(peerId, m, hearted));
        if (Message.TEXT.equals(m.type) && m.text != null) {
            labels.add("Copy");
            actions.add(() -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("message", m.text));
            });
        }
        if (ChatRepo.canModify(m)) {
            if (Message.TEXT.equals(m.type)) {
                labels.add("Edit");
                actions.add(() -> startEdit(m));
            }
            labels.add("Delete");
            actions.add(() -> confirmDelete(m));
        }
        new AlertDialog.Builder(this)
                .setItems(labels.toArray(new String[0]), (d, which) -> actions.get(which).run())
                .show();
    }

    // -------------------------------------------------------------- lifecycle
    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        KinnectMessagingService.openChatPeer = peerId;
        ChatRepo.markRead(peerId);
        if (screenshots != null) screenshots.start();
    }

    @Override protected void onPause() {
        resumed = false;
        if (peerId != null && peerId.equals(KinnectMessagingService.openChatPeer)) {
            KinnectMessagingService.openChatPeer = null;
        }
        if (screenshots != null) screenshots.stop();
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (query != null && listener != null) query.removeEventListener(listener);
        if (peerWatch != null) peerWatch.stop();
        super.onDestroy();
    }

    // ---------------------------------------------------------------- adapter
    private class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.Holder> {
        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            final Message m = messages.get(position);

            if (Message.SYSTEM.equals(m.type)) {
                h.sysText.setVisibility(View.VISIBLE);
                h.msgRow.setVisibility(View.GONE);
                h.sysText.setText(m.text);
                return;
            }
            h.sysText.setVisibility(View.GONE);
            h.msgRow.setVisibility(View.VISIBLE);

            final boolean mine = me.equals(m.senderId);
            h.msgRow.setGravity(mine ? Gravity.END : Gravity.START);
            h.avatar.setVisibility(mine ? View.GONE : View.VISIBLE);
            if (!mine) h.avatar.bind(peer);
            h.bubble.setBackgroundResource(mine ? R.drawable.bg_bubble_out : R.drawable.bg_bubble_in);
            int fg = mine ? 0xFFFFFFFF : 0xFF1E1E1E;
            h.text.setTextColor(fg);
            h.quote.setTextColor(fg);
            h.postLabel.setTextColor(fg);
            h.col.setGravity(mine ? Gravity.END : Gravity.START);

            // reply quote
            if (m.replyToId != null) {
                h.quote.setVisibility(View.VISIBLE);
                h.quote.setText((m.replyToSender == null ? "" : m.replyToSender + ": ") + (m.replyToText == null ? "" : m.replyToText));
            } else {
                h.quote.setVisibility(View.GONE);
            }

            // content
            boolean isImage = Message.IMAGE.equals(m.type);
            boolean isPost = Message.POST.equals(m.type);
            if ((isImage || isPost) && m.image != null && !m.image.isEmpty()) {
                h.image.setVisibility(View.VISIBLE);
                ImageUtil.show(h.image, m.image);
            } else {
                h.image.setVisibility(View.GONE);
            }
            h.postLabel.setVisibility(isPost ? View.VISIBLE : View.GONE);
            String body = isPost ? m.postPreview : m.text;
            h.text.setVisibility(body == null || body.isEmpty() ? View.GONE : View.VISIBLE);
            h.text.setText(body == null ? "" : body);

            // meta
            StringBuilder meta = new StringBuilder();
            if (m.edited) meta.append("Edited · ");
            meta.append(TimeUtil.clock(m.timestamp));
            int hearts = m.hearts == null ? 0 : m.hearts.size();
            if (hearts > 0) meta.append("  ❤️ ").append(hearts);
            h.meta.setText(meta.toString());

            h.bind(m);
        }

        @Override public int getItemCount() { return messages.size(); }

        class Holder extends RecyclerView.ViewHolder {
            final View msgRow, bubble;
            final LinearLayout col;
            final AvatarView avatar;
            final TextView sysText, quote, postLabel, text, meta;
            final ImageView image;
            Message current;

            Holder(View v) {
                super(v);
                sysText = v.findViewById(R.id.sysText);
                msgRow = v.findViewById(R.id.msgRow);
                col = v.findViewById(R.id.bubbleCol);
                bubble = v.findViewById(R.id.bubble);
                avatar = v.findViewById(R.id.msgAvatar);
                quote = v.findViewById(R.id.replyQuote);
                image = v.findViewById(R.id.msgImage);
                postLabel = v.findViewById(R.id.postLabel);
                text = v.findViewById(R.id.msgText);
                meta = v.findViewById(R.id.msgMeta);

                final GestureDetector gd = new GestureDetector(v.getContext(), new GestureDetector.SimpleOnGestureListener() {
                    @Override public boolean onDown(MotionEvent e) { return true; }
                    @Override public boolean onDoubleTap(MotionEvent e) {
                        if (current == null || Message.SYSTEM.equals(current.type)) return true;
                        boolean hearted = current.hearts != null && current.hearts.containsKey(me);
                        ChatRepo.toggleHeart(peerId, current, hearted);
                        return true;
                    }
                    @Override public void onLongPress(MotionEvent e) {
                        if (current != null) showMessageMenu(current);
                    }
                    @Override public boolean onSingleTapConfirmed(MotionEvent e) {
                        if (current != null && Message.POST.equals(current.type) && current.postId != null) {
                            startActivity(new Intent(ChatActivity.this, LikeActivity.class)
                                    .putExtra(LikeActivity.EXTRA_POST_ID, current.postId));
                        }
                        return true;
                    }
                });
                bubble.setOnTouchListener((x, ev) -> gd.onTouchEvent(ev));
            }

            void bind(Message m) {
                current = m;
            }
        }
    }
}
