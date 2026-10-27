package com.zainabfatima.i221064.util;

import com.zainabfatima.i221064.R;

/** The six reactions of the reaction picker (screen 05), in the order they appear in the picker. */
public final class Reactions {

    public static final int LIKE = 0, LOVE = 1, HAHA = 2, WOW = 3, SAD = 4, ANGRY = 5;
    public static final int COUNT = 6;

    public static final String[] NAMES = {"Like", "Love", "Haha", "Wow", "Sad", "Angry"};
    public static final int[] COLORS = {0xFF0B5F63, 0xFFD13A55, 0xFFC98A12, 0xFFC98A12, 0xFFC98A12, 0xFFBC4A28};
    public static final int[] ICONS = {
            R.drawable.react_like, R.drawable.react_love, R.drawable.react_haha,
            R.drawable.react_wow, R.drawable.react_sad, R.drawable.react_angry};

    private Reactions() { }

    public static String name(int type) {
        return type >= 0 && type < COUNT ? NAMES[type] : "Like";
    }

    public static int color(int type) {
        return type >= 0 && type < COUNT ? COLORS[type] : 0xFF8B898E;
    }

    public static int icon(int type) {
        return type >= 0 && type < COUNT ? ICONS[type] : ICONS[0];
    }
}
