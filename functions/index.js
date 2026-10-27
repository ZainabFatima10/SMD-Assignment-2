/**
 * Kinnect Cloud Functions
 *
 *  - notifyNewMessage : push notification (FCM) when a chat message is written
 *                       (also covers the "X took a screenshot" system message)
 *  - notifyIncomingCall : push so a call rings even when the app is closed
 *  - cleanupStories   : hourly sweep that deletes stories older than 24 hours
 *
 * Deploy:  cd functions && npm install && firebase deploy --only functions
 */
const { onValueCreated, onValueWritten } = require("firebase-functions/v2/database");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.database();

async function tokenOf(uid) {
  const snap = await db.ref(`users/${uid}/fcmToken`).get();
  return snap.val();
}

async function nameOf(uid) {
  const snap = await db.ref(`users/${uid}`).get();
  const u = snap.val() || {};
  return u.name || u.username || "Someone";
}

async function sendData(token, data) {
  if (!token) return;
  try {
    await admin.messaging().send({
      token,
      data,
      android: { priority: "high" },
    });
  } catch (e) {
    // stale tokens are removed when the user logs out / re-registers
    console.warn("FCM send failed", e.code || e.message);
  }
}

function preview(m) {
  if (m.type === "image") return "Sent you a photo";
  if (m.type === "post") return "Shared a post with you";
  return m.text || "";
}

exports.notifyNewMessage = onValueCreated("/chats/{chatId}/messages/{messageId}", async (event) => {
  const m = event.data.val();
  if (!m || !m.senderId) return;
  const [a, b] = event.params.chatId.split("_");
  const recipient = m.senderId === a ? b : a;
  if (!recipient) return;

  const token = await tokenOf(recipient);
  if (m.type === "system") {
    await sendData(token, {
      type: "screenshot",
      peerId: m.senderId,
      title: "Screenshot alert",
      body: m.text || "Someone took a screenshot of your chat",
    });
    return;
  }
  const sender = await nameOf(m.senderId);
  await sendData(token, {
    type: "message",
    peerId: m.senderId,
    title: sender,
    body: preview(m),
  });
});

exports.notifyIncomingCall = onValueWritten("/calls/{calleeId}", async (event) => {
  const after = event.data.after.val();
  const before = event.data.before.val();
  if (!after || after.status !== "ringing") return;
  if (before && before.channel === after.channel) return;
  const token = await tokenOf(event.params.calleeId);
  await sendData(token, {
    type: "call",
    callerId: after.callerId || "",
    callerName: after.callerName || "",
    channel: after.channel || "",
    video: String(!!after.video),
  });
});

exports.cleanupStories = onSchedule("every 60 minutes", async () => {
  const now = Date.now();
  const owners = await db.ref("stories").get();
  const updates = {};
  owners.forEach((owner) => {
    owner.forEach((story) => {
      const s = story.val();
      if (s && s.expiresAt && s.expiresAt < now) {
        updates[`stories/${owner.key}/${story.key}`] = null;
        updates[`storyMedia/${story.key}`] = null;
        updates[`storyViews/${story.key}`] = null;
      }
    });
  });
  if (Object.keys(updates).length) await db.ref().update(updates);
});
