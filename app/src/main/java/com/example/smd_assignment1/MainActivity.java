package com.example.smd_assignment1;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

/** Screen 1 - Splash (launcher). Shows the logo, then moves to Login. */
public class MainActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable next = () -> {
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_screen);
        // tap anywhere to skip the wait
        findViewById(R.id.main).setOnClickListener(v -> {
            handler.removeCallbacks(next);
            next.run();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(next, 2000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(next);
    }
}
