package com.zainabfatima.i221064;

import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.AssertionFailedError;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;

import org.hamcrest.Matcher;

import java.util.concurrent.TimeUnit;

import android.view.View;

/** Small helpers shared by the two backend-connected UI tests. */
final class EspressoSupport {

    private EspressoSupport() { }

    /** Polls until the matcher is displayed (network calls make a fixed sleep unreliable). */
    static void waitUntilDisplayed(Matcher<View> matcher, long timeoutMs) throws InterruptedException {
        long end = System.currentTimeMillis() + timeoutMs;
        Throwable last = null;
        while (System.currentTimeMillis() < end) {
            try {
                onView(matcher).check(ViewAssertions.matches(isDisplayed()));
                return;
            } catch (NoMatchingViewException | AssertionFailedError e) {
                last = e;
                Thread.sleep(250);
            }
        }
        throw new AssertionError("Timed out waiting for view: " + matcher, last);
    }

    static void waitForId(int id, long timeoutMs) throws InterruptedException {
        waitUntilDisplayed(withId(id), timeoutMs);
    }

    static void signOut() {
        FirebaseAuth.getInstance().signOut();
    }

    /** Blocking sign in with the test account from local.properties. */
    static void signIn() throws Exception {
        Tasks.await(FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(BuildConfig.TEST_EMAIL, BuildConfig.TEST_PASSWORD), 30, TimeUnit.SECONDS);
    }

    static boolean hasTestAccount() {
        return !BuildConfig.TEST_EMAIL.isEmpty() && !BuildConfig.TEST_PASSWORD.isEmpty();
    }

    static android.content.Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }
}
