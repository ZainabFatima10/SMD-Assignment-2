package com.zainabfatima.i221064.feed;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.chat.ChatRepo;
import com.zainabfatima.i221064.model.Post;
import com.zainabfatima.i221064.util.Db;
import com.zainabfatima.i221064.util.UserCache;

import java.util.ArrayList;
import java.util.List;

/** "Share": choose a friend and send them the post as a chat message. */
public final class ShareHelper {

    private ShareHelper() { }

    public static void sharePostToChat(final Context ctx, final Post post) {
        String me = Db.uid();
        if (me == null) return;
        Db.friends(me).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                final List<String> uids = new ArrayList<>();
                for (DataSnapshot c : s.getChildren()) uids.add(c.getKey());
                if (uids.isEmpty()) {
                    Toast.makeText(ctx, "Add some friends first to share with them", Toast.LENGTH_SHORT).show();
                    return;
                }
                final String[] names = new String[uids.size()];
                final int[] pending = {uids.size()};
                for (int i = 0; i < uids.size(); i++) {
                    final int index = i;
                    UserCache.get(uids.get(i), u -> {
                        names[index] = u == null ? "Kinnect user" : u.displayName();
                        if (--pending[0] == 0) showDialog(ctx, post, uids, names);
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        });
    }

    private static void showDialog(final Context ctx, final Post post, final List<String> uids, String[] names) {
        new AlertDialog.Builder(ctx)
                .setTitle("Send to")
                .setItems(names, (d, which) -> {
                    ChatRepo.sendPost(uids.get(which), post);
                    Toast.makeText(ctx, "Post sent", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
