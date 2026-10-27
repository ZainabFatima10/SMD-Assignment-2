package com.zainabfatima.i221064.chat;

import com.google.firebase.database.ServerValue;
import com.zainabfatima.i221064.model.AppNotification;
import com.zainabfatima.i221064.model.Message;
import com.zainabfatima.i221064.model.Post;
import com.zainabfatima.i221064.util.Clock;
import com.zainabfatima.i221064.util.Db;
import com.zainabfatima.i221064.util.Notifier;

import java.util.HashMap;
import java.util.Map;

/**
 * All the writes of the messaging feature. A send is a single multi-path update that stores the
 * message, refreshes both people's "Chats" row and bumps the receiver's unread counter.
 * Push notifications are produced by the Cloud Function that watches /chats/.../messages.
 */
public final class ChatRepo {

    private ChatRepo() { }

    // --------------------------------------------------------------- sending
    public static String sendText(String peer, String text) {
        return sendText(peer, text, null, null);
    }

    /** {@code replyTo} may be null; {@code replyToSenderName} is the display name of its author. */
    public static String sendText(String peer, String text, Message replyTo, String replyToSenderName) {
        Message m = base(Message.TEXT);
        m.text = text;
        if (replyTo != null) applyReply(m, replyTo, replyToSenderName);
        return push(peer, m, text);
    }

    public static String sendImage(String peer, String base64, Message replyTo, String replyToSenderName) {
        Message m = base(Message.IMAGE);
        m.image = base64;
        m.text = "";
        if (replyTo != null) applyReply(m, replyTo, replyToSenderName);
        return push(peer, m, "📷 Photo");
    }

    public static String sendPost(String peer, Post post) {
        Message m = base(Message.POST);
        m.postId = post.id;
        String preview = post.text == null ? "" : post.text;
        if (preview.length() > 120) preview = preview.substring(0, 120) + "…";
        m.postPreview = preview;
        m.text = "";
        // a small thumbnail of the first picture so the bubble looks like a post
        if (post.images != null && !post.images.isEmpty()) m.image = post.images.get(0);
        return push(peer, m, "Shared a post");
    }

    /** Message shown in the middle of the chat, e.g. "Aisha took a screenshot". */
    public static String sendSystem(String peer, String text) {
        Message m = base(Message.SYSTEM);
        m.text = text;
        return push(peer, m, text);
    }

    private static Message base(String type) {
        Message m = new Message();
        m.senderId = Db.uid();
        m.type = type;
        m.timestamp = Clock.now();
        return m;
    }

    private static void applyReply(Message m, Message replyTo, String senderName) {
        m.replyToId = replyTo.id;
        m.replyToSender = senderName;
        String t = replyTo.text;
        if (Message.IMAGE.equals(replyTo.type)) t = "📷 Photo";
        else if (Message.POST.equals(replyTo.type)) t = "Shared a post";
        if (t == null) t = "";
        if (t.length() > 80) t = t.substring(0, 80) + "…";
        m.replyToText = t;
    }

    private static String push(String peer, Message m, String preview) {
        String me = Db.uid();
        if (me == null || peer == null) return null;
        String chatId = Db.chatId(me, peer);
        String id = Db.messages(chatId).push().getKey();
        m.id = id;
        if (m.timestamp == 0) m.timestamp = Clock.now();

        Map<String, Object> up = new HashMap<>();
        up.put("chats/" + chatId + "/messages/" + id, m);
        // sender's row
        up.put("userChats/" + me + "/" + peer + "/peerId", peer);
        up.put("userChats/" + me + "/" + peer + "/lastMessage", preview);
        up.put("userChats/" + me + "/" + peer + "/lastSenderId", me);
        up.put("userChats/" + me + "/" + peer + "/timestamp", m.timestamp);
        // receiver's row (+1 unread, except for system messages that are meant for the receiver too)
        up.put("userChats/" + peer + "/" + me + "/peerId", me);
        up.put("userChats/" + peer + "/" + me + "/lastMessage", preview);
        up.put("userChats/" + peer + "/" + me + "/lastSenderId", me);
        up.put("userChats/" + peer + "/" + me + "/timestamp", m.timestamp);
        up.put("userChats/" + peer + "/" + me + "/unread", ServerValue.increment(1));
        Db.root().updateChildren(up);
        return id;
    }

    // ---------------------------------------------------------- edit / delete
    public static boolean withinEditWindow(Message m) {
        return m != null && Clock.now() - m.timestamp <= Message.EDIT_WINDOW_MS;
    }

    public static boolean canModify(Message m) {
        return m != null && Db.uid() != null && Db.uid().equals(m.senderId)
                && !Message.SYSTEM.equals(m.type) && withinEditWindow(m);
    }

    /** Only the sender, only text messages, only within 5 minutes. Returns false when refused. */
    public static boolean edit(String peer, Message m, String newText, boolean isLast) {
        if (!canModify(m) || !Message.TEXT.equals(m.type)) return false;
        String chatId = Db.chatId(Db.uid(), peer);
        Map<String, Object> up = new HashMap<>();
        up.put("chats/" + chatId + "/messages/" + m.id + "/text", newText);
        up.put("chats/" + chatId + "/messages/" + m.id + "/edited", true);
        if (isLast) {
            up.put("userChats/" + Db.uid() + "/" + peer + "/lastMessage", newText);
            up.put("userChats/" + peer + "/" + Db.uid() + "/lastMessage", newText);
        }
        Db.root().updateChildren(up);
        return true;
    }

    /** {@code previousPreview} is what the chat row should show afterwards when the last message is removed. */
    public static boolean delete(String peer, Message m, boolean isLast, String previousPreview) {
        if (!canModify(m)) return false;
        String chatId = Db.chatId(Db.uid(), peer);
        Map<String, Object> up = new HashMap<>();
        up.put("chats/" + chatId + "/messages/" + m.id, null);
        if (isLast) {
            String p = previousPreview == null ? "" : previousPreview;
            up.put("userChats/" + Db.uid() + "/" + peer + "/lastMessage", p);
            up.put("userChats/" + peer + "/" + Db.uid() + "/lastMessage", p);
        }
        Db.root().updateChildren(up);
        return true;
    }

    // ---------------------------------------------------------------- hearts
    public static void toggleHeart(String peer, Message m, boolean currentlyHearted) {
        String me = Db.uid();
        if (me == null) return;
        String chatId = Db.chatId(me, peer);
        Db.messages(chatId).child(m.id).child("hearts").child(me).setValue(currentlyHearted ? null : Boolean.TRUE);
    }

    // ----------------------------------------------------------------- unread
    public static void markRead(String peer) {
        String me = Db.uid();
        if (me == null || peer == null) return;
        Db.userChats(me).child(peer).child("unread").setValue(0);
    }

    // --------------------------------------------------------- screenshot alert
    /** Tells {@code peer} that the signed-in user took a screenshot of their chat. */
    public static void reportScreenshot(String peer, String myDisplayName) {
        sendSystem(peer, myDisplayName + " took a screenshot");
        Notifier.send(peer, AppNotification.SCREENSHOT, Db.chatId(Db.uid(), peer),
                "took a screenshot of your chat.");
    }

    /** Preview text for a message (used when a delete exposes the previous message in the chat row). */
    public static String previewOf(Message m) {
        if (m == null) return "";
        if (Message.IMAGE.equals(m.type)) return "📷 Photo";
        if (Message.POST.equals(m.type)) return "Shared a post";
        return m.text == null ? "" : m.text;
    }
}
