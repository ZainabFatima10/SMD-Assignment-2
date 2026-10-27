package com.zainabfatima.i221064.util;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/** Time as the Firebase server sees it, so the 5-minute edit window and 24-hour story expiry are fair. */
public final class Clock {

    private static volatile long offset = 0;
    private static boolean started = false;

    private Clock() { }

    public static synchronized void start() {
        if (started) return;
        started = true;
        FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset")
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot s) {
                        Long o = s.getValue(Long.class);
                        offset = o == null ? 0 : o;
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) { }
                });
    }

    public static long now() {
        return System.currentTimeMillis() + offset;
    }
}
