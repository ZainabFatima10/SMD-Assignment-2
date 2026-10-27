package com.zainabfatima.i221064;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.zainabfatima.i221064.feed.PostRepo;
import com.zainabfatima.i221064.util.Async;
import com.zainabfatima.i221064.util.AvatarView;
import com.zainabfatima.i221064.util.Db;
import com.zainabfatima.i221064.util.ImageUtil;
import com.zainabfatima.i221064.util.UserCache;

import java.util.ArrayList;
import java.util.List;

/** Screen 7 - Create post: text plus any number of images chosen via the photo picker (08) or camera (09). */
public class CreatePostActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_PICKER = "open_picker";

    private final List<Uri> images = new ArrayList<>();
    private LinearLayout strip;
    private EditText text;
    private Button btnPost;

    private final ActivityResultLauncher<Intent> pickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() != RESULT_OK || r.getData() == null) return;
                ArrayList<String> uris = r.getData().getStringArrayListExtra(PictureSendActivity.RESULT_URIS);
                if (uris == null) return;
                for (String s : uris) {
                    Uri u = Uri.parse(s);
                    if (!images.contains(u)) images.add(u);
                }
                renderStrip();
            });

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() != RESULT_OK || r.getData() == null) return;
                String s = r.getData().getStringExtra(CameraActivity.RESULT_URI);
                if (s != null) {
                    images.add(Uri.parse(s));
                    renderStrip();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_post_screen);

        strip = findViewById(R.id.selectedStrip);
        text = findViewById(R.id.post_text);
        btnPost = findViewById(R.id.btnPost);

        AvatarView avatar = findViewById(R.id.seagreenpfp);
        TextView name = findViewById(R.id.authorName);
        String uid = Db.uid();
        if (uid != null) {
            UserCache.get(uid, u -> {
                if (u == null) return;
                avatar.bind(u);
                name.setText(u.displayName());
            });
        }

        NavHelper.click(this, R.id.backarrow, this::finish);
        NavHelper.click(this, R.id.btnPost, this::post);
        NavHelper.click(this, R.id.public_button, () -> NavHelper.toast(this, "Visible to you and your friends"));
        NavHelper.click(this, R.id.line1, this::openPicker);
        NavHelper.click(this, R.id.line6, () -> cameraLauncher.launch(
                new Intent(this, CameraActivity.class).putExtra(CameraActivity.EXTRA_RESULT_MODE, true)));

        if (savedInstanceState == null && getIntent().getBooleanExtra(EXTRA_OPEN_PICKER, false)) openPicker();
    }

    private void openPicker() {
        pickerLauncher.launch(new Intent(this, PictureSendActivity.class)
                .putExtra(PictureSendActivity.EXTRA_MULTI, true));
    }

    private void renderStrip() {
        strip.removeAllViews();
        float d = getResources().getDisplayMetrics().density;
        int size = (int) (110 * d), gap = (int) (6 * d);
        for (final Uri u : images) {
            FrameLayout cell = new FrameLayout(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMarginEnd(gap);
            cell.setLayoutParams(lp);

            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(this).load(u).into(iv);
            cell.addView(iv, new FrameLayout.LayoutParams(-1, -1));

            TextView x = new TextView(this);
            x.setText("✕");
            x.setTextColor(0xFFFFFFFF);
            x.setBackgroundColor(0x99000000);
            x.setGravity(android.view.Gravity.CENTER);
            FrameLayout.LayoutParams xp = new FrameLayout.LayoutParams((int) (28 * d), (int) (28 * d),
                    android.view.Gravity.TOP | android.view.Gravity.END);
            x.setLayoutParams(xp);
            x.setOnClickListener(v -> {
                images.remove(u);
                renderStrip();
            });
            cell.addView(x);
            strip.addView(cell);
        }
    }

    private void post() {
        final String body = text.getText().toString().trim();
        if (body.isEmpty() && images.isEmpty()) {
            NavHelper.toast(this, "Write something or add a photo");
            return;
        }
        btnPost.setEnabled(false);
        final List<Uri> toEncode = new ArrayList<>(images);
        Async.run(() -> {
            List<String> encoded = new ArrayList<>();
            for (Uri u : toEncode) encoded.add(ImageUtil.uriToBase64(this, u, ImageUtil.POST_DIM, 75));
            return encoded;
        }, (encoded, err) -> {
            if (encoded == null) {
                btnPost.setEnabled(true);
                NavHelper.toast(this, "Couldn't read one of the images");
                return;
            }
            PostRepo.create(body, encoded);
            NavHelper.toast(this, "Posted");
            finish();
        });
    }
}
