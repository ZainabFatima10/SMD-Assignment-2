# SMD Assignment 2 – Kinnect (Firebase backend)

| | |
|---|---|
| **Group** | Zainab Fatima (22i-1064), Ayan Asif (22i-1097) |
| **Course** | SMD (Mobile Application Development) – Assignment 2 |

Kinnect from Assignment 1 (same UI) now runs on a real backend: Firebase Auth, Realtime Database, Cloud Messaging and Agora calls.

## Features
Splash (5 s, then Home or Login) · email/password auth with live password strength, unique username + bio on first login, "Recent login" card, logout ·
stories (camera/gallery, image or video, text label, Your story / Close friends, 24 h expiry) · posts with multiple images and "+N" grid ·
six reactions (one per user, changeable) · comments with replies, likes and image comments · real-time chat (text, images, shared posts,
reply, heart, edit/delete within 5 min) · voice/video calls (Agora) · friend requests (Add → Requested → Friends) · mutual friends ·
edit profile · FCM push for new messages · live user search · online/offline presence · screenshot detection with a system message.

## Setup
1. **Firebase**: create a project, add the Android app `com.zainabfatima.i221064`, download `google-services.json` into `app/`.
   Enable **Authentication → Email/Password** and create a **Realtime Database**.
2. **Rules**: paste `database.rules.json` into the Realtime Database rules (or `firebase deploy --only database`).
3. **Cloud Functions (push notifications)**: `cd functions && npm install`, then `firebase deploy --only functions` (Blaze plan required).
4. **Agora**: create a project in testing mode and put its App ID in `local.properties`.
5. `local.properties` (git-ignored):
   ```
   AGORA_APP_ID=your_agora_app_id
   TEST_EMAIL=test account email
   TEST_PASSWORD=its password
   TEST_FRIEND_UID=uid of a second account that is that user's friend
   ```
6. Open in Android Studio, sync Gradle, run on a device/emulator (Android 7.0+). Calls and camera need a real device or camera-enabled emulator.

## Espresso tests (`app/src/androidTest`)
- `LoginWorkflowTest` – signs in through the Login screen against Firebase Auth and expects Home; a wrong password shows an error and no session.
- `SendMessageWorkflowTest` – opens a chat, sends a unique message, checks it in the UI **and** in `/chats/.../messages`, then cleans it up.

Run with a device connected: `./gradlew connectedAndroidTest` (tests are skipped if the `TEST_*` values are missing).

## Notes
- Images/videos are stored as Base64 in the Realtime Database (no Firebase Storage); videos are capped at about 4 MB.
- Close-friends story visibility is enforced in the client; stories are readable by any signed-in user.
