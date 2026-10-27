package com.zainabfatima.i221064.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.zainabfatima.i221064.model.User;

/** Round avatar: the profile picture when there is one, otherwise coloured initials (as in the design). */
public class AvatarView extends FrameLayout {

    private static final int[] PALETTE = {
            0xFFA23B52, 0xFF7B4F7F, 0xFF4B4F93, 0xFF2F6E4E,
            0xFF8B5A2B, 0xFF3F6E8C, 0xFF6B5B2B, 0xFF0B5F61};

    private final ImageView image;
    private final TextView initials;
    private final GradientDrawable bg = new GradientDrawable();

    public AvatarView(Context c) {
        this(c, null);
    }

    public AvatarView(Context c, @Nullable AttributeSet attrs) {
        super(c, attrs);
        bg.setShape(GradientDrawable.OVAL);
        setBackground(bg);
        setClipToOutline(true);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setOval(0, 0, v.getWidth(), v.getHeight());
            }
        });

        image = new ImageView(c);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        addView(image, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        initials = new TextView(c);
        initials.setGravity(Gravity.CENTER);
        initials.setTextColor(Color.WHITE);
        initials.setTypeface(initials.getTypeface(), android.graphics.Typeface.BOLD);
        addView(initials, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        initials.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, Math.min(w, h) * 0.38f);
    }

    public void bind(@Nullable User u) {
        if (u == null) {
            bind("?", null, "?");
            return;
        }
        bind(u.displayName(), u.profilePic, u.uid == null ? u.displayName() : u.uid);
    }

    public void bind(String name, @Nullable String picBase64, String seed) {
        bg.setColor(PALETTE[Math.abs(seed == null ? 0 : seed.hashCode()) % PALETTE.length]);
        if (picBase64 != null && !picBase64.isEmpty()) {
            initials.setVisibility(GONE);
            image.setVisibility(VISIBLE);
            ImageUtil.show(image, picBase64);
        } else {
            image.setVisibility(GONE);
            ImageUtil.show(image, null);
            initials.setVisibility(VISIBLE);
            initials.setText(initialsOf(name));
        }
    }

    public static String initialsOf(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        String s = parts[0].substring(0, 1);
        if (parts.length > 1) s += parts[parts.length - 1].substring(0, 1);
        return s.toUpperCase();
    }
}
