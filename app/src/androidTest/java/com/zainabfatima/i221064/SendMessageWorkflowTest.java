package com.zainabfatima.i221064;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.zainabfatima.i221064.model.Message;
import com.zainabfatima.i221064.util.Db;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

/**
 * Espresso test 2 - Messaging workflow against Realtime Database.
 *
 * Signs in as the test account, opens a chat with TEST_FRIEND_UID, types a unique message and taps
 * Send. The test passes when (a) the message bubble appears in the conversation and (b) the same
 * message really exists under /chats/{chatId}/messages in the database. The message is deleted afterwards.
 */
@RunWith(AndroidJUnit4.class)
public class SendMessageWorkflowTest {

    private ActivityScenario<ChatActivity> scenario;
    private String sentText;

    @Before
    public void setUp() throws Exception {
        assumeTrue("Set TEST_EMAIL / TEST_PASSWORD / TEST_FRIEND_UID in local.properties",
                EspressoSupport.hasTestAccount() && !BuildConfig.TEST_FRIEND_UID.isEmpty());
        EspressoSupport.signIn();
    }

    @After
    public void tearDown() throws Exception {
        if (scenario != null) scenario.close();
        // remove the message created by the test
        String me = Db.uid();
        if (me != null && sentText != null) {
            DataSnapshot all = Tasks.await(Db.messages(Db.chatId(me, BuildConfig.TEST_FRIEND_UID))
                    .limitToLast(20).get(), 30, TimeUnit.SECONDS);
            for (DataSnapshot c : all.getChildren()) {
                Message m = c.getValue(Message.class);
                if (m != null && sentText.equals(m.text) && me.equals(m.senderId)) {
                    Tasks.await(c.getRef().removeValue(), 30, TimeUnit.SECONDS);
                }
            }
        }
        FirebaseAuth.getInstance().signOut();
    }

    @Test
    public void sendingTextMessage_appearsInChatAndIsStoredInDatabase() throws Exception {
        sentText = "espresso-" + System.currentTimeMillis();

        Intent intent = new Intent(EspressoSupport.context(), ChatActivity.class)
                .putExtra(ChatActivity.EXTRA_PEER_ID, BuildConfig.TEST_FRIEND_UID);
        scenario = ActivityScenario.launch(intent);

        onView(withId(R.id.etMsg)).perform(replaceText(sentText));
        closeSoftKeyboard();
        onView(withId(R.id.btnSend)).perform(click());

        // (a) UI: the new bubble is shown
        EspressoSupport.waitUntilDisplayed(allOf(withText(sentText), isDisplayed()), 30_000);

        // (b) Backend: the message was really written for this conversation
        String me = Db.uid();
        assertNotNull(me);
        DataSnapshot all = Tasks.await(Db.messages(Db.chatId(me, BuildConfig.TEST_FRIEND_UID))
                .limitToLast(20).get(), 30, TimeUnit.SECONDS);
        boolean stored = false;
        for (DataSnapshot c : all.getChildren()) {
            Message m = c.getValue(Message.class);
            if (m != null && sentText.equals(m.text) && me.equals(m.senderId)) stored = true;
        }
        assertTrue("Message should exist in /chats/.../messages", stored);
    }
}
