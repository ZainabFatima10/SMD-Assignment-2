package com.zainabfatima.i221064.feed;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.R;
import com.zainabfatima.i221064.model.Story;
import com.zainabfatima.i221064.util.Async;
import com.zainabfatima.i221064.util.Db;
import com.zainabfatima.i221064.util.ImageUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Plays a person's stories one after another inside the story viewer / your story layouts:
 * segmented progress bar, tap right = next, tap left = previous, images last 5 seconds,
 * videos play to their end. Media is fetched lazily from /storyMedia/{id}.
 */
public class StoryPlayer {

    public interface Listener {
        void onShown(Story story, int index);
        void onFinished();
    }

    private static final long IMAGE_MS = 5000;

    private final Activity a;
    private final Listener listener;
    private final ImageView image;
    private final VideoView video;
    private final LinearLayout progress;
    private final TextView label;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Story> stories = new ArrayList<>();
    private final List<View> fills = new ArrayList<>();

    private int index = -1;
    private long shownAt;
    private boolean paused, videoReady, finished;
    private long pausedElapsed;
    private ValueEventListener mediaListener;
    private com.google.firebase.database.DatabaseReference mediaRef;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (finished) return;
            if (!paused && index >= 0 && index < stories.size()) {
                Story s = stories.get(index);
                float f;
                if (Story.VIDEO.equals(s.mediaType)) {
                    int dur = videoReady ? video.getDuration() : 0;
                    f = dur > 0 ? (float) video.getCurrentPosition() / dur : 0f;
                } else {
                    f = Math.min(1f, (float) (System.currentTimeMillis() - shownAt) / IMAGE_MS);
                    if (f >= 1f) {
                        next();
                        return;
                    }
                }
                setFill(index, f);
            }
            handler.postDelayed(this, 50);
        }
    };

    public StoryPlayer(Activity a, Listener l, int labelViewId) {
        this.a = a;
        this.listener = l;
        image = a.findViewById(R.id.story_image);
        video = a.findViewById(R.id.story_video);
        progress = a.findViewById(R.id.story_progress);
        label = a.findViewById(labelViewId);
        a.findViewById(R.id.tap_left).setOnClickListener(v -> previous());
        a.findViewById(R.id.tap_right).setOnClickListener(v -> next());
    }

    public Story current() {
        return index >= 0 && index < stories.size() ? stories.get(index) : null;
    }

    public int size() {
        return stories.size();
    }

    public void start(List<Story> list, int startAt) {
        stories.clear();
        stories.addAll(list);
        buildProgress();
        finished = false;
        show(Math.max(0, Math.min(startAt, stories.size() - 1)));
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    /** Removes the story currently on screen (after the owner deleted it). */
    public void removeCurrent() {
        if (index < 0 || index >= stories.size()) return;
        stories.remove(index);
        if (stories.isEmpty()) {
            finishNow();
            return;
        }
        buildProgress();
        show(Math.min(index, stories.size() - 1));
    }

    private void buildProgress() {
        progress.removeAllViews();
        fills.clear();
        float d = a.getResources().getDisplayMetrics().density;
        for (int i = 0; i < stories.size(); i++) {
            FrameLayout seg = new FrameLayout(a);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
            if (i < stories.size() - 1) lp.setMarginEnd((int) (4 * d));
            seg.setLayoutParams(lp);
            seg.setBackgroundResource(R.drawable.gray_progress);
            View fill = new View(a);
            fill.setBackgroundResource(R.drawable.white_progress);
            fill.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
            fill.setPivotX(0f);
            fill.setScaleX(0f);
            seg.addView(fill);
            progress.addView(seg);
            fills.add(fill);
        }
    }

    private void setFill(int i, float f) {
        if (i >= 0 && i < fills.size()) fills.get(i).setScaleX(f);
    }

    private void show(int i) {
        detachMedia();
        index = i;
        for (int k = 0; k < fills.size(); k++) setFill(k, k < i ? 1f : 0f);
        final Story s = stories.get(i);
        shownAt = System.currentTimeMillis();
        videoReady = false;
        pausedElapsed = 0;

        if (label != null) {
            boolean has = s.text != null && !s.text.isEmpty();
            label.setText(has ? s.text : "");
            label.setVisibility(has ? View.VISIBLE : View.GONE);
        }
        video.stopPlayback();
        video.setVisibility(View.GONE);
        image.setVisibility(View.VISIBLE);
        ImageUtil.show(image, s.thumb);          // instant placeholder while the full media loads
        listener.onShown(s, i);

        mediaRef = Db.storyMedia(s.id).child("data");
        mediaListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                final String data = snap.getValue(String.class);
                if (data == null || index < 0 || index >= stories.size() || stories.get(index) != s) return;
                if (Story.VIDEO.equals(s.mediaType)) playVideo(s, data);
                else ImageUtil.show(image, data);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        };
        mediaRef.addListenerForSingleValueEvent(mediaListener);
    }

    private void playVideo(final Story s, final String data) {
        Async.run(() -> ImageUtil.base64ToFile(a, data, "story_" + s.id + ".mp4"), (file, err) -> {
            if (file == null || finished || index < 0 || index >= stories.size() || stories.get(index) != s) return;
            video.setVisibility(View.VISIBLE);
            image.setVisibility(View.GONE);
            video.setOnPreparedListener(mp -> {
                videoReady = true;
                if (!paused) video.start();
            });
            video.setOnCompletionListener(mp -> next());
            video.setOnErrorListener((mp, w, e) -> {
                next();
                return true;
            });
            video.setVideoPath(file.getAbsolutePath());
        });
    }

    private void detachMedia() {
        if (mediaRef != null && mediaListener != null) mediaRef.removeEventListener(mediaListener);
        mediaRef = null;
        mediaListener = null;
    }

    public void next() {
        if (index + 1 >= stories.size()) {
            finishNow();
            return;
        }
        show(index + 1);
    }

    public void previous() {
        show(Math.max(0, index - 1));
    }

    private void finishNow() {
        finished = true;
        handler.removeCallbacks(tick);
        detachMedia();
        video.stopPlayback();
        listener.onFinished();
    }

    public void pause() {
        if (paused) return;
        paused = true;
        pausedElapsed = System.currentTimeMillis() - shownAt;
        if (videoReady && video.isPlaying()) video.pause();
    }

    public void resume() {
        if (!paused) return;
        paused = false;
        shownAt = System.currentTimeMillis() - pausedElapsed;
        if (videoReady) video.start();
    }

    public void release() {
        finished = true;
        handler.removeCallbacksAndMessages(null);
        detachMedia();
        video.stopPlayback();
    }
}
