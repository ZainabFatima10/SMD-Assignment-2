package com.zainabfatima.i221064;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.model.User;
import com.zainabfatima.i221064.util.AuthErrors;
import com.zainabfatima.i221064.util.AvatarView;
import com.zainabfatima.i221064.util.Db;
import com.zainabfatima.i221064.util.RecentLogin;
import com.zainabfatima.i221064.util.Session;

import androidx.annotation.NonNull;

/** Screen 2 - Log in (Firebase email + password). */
public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private TextView tvError;
    private View progress, btnLogin;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_screen);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvError = findViewById(R.id.tvError);
        progress = findViewById(R.id.progress);
        btnLogin = findViewById(R.id.button);

        // ---- Recent login card: the last account that signed in on this device ----
        final RecentLogin.Account recent = RecentLogin.load(this);
        if (recent != null) {
            findViewById(R.id.recentSection).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.textView)).setText(recent.name);
            ((TextView) findViewById(R.id.tvRecentEmail)).setText(recent.email);
            ((AvatarView) findViewById(R.id.recentAvatar)).bind(recent.name, recent.pic, recent.uid);
            // tapping the card pre-fills the email and puts the cursor on the password
            NavHelper.click(this, R.id.cardView2, () -> {
                etEmail.setText(recent.email);
                etPassword.requestFocus();
            });
        }

        final ImageView reveal = findViewById(R.id.btnReveal);
        reveal.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            etPassword.setInputType(passwordVisible
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            etPassword.setSelection(etPassword.getText().length());
        });

        btnLogin.setOnClickListener(v -> attemptLogin());
        NavHelper.click(this, R.id.button2, () -> startActivity(new Intent(this, CreateAccountActivity.class)));
        NavHelper.click(this, R.id.text4, this::resetPassword);
    }

    private void attemptLogin() {
        final String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        if (email.isEmpty() || password.isEmpty()) {
            showError("Enter your email and password.");
            return;
        }
        setBusy(true);
        Db.auth().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(r -> onSignedIn(email))
                .addOnFailureListener(e -> {
                    setBusy(false);
                    showError(AuthErrors.message(e));
                });
    }

    /** Loads the profile; first-time users (no username yet) are sent to profile setup before Home. */
    private void onSignedIn(final String email) {
        final String uid = Db.uid();
        if (uid == null) {
            setBusy(false);
            return;
        }
        Session.onLogin(this);
        Db.user(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                User u = s.getValue(User.class);
                if (u == null) {
                    // account exists in Authentication but its profile was never written: create it
                    u = SignupDefaults.createMissingProfile(uid, email);
                }
                u.uid = uid;
                RecentLogin.save(LoginActivity.this, u, email);
                Intent i = new Intent(LoginActivity.this,
                        u.profileComplete ? StartSocialMediaActivity.class : EditProfileActivity.class);
                if (!u.profileComplete) i.putExtra(EditProfileActivity.EXTRA_SETUP, true);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                setBusy(false);
                showError(e.getMessage());
            }
        });
    }

    private void resetPassword() {
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            showError("Type your email above, then tap Forget Password.");
            return;
        }
        Db.auth().sendPasswordResetEmail(email)
                .addOnSuccessListener(x -> NavHelper.toast(this, "Password reset email sent"))
                .addOnFailureListener(e -> showError(AuthErrors.message(e)));
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!busy);
        if (busy) tvError.setVisibility(View.GONE);
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }
}
