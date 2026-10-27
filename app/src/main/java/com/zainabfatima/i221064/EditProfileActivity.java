package com.zainabfatima.i221064;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.model.User;
import com.zainabfatima.i221064.util.Async;
import com.zainabfatima.i221064.util.AvatarView;
import com.zainabfatima.i221064.util.Db;
import com.zainabfatima.i221064.util.ImageUtil;
import com.zainabfatima.i221064.util.RecentLogin;
import com.zainabfatima.i221064.util.UserCache;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Screen 16 - Edit profile. Also the first-time "profile setup" step: new accounts must choose a
 * unique username (and bio) here before they reach Home.
 */
public class EditProfileActivity extends AppCompatActivity {

    public static final String EXTRA_SETUP = "setup";

    private static final String USERNAME_PATTERN = "[a-z0-9._]{3,20}";

    private boolean setupMode;
    private User me;
    private String originalUsernameLower = "";

    private EditText etName, etUsername, etBio;
    private TextView tvUsernameStatus, tvGender;
    private AvatarView avatar;
    private ImageView cover;
    private View progress;

    private Uri newProfileUri, newCoverUri;
    private boolean usernameOk = false;
    private String checkedFor = "";
    private final Handler debounce = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<PickVisualMediaRequest> pickProfile =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri == null) return;
                newProfileUri = uri;
                Async.run(() -> ImageUtil.uriToBase64(this, uri, ImageUtil.PROFILE_DIM, 80), (b64, err) -> {
                    if (b64 != null) avatar.bind(etName.getText().toString(), b64, me != null ? me.uid : "x");
                });
            });

    private final ActivityResultLauncher<PickVisualMediaRequest> pickCover =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri == null) return;
                newCoverUri = uri;
                Async.run(() -> ImageUtil.uriToBase64(this, uri, ImageUtil.COVER_DIM, 70), (b64, err) -> {
                    if (b64 != null) ImageUtil.show(cover, b64);
                });
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_profile_screen);

        setupMode = getIntent().getBooleanExtra(EXTRA_SETUP, false);

        etName = findViewById(R.id.etName);
        etUsername = findViewById(R.id.etUsername);
        etBio = findViewById(R.id.etBio);
        tvUsernameStatus = findViewById(R.id.tvUsernameStatus);
        tvGender = findViewById(R.id.tvGender);
        avatar = findViewById(R.id.editAvatar);
        cover = findViewById(R.id.coverImage);
        progress = findViewById(R.id.progress);

        if (setupMode) {
            ((TextView) findViewById(R.id.tvTitle)).setText("Set up your profile");
            findViewById(R.id.tvSetupHint).setVisibility(View.VISIBLE);
            findViewById(R.id.btn_cancel).setVisibility(View.INVISIBLE);   // cannot skip setup
            ((TextView) findViewById(R.id.btn_save)).setText("Done");
            // back would leave a half-made account on screen; keep the user in setup
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override public void handleOnBackPressed() {
                    NavHelper.toast(EditProfileActivity.this, "Choose a username to continue");
                }
            });
        } else {
            NavHelper.click(this, R.id.btn_cancel, this::finish);
        }

        NavHelper.click(this, R.id.btn_save, this::save);
        NavHelper.click(this, R.id.btn_edit_picture, () -> pickProfile.launch(imageOnly()));
        NavHelper.click(this, R.id.editAvatar, () -> pickProfile.launch(imageOnly()));
        NavHelper.click(this, R.id.btn_edit_cover, () -> pickCover.launch(imageOnly()));
        NavHelper.click(this, R.id.btn_cover_camera, () -> pickCover.launch(imageOnly()));

        tvGender.setOnClickListener(v -> {
            final String[] options = {"Female", "Male", "Custom"};
            new AlertDialog.Builder(this)
                    .setTitle("Gender")
                    .setItems(options, (d, which) -> tvGender.setText(options[which]))
                    .show();
        });

        etUsername.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                usernameOk = false;
                debounce.removeCallbacksAndMessages(null);
                final String typed = s.toString().trim().toLowerCase(Locale.ROOT);
                if (typed.isEmpty()) {
                    status("3-20 letters, numbers, dots or underscores", 0xFF8B898E);
                } else if (!typed.matches(USERNAME_PATTERN)) {
                    status("Use 3-20 letters, numbers, dots or underscores", 0xFFBA4929);
                } else {
                    status("Checking...", 0xFF8B898E);
                    debounce.postDelayed(() -> checkAvailability(typed), 350);
                }
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        loadProfile();
    }

    private PickVisualMediaRequest imageOnly() {
        return new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build();
    }

    private void loadProfile() {
        String uid = Db.uid();
        if (uid == null) return;
        Db.user(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                me = s.getValue(User.class);
                if (me == null) me = new User();
                me.uid = uid;
                etName.setText(me.name);
                etBio.setText(me.bio);
                ((TextView) findViewById(R.id.tvEmail)).setText(me.email);
                if (me.gender != null && !me.gender.isEmpty()) tvGender.setText(me.gender);
                if (me.username != null) {
                    originalUsernameLower = me.username.toLowerCase(Locale.ROOT);
                    etUsername.setText(me.username);
                }
                avatar.bind(me);
                ImageUtil.show(cover, me.coverPic);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        });
    }

    private void status(String msg, int color) {
        tvUsernameStatus.setText(msg);
        tvUsernameStatus.setTextColor(color);
    }

    private void checkAvailability(final String lower) {
        checkedFor = lower;
        Db.usernames().child(lower).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                if (!lower.equals(checkedFor)) return;   // user kept typing
                String owner = s.getValue(String.class);
                boolean free = owner == null || owner.equals(Db.uid());
                usernameOk = free;
                status(free ? "@" + lower + " is available" : "@" + lower + " is already taken",
                        free ? 0xFF2F6B4F : 0xFFBA4929);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                status("Could not check the username", 0xFFBA4929);
            }
        });
    }

    private void save() {
        final String uid = Db.uid();
        if (uid == null || me == null) return;
        final String name = etName.getText().toString().trim();
        final String username = etUsername.getText().toString().trim();
        final String lower = username.toLowerCase(Locale.ROOT);
        final String bio = etBio.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Enter your name");
            return;
        }
        if (!lower.matches(USERNAME_PATTERN)) {
            etUsername.setError("Use 3-20 letters, numbers, dots or underscores");
            return;
        }
        if (!usernameOk && !lower.equals(originalUsernameLower)) {
            NavHelper.toast(this, "Pick an available username first");
            return;
        }

        setBusy(true);
        // 1) encode any new pictures off the main thread
        Async.run(() -> {
            String[] pics = new String[2];
            if (newProfileUri != null) pics[0] = ImageUtil.uriToBase64(this, newProfileUri, ImageUtil.PROFILE_DIM, 80);
            if (newCoverUri != null) pics[1] = ImageUtil.uriToBase64(this, newCoverUri, ImageUtil.COVER_DIM, 70);
            return pics;
        }, (pics, err) -> {
            if (err != null || pics == null) {
                setBusy(false);
                NavHelper.toast(this, "Could not read the chosen photo");
                return;
            }
            // 2) claim the username atomically so two people can never share it
            claimUsername(uid, lower, ok -> {
                if (!ok) {
                    setBusy(false);
                    usernameOk = false;
                    status("@" + lower + " is already taken", 0xFFBA4929);
                    return;
                }
                writeProfile(uid, name, username, lower, bio, pics[0], pics[1]);
            });
        });
    }

    private interface BoolCallback { void done(boolean ok); }

    private void claimUsername(final String uid, final String lower, final BoolCallback cb) {
        Db.usernames().child(lower).runTransaction(new Transaction.Handler() {
            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData d) {
                String cur = d.getValue(String.class);
                if (cur == null || cur.equals(uid)) {
                    d.setValue(uid);
                    return Transaction.success(d);
                }
                return Transaction.abort();
            }
            @Override public void onComplete(DatabaseError error, boolean committed, DataSnapshot snap) {
                cb.done(error == null && committed);
            }
        });
    }

    private void writeProfile(String uid, String name, String username, String lower, String bio,
                              String profilePic, String coverPic) {
        Map<String, Object> up = new HashMap<>();
        up.put("name", name);
        up.put("nameLower", name.toLowerCase(Locale.ROOT));
        up.put("username", username);
        up.put("usernameLower", lower);
        up.put("bio", bio);
        up.put("gender", tvGender.getText().toString());
        up.put("profileComplete", true);
        if (profilePic != null) up.put("profilePic", profilePic);
        if (coverPic != null) up.put("coverPic", coverPic);

        Db.user(uid).updateChildren(up).addOnSuccessListener(x -> {
            // release the old username so somebody else can use it
            if (!originalUsernameLower.isEmpty() && !originalUsernameLower.equals(lower)) {
                Db.usernames().child(originalUsernameLower).removeValue();
            }
            me.name = name;
            me.username = username;
            me.bio = bio;
            if (profilePic != null) me.profilePic = profilePic;
            UserCache.put(me);
            RecentLogin.save(this, me, me.email);
            setBusy(false);
            if (setupMode) {
                Intent i = new Intent(this, StartSocialMediaActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            } else {
                NavHelper.toast(this, "Profile saved");
            }
            finish();
        }).addOnFailureListener(e -> {
            setBusy(false);
            NavHelper.toast(this, "Could not save: " + e.getMessage());
        });
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        findViewById(R.id.btn_save).setEnabled(!busy);
    }
}
