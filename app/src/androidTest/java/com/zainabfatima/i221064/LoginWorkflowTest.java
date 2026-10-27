package com.zainabfatima.i221064;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Espresso test 1 - Authentication workflow against Firebase.
 *
 * Starts on the Log in screen while signed out, enters the real test account's credentials,
 * taps Log in and expects Firebase Auth to sign in and the Home feed to appear.
 * Then it checks that a wrong password is rejected with an error message and no session.
 */
@RunWith(AndroidJUnit4.class)
public class LoginWorkflowTest {

    private ActivityScenario<LoginActivity> scenario;

    @Before
    public void setUp() {
        assumeTrue("Set TEST_EMAIL / TEST_PASSWORD in local.properties", EspressoSupport.hasTestAccount());
        EspressoSupport.signOut();
    }

    @After
    public void tearDown() {
        if (scenario != null) scenario.close();
        EspressoSupport.signOut();
    }

    @Test
    public void validCredentials_signInAndOpenHome() throws Exception {
        scenario = ActivityScenario.launch(LoginActivity.class);

        onView(withId(R.id.etEmail)).perform(replaceText(BuildConfig.TEST_EMAIL));
        onView(withId(R.id.etPassword)).perform(replaceText(BuildConfig.TEST_PASSWORD));
        closeSoftKeyboard();
        onView(withId(R.id.button)).perform(click());

        // Home (or the one-time profile setup for brand new accounts) must show up after Firebase answers
        EspressoSupport.waitForId(R.id.feed, 30_000);
        onView(withId(R.id.feed)).check(matches(isDisplayed()));

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assertNotNull("Firebase should have a signed-in user", user);
        assertEquals(BuildConfig.TEST_EMAIL.toLowerCase(), user.getEmail().toLowerCase());
    }

    @Test
    public void wrongPassword_showsErrorAndStaysSignedOut() throws Exception {
        scenario = ActivityScenario.launch(LoginActivity.class);

        onView(withId(R.id.etEmail)).perform(replaceText(BuildConfig.TEST_EMAIL));
        onView(withId(R.id.etPassword)).perform(replaceText("definitely-not-the-password-1"));
        closeSoftKeyboard();
        onView(withId(R.id.button)).perform(click());

        EspressoSupport.waitForId(R.id.tvError, 30_000);
        onView(withId(R.id.tvError)).check(matches(isDisplayed()));
        assertEquals(null, FirebaseAuth.getInstance().getCurrentUser());
    }
}
