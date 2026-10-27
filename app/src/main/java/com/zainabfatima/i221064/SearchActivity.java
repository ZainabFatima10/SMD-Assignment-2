package com.zainabfatima.i221064;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.zainabfatima.i221064.model.User;
import com.zainabfatima.i221064.util.AvatarView;
import com.zainabfatima.i221064.util.Db;
import com.zainabfatima.i221064.util.FriendsHelper;
import com.zainabfatima.i221064.util.RelationTracker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Screen 13 - Search. Finds people by username or name while you type (prefix match on the
 * lower-cased fields in Realtime Database) and lets you send / cancel / confirm friend requests.
 */
public class SearchActivity extends AppCompatActivity {

    private static final String PREFS = "kinnect_search";
    private static final String KEY_RECENT = "recent";

    private final List<User> results = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ResultAdapter adapter;
    private RelationTracker relations;
    private EditText etQuery;
    private View recentSection, clear, noResults;
    private RecyclerView resultList;
    private TextView[] chips;
    private int searchToken = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_screen);

        etQuery = findViewById(R.id.etQuery);
        recentSection = findViewById(R.id.recent_section);
        clear = findViewById(R.id.btn_clear);
        noResults = findViewById(R.id.noResults);
        resultList = findViewById(R.id.resultList);
        resultList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ResultAdapter();
        resultList.setAdapter(adapter);

        relations = new RelationTracker(() -> adapter.notifyDataSetChanged());
        relations.start();

        NavHelper.click(this, R.id.backarrow, this::finish);
        NavHelper.click(this, R.id.btn_clear, () -> etQuery.setText(""));

        chips = new TextView[]{findViewById(R.id.chip_all), findViewById(R.id.chip_people),
                findViewById(R.id.chip_marketplace)};
        NavHelper.click(this, R.id.chip_all, () -> selectChip(0));
        NavHelper.click(this, R.id.chip_people, () -> selectChip(1));
        NavHelper.click(this, R.id.chip_marketplace, () -> startActivity(new Intent(this, MarketplaceActivity.class)));

        etQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                final String q = s.toString().trim().toLowerCase();
                clear.setVisibility(q.isEmpty() ? View.GONE : View.VISIBLE);
                handler.removeCallbacksAndMessages(null);
                if (q.isEmpty()) {
                    searchToken++;
                    showRecents();
                } else {
                    handler.postDelayed(() -> search(q), 250);
                }
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        etQuery.setOnEditorActionListener((v, actionId, e) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                rememberSearch(etQuery.getText().toString().trim());
                return true;
            }
            return false;
        });

        showRecents();
        etQuery.requestFocus();
    }

    private void selectChip(int selected) {
        for (int i = 0; i < chips.length; i++) NavHelper.chip(chips[i], i == selected, true);
    }

    // ------------------------------------------------------------------ search
    private void search(final String q) {
        final int token = ++searchToken;
        final Map<String, User> found = new LinkedHashMap<>();
        final int[] pending = {2};
        final String end = q + "";

        ValueEventListener collect = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                for (DataSnapshot c : s.getChildren()) {
                    User u = c.getValue(User.class);
                    if (u == null) continue;
                    if (u.uid == null) u.uid = c.getKey();
                    if (u.uid.equals(Db.uid())) continue;
                    found.put(u.uid, u);
                }
                if (--pending[0] == 0 && token == searchToken) show(found);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                if (--pending[0] == 0 && token == searchToken) show(found);
            }
        };
        Query byName = Db.users().orderByChild("nameLower").startAt(q).endAt(end).limitToFirst(25);
        Query byUsername = Db.users().orderByChild("usernameLower").startAt(q).endAt(end).limitToFirst(25);
        byName.addListenerForSingleValueEvent(collect);
        byUsername.addListenerForSingleValueEvent(collect);
    }

    private void show(Map<String, User> found) {
        results.clear();
        results.addAll(found.values());
        adapter.notifyDataSetChanged();
        recentSection.setVisibility(View.GONE);
        resultList.setVisibility(results.isEmpty() ? View.GONE : View.VISIBLE);
        noResults.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ------------------------------------------------------------------ recents
    private List<String> recents() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String raw = sp.getString(KEY_RECENT, "");
        List<String> out = new ArrayList<>();
        for (String s : raw.split("\n")) if (!s.trim().isEmpty()) out.add(s.trim());
        return out;
    }

    private void saveRecents(List<String> list) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_RECENT, android.text.TextUtils.join("\n", list)).apply();
    }

    private void rememberSearch(String q) {
        if (q.isEmpty()) return;
        List<String> r = recents();
        r.remove(q);
        r.add(0, q);
        while (r.size() > 6) r.remove(r.size() - 1);
        saveRecents(r);
    }

    private void showRecents() {
        resultList.setVisibility(View.GONE);
        noResults.setVisibility(View.GONE);
        LinearLayout list = findViewById(R.id.recent_list);
        list.removeAllViews();
        final List<String> r = recents();
        recentSection.setVisibility(r.isEmpty() ? View.GONE : View.VISIBLE);
        float d = getResources().getDisplayMetrics().density;
        for (final String q : r) {
            TextView tv = new TextView(this);
            tv.setText(q);
            tv.setTextColor(0xFF000000);
            tv.setTextSize(16);
            tv.setPadding(Math.round(18 * d), Math.round(12 * d), Math.round(18 * d), Math.round(12 * d));
            tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.search_icon_gray, 0, 0, 0);
            tv.setCompoundDrawablePadding(Math.round(12 * d));
            tv.setOnClickListener(v -> etQuery.setText(q));
            tv.setOnLongClickListener(v -> {
                r.remove(q);
                saveRecents(r);
                showRecents();
                return true;
            });
            list.addView(tv);
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (relations != null) relations.stop();
        super.onDestroy();
    }

    // ------------------------------------------------------------------ adapter
    private class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.Holder> {
        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            final User u = results.get(position);
            h.avatar.bind(u);
            h.name.setText(u.displayName());
            h.sub.setText(u.username == null || u.username.isEmpty() ? "" : "@" + u.username);
            final int state = relations.stateOf(u.uid);
            h.action.setText(state == FriendsHelper.RECEIVED ? "Confirm" : FriendsHelper.labelFor(state));
            boolean primary = state == FriendsHelper.NONE || state == FriendsHelper.RECEIVED;
            h.action.setBackgroundResource(primary ? R.drawable.seagreen_button_10 : R.drawable.beige_button_10);
            h.action.setTextColor(primary ? 0xFFFFFFFF : 0xFF000000);
            h.action.setOnClickListener(v -> {
                if (state == FriendsHelper.FRIENDS) {
                    new AlertDialog.Builder(SearchActivity.this)
                            .setTitle("Unfriend " + u.displayName() + "?")
                            .setPositiveButton("Unfriend", (d, w) -> FriendsHelper.unfriend(u.uid))
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    FriendsHelper.onButtonTapped(state, u.uid);
                }
            });
            h.itemView.setOnClickListener(v -> {
                rememberSearch(etQuery.getText().toString().trim());
                startActivity(new Intent(SearchActivity.this, ProfileActivity.class)
                        .putExtra(ProfileActivity.EXTRA_UID, u.uid));
            });
        }

        @Override public int getItemCount() { return results.size(); }

        class Holder extends RecyclerView.ViewHolder {
            final AvatarView avatar;
            final TextView name, sub, action;
            Holder(View v) {
                super(v);
                avatar = v.findViewById(R.id.userAvatar);
                name = v.findViewById(R.id.userName);
                sub = v.findViewById(R.id.userSub);
                action = v.findViewById(R.id.userAction);
            }
        }
    }
}
