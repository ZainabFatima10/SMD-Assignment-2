package com.example.smd_assignment1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

public class NavHelper {

    public static final int HOME = 0, FRIENDS = 1, MARKET = 2, NOTIF = 3, MENU = 4;

    /** Wires the shared header + 5-tab bar (tabbar_*.xml) used by screens 18, 19 and 23. */
    public static void setup(final Activity a, int selected, String title) {
        ((TextView) a.findViewById(R.id.tbTitle)).setText(title);

        int[] tabs = {R.id.tab0, R.id.tab1, R.id.tab2, R.id.tab3, R.id.tab4};
        int[] icons = {R.id.ic0, R.id.ic1, R.id.ic2, R.id.ic3, R.id.ic4};
        int[] inds = {R.id.ind0, R.id.ind1, R.id.ind2, R.id.ind3, R.id.ind4};

        for (int i = 0; i < 5; i++) {
            ImageView ic = a.findViewById(icons[i]);
            ic.setColorFilter(ContextCompat.getColor(a, i == selected ? R.color.teal : R.color.tab_idle));
            a.findViewById(inds[i]).setVisibility(i == selected ? View.VISIBLE : View.INVISIBLE);

            final int index = i;
            final int sel = selected;
            a.findViewById(tabs[i]).setOnClickListener(v -> {
                if (index == sel) return;
                switch (index) {
                    case HOME: goHome(a); break;
                    case MARKET: go(a, MarketplaceActivity.class); break;
                    case NOTIF: go(a, NotificationsActivity.class); break;
                    case MENU: go(a, MenuActivity.class); break;
                    case FRIENDS: go(a, FriendsActivity.class); break;
                }
            });
        }

        a.findViewById(R.id.btnMessenger).setOnClickListener(v ->
                a.startActivity(new Intent(a, ChatsActivity.class)));
        a.findViewById(R.id.btnSearch).setOnClickListener(v ->
                a.startActivity(new Intent(a, SearchActivity.class)));
    }

    /** Selected / unselected look for the pill chips used on Search and Profile. */
    public static void chip(TextView tv, boolean selected, boolean keepBackgroundWhenIdle) {
        int l = tv.getPaddingLeft(), t = tv.getPaddingTop(), r = tv.getPaddingRight(), b = tv.getPaddingBottom();
        if (selected) {
            tv.setBackgroundResource(R.drawable.lightseagreen_chip);
            tv.setTextColor(ContextCompat.getColor(tv.getContext(), R.color.SeaGreen));
        } else {
            if (keepBackgroundWhenIdle) tv.setBackgroundResource(R.drawable.beige_chip);
            else tv.setBackground(null);
            tv.setTextColor(ContextCompat.getColor(tv.getContext(),
                    keepBackgroundWhenIdle ? R.color.black : R.color.gray));
        }
        tv.setPadding(l, t, r, b);
    }

    /** Switch between top-level tabs without stacking activities. */
    public static void go(Activity a, Class<?> target) {
        a.startActivity(new Intent(a, target));
        a.finish();
    }

    /** Back to the home feed (screen 4), reusing the existing instance if it is still alive. */
    public static void goHome(Activity a) {
        Intent i = new Intent(a, StartSocialMediaActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        a.startActivity(i);
        a.finish();
    }

    /** Null-safe click helper so a missing id can never crash an activity. */
    public static void click(Activity a, int id, Runnable action) {
        View v = a.findViewById(id);
        if (v != null) v.setOnClickListener(x -> action.run());
    }

    public static void longClick(Activity a, int id, Runnable action) {
        View v = a.findViewById(id);
        if (v != null) v.setOnLongClickListener(x -> {
            action.run();
            return true;
        });
    }

    public static void toast(Context c, String msg) {
        Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
    }

    /** Paints an initials avatar (style AvatarText) with the given colour. */
    public static void avatar(TextView tv, String initials, int color) {
        tv.setText(initials);
        tv.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    public static int c(String hex) {
        return Color.parseColor(hex);
    }
}
