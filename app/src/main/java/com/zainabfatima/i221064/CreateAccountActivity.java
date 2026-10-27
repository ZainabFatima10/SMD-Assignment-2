package com.zainabfatima.i221064;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.zainabfatima.i221064.model.User;
import com.zainabfatima.i221064.util.AuthErrors;
import com.zainabfatima.i221064.util.Clock;
import com.zainabfatima.i221064.util.Db;
import com.zainabfatima.i221064.util.PasswordStrength;
import com.zainabfatima.i221064.util.RecentLogin;
import com.zainabfatima.i221064.util.Session;

import java.util.Calendar;
import java.util.Locale;

/**
 * Screen 3 - Create account. Creates the Firebase Authentication user, saves the profile to the
 * Realtime Database (/users/{uid}), then sends the new user to profile setup (screen 16).
 */
public class CreateAccountActivity extends AppCompatActivity {

    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    private boolean agreed = true;
    private boolean passwordVisible = false;
    private String gender = "Male";
    private int day = 14, month = 2, year = 2004;   // month is 0-based

    private EditText etFirst, etLast, etEmail, etPassword;
    private TextView tvError;
    private View progress, btnCreate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_account_screen);

        etFirst = findViewById(R.id.etFirstName);
        etLast = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvError = findViewById(R.id.tvError);
        progress = findViewById(R.id.progress);
        btnCreate = findViewById(R.id.button2);

        NavHelper.click(this, R.id.backbutton, this::finish);
        NavHelper.click(this, R.id.loginLink, this::finish);   // "Already have an account? Log in"

        // Terms checkbox: dim it when unchecked
        final View checkbox = findViewById(R.id.checkbox);
        NavHelper.click(this, R.id.checkbox, () -> {
            agreed = !agreed;
            checkbox.setAlpha(agreed ? 1f : 0.35f);
        });

        // Birthday: any of the three cards opens one date picker
        refreshBirthday();
        Runnable pickDate = () -> {
            DatePickerDialog d = new DatePickerDialog(this, (picker, y, m, dd) -> {
                year = y;
                month = m;
                day = dd;
                refreshBirthday();
            }, year, month, day);
            d.getDatePicker().setMaxDate(System.currentTimeMillis());
            d.show();
        };
        NavHelper.click(this, R.id.DayView3RL, pickDate);
        NavHelper.click(this, R.id.MonthView3RL, pickDate);
        NavHelper.click(this, R.id.YearView3RL, pickDate);

        // Gender
        NavHelper.click(this, R.id.FemaleChoice, () -> selectGender("Female"));
        NavHelper.click(this, R.id.MaleChoice, () -> selectGender("Male"));
        NavHelper.click(this, R.id.CustomChoice, () -> selectGender("Custom"));
        selectGender("Male");

        // Show / hide password
        NavHelper.click(this, R.id.btnReveal, () -> {
            passwordVisible = !passwordVisible;
            etPassword.setInputType(passwordVisible
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            etPassword.setSelection(etPassword.getText().length());
        });

        // Password strength updates as the user types
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                showStrength(PasswordStrength.of(s.toString()));
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        btnCreate.setOnClickListener(v -> attemptSignup());
    }

    private void refreshBirthday() {
        ((TextView) findViewById(R.id.tvDay)).setText(String.valueOf(day));
        ((TextView) findViewById(R.id.tvMonth)).setText(MONTHS[month]);
        ((TextView) findViewById(R.id.tvYear)).setText(String.valueOf(year));
    }

    private void selectGender(String g) {
        gender = g;
        setChoice(R.id.FemaleChoice, R.id.dotFemale, R.id.tvFemale, g.equals("Female"));
        setChoice(R.id.MaleChoice, R.id.dotMale, R.id.tvMale, g.equals("Male"));
        setChoice(R.id.CustomChoice, R.id.dotCustom, R.id.tvCustom, g.equals("Custom"));
    }

    private void setChoice(int cardId, int dotId, int labelId, boolean selected) {
        CardView card = findViewById(cardId);
        card.setForeground(selected ? getDrawable(R.drawable.selected_choice_border) : null);
        findViewById(dotId).setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        ((TextView) findViewById(labelId)).setTypeface(null, selected
                ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void showStrength(int level) {
        View box = findViewById(R.id.password_strength_indicator);
        box.setVisibility(level == PasswordStrength.NONE ? View.INVISIBLE : View.VISIBLE);
        int[] bars = {R.id.strengthBar1, R.id.strengthBar2, R.id.strengthBar3};
        for (int i = 0; i < bars.length; i++) {
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(4f);
            g.setColor(i < level ? PasswordStrength.color(level) : PasswordStrength.color(PasswordStrength.NONE));
            findViewById(bars[i]).setBackground(g);
        }
        TextView label = findViewById(R.id.tvStrength);
        label.setText(PasswordStrength.label(level));
        label.setTextColor(PasswordStrength.color(level));
    }

    private void attemptSignup() {
        final String first = etFirst.getText().toString().trim();
        final String last = etLast.getText().toString().trim();
        final String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (first.isEmpty()) { showError("Enter your first name."); return; }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Enter a valid email address.");
            return;
        }
        if (password.length() < 6) { showError("Your password needs at least 6 characters."); return; }
        if (!agreed) { showError("Please agree to the Terms and Privacy Policy."); return; }

        setBusy(true);
        final String fullName = last.isEmpty() ? first : first + " " + last;
        Db.auth().createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser() == null ? null : result.getUser().getUid();
                    if (uid == null) {
                        setBusy(false);
                        showError("Could not create the account.");
                        return;
                    }
                    saveProfile(uid, fullName, email);
                })
                .addOnFailureListener(e -> {
                    setBusy(false);
                    showError(AuthErrors.message(e));
                });
    }

    /** Sign-up must save the user's info to the Realtime Database. */
    private void saveProfile(final String uid, String fullName, final String email) {
        final User u = new User();
        u.uid = uid;
        u.name = fullName;
        u.nameLower = fullName.toLowerCase(Locale.ROOT);
        u.email = email;
        u.gender = gender;
        u.birthday = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
        u.bio = "";
        u.online = false;
        u.profileComplete = false;     // username + bio are chosen on the next step
        u.createdAt = Clock.now();

        Db.user(uid).setValue(u)
                .addOnSuccessListener(x -> {
                    RecentLogin.save(this, u, email);
                    Session.onLogin(this);
                    // first-time users choose a unique username and bio before reaching Home
                    Intent i = new Intent(this, EditProfileActivity.class);
                    i.putExtra(EditProfileActivity.EXTRA_SETUP, true);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setBusy(false);
                    showError("Account created but saving your profile failed: " + e.getMessage());
                });
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        btnCreate.setEnabled(!busy);
        if (busy) tvError.setVisibility(View.GONE);
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }
}
