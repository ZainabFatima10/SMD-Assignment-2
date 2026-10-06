package com.example.smd_assignment1;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/** Screen 16 - Edit profile. Fields are TextViews in the design, so tapping one opens a small edit dialog. */
public class EditProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_profile_screen);

        NavHelper.click(this, R.id.btn_cancel, this::finish);
        NavHelper.click(this, R.id.btn_save, () -> {
            NavHelper.toast(this, "Profile saved");
            finish();
        });

        // profile picture / cover photo -> pick a photo (screen 8)
        Runnable pickPhoto = () -> startActivity(new Intent(this, PictureSendActivity.class));
        NavHelper.click(this, R.id.btn_edit_picture, pickPhoto);
        NavHelper.click(this, R.id.btn_edit_cover, pickPhoto);
        NavHelper.click(this, R.id.btn_cover_camera, pickPhoto);

        NavHelper.click(this, R.id.btn_professional, () -> NavHelper.toast(this, "Professional mode"));

        field(R.id.field_name, "Name");
        field(R.id.field_username, "Username");
        field(R.id.field_website, "Website");
        field(R.id.field_bio, "Bio");
        field(R.id.field_email, "Email");
        field(R.id.field_phone, "Phone");
        field(R.id.field_gender, "Gender");
    }

    private void field(int id, final String label) {
        final TextView tv = findViewById(id);
        tv.setOnClickListener(v -> {
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setText(tv.getText());
            input.setSelection(input.getText().length());
            new AlertDialog.Builder(this)
                    .setTitle(label)
                    .setView(input)
                    .setPositiveButton("OK", (d, w) -> tv.setText(input.getText().toString()))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }
}
