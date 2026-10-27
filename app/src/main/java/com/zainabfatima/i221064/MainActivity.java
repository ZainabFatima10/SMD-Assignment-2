package com.zainabfatima.i221064;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.zainabfatima.i221064.util.Db;

/**
 * Screen 1 - Splash (launcher). Stays for 5 seconds, then goes straight to Home when somebody is
 * already signed in, otherwise to Log in.
 */
public class MainActivity extends AppCompatActivity {

    public static final long SPLASH_MS = 5000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long shownAt;
    private long remaining = SPLASH_MS;

    private final Runnable next = () -> {
        Class<?> target = Db.isLoggedIn() ? StartSocialMediaActivity.class : LoginActivity.class;
        startActivity(new Intent(MainActivity.this, target));
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_screen);
    }

    @Override
    protected void onResume() {
        super.onResume();
        shownAt = System.currentTimeMillis();
        handler.postDelayed(next, remaining);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(next);
        // if the app is backgrounded mid-splash, only wait out the rest of the 5 seconds
        remaining = Math.max(0, remaining - (System.currentTimeMillis() - shownAt));
    }
}
