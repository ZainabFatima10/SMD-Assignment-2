package com.example.smd_assignment1;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Screen 20 - Chats (all rows are declared in activity_chats.xml). */
public class ChatsActivity extends AppCompatActivity {

    // row id, name view id, display name, initials, colour
    private static final Object[][] CHATS = {
            {R.id.c1, R.id.c1_name, "Aisha Khan", "AK", "#A23B52"},
            {R.id.c2, R.id.c2_name, "Design crew", "DC", "#6B5B2B"},
            {R.id.c3, R.id.c3_name, "Lina Marsh", "LM", "#7B4F7F"},
            {R.id.c4, R.id.c4_name, "Omar Farooq", "OF", "#2F6E4E"},
            {R.id.c5, R.id.c5_name, "Bilal Ahmed", "BA", "#7B4F7F"},
            {R.id.c6, R.id.c6_name, "Noor Fatima", "NF", "#2F6E4E"},
    };

    // active-now bubbles
    private static final Object[][] ACTIVE = {
            {R.id.a1, "Aisha Khan", "AK", "#A23B52"},
            {R.id.a2, "Omar Farooq", "OF", "#2F6E4E"},
            {R.id.a3, "Sara Iqbal", "SI", "#4B4F93"},
            {R.id.a4, "Hamza Ali", "HA", "#8B5A2B"},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chats);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCompose).setOnClickListener(v -> NavHelper.toast(this, "New message"));
        findViewById(R.id.navPeople).setOnClickListener(v -> NavHelper.toast(this, "People"));
        findViewById(R.id.navStories).setOnClickListener(v -> NavHelper.toast(this, "Stories"));
        findViewById(R.id.a0).setOnClickListener(v -> NavHelper.toast(this, "Add a note"));

        for (Object[] a : ACTIVE) {
            final String name = (String) a[1], ini = (String) a[2], col = (String) a[3];
            findViewById((Integer) a[0]).setOnClickListener(v -> openChat(name, ini, col));
        }
        for (Object[] c : CHATS) {
            final String name = (String) c[2], ini = (String) c[3], col = (String) c[4];
            findViewById((Integer) c[0]).setOnClickListener(v -> openChat(name, ini, col));
        }

        ((EditText) findViewById(R.id.etSearch)).addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                String q = s.toString().trim().toLowerCase();
                for (Object[] row : CHATS) {
                    String name = ((TextView) findViewById((Integer) row[1])).getText().toString();
                    findViewById((Integer) row[0]).setVisibility(
                            name.toLowerCase().contains(q) ? View.VISIBLE : View.GONE);
                }
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void openChat(String name, String initials, String color) {
        Intent i = new Intent(this, ChatActivity.class);
        i.putExtra("name", name);
        i.putExtra("initials", initials);
        i.putExtra("color", color);
        startActivity(i);
    }
}
