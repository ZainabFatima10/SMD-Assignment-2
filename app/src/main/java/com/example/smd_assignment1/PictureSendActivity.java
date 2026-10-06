package com.example.smd_assignment1;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

/** Screen 8 - Photo picker. */
public class PictureSendActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picture_send_screen);

        NavHelper.click(this, R.id.cancel_text, this::finish);
        NavHelper.click(this, R.id.most_relevant_text, () -> {      // "Next (2)"
            NavHelper.toast(this, "2 items selected");
            finish();
        });
        NavHelper.click(this, R.id.recents_textview, () -> NavHelper.toast(this, "Recents"));
        NavHelper.click(this, R.id.btnSelectMultiple, () -> NavHelper.toast(this, "Select multiple"));

        // Tapping a thumbnail shows it in the big preview
        final ImageView preview = findViewById(R.id.previewImage);
        for (int i = 1; i <= 16; i++) {
            int cellId = getResources().getIdentifier("img" + i, "id", getPackageName());
            int thumbId = getResources().getIdentifier("w_img" + i, "id", getPackageName());
            final ImageView thumb = findViewById(thumbId);
            NavHelper.click(this, cellId, () -> {
                Drawable d = thumb.getDrawable();
                if (d != null && preview != null) preview.setImageDrawable(d);
            });
        }
    }
}
