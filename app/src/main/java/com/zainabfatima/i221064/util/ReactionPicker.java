package com.zainabfatima.i221064.util;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.zainabfatima.i221064.R;

/**
 * The six-reaction picker of screen 05 (Like, Love, Haha, Wow, Sad, Angry), shown above the Like button.
 * The artwork is the picker image from the design; a tap is mapped onto one of its six icons.
 */
public final class ReactionPicker {

    public interface OnPicked {
        void onPicked(int type);
    }

    // x positions (fraction of the image width) where one icon ends and the next begins
    private static final float[] BOUNDS = {0.188f, 0.358f, 0.518f, 0.667f, 0.817f};

    private ReactionPicker() { }

    public static void show(View anchor, final OnPicked callback) {
        final Context c = anchor.getContext();
        final float d = c.getResources().getDisplayMetrics().density;
        int w = Math.round(310 * d);
        int h = Math.round(w * 173f / 589f);
        int tipH = Math.round(26 * d);

        FrameLayout root = new FrameLayout(c);
        final ImageView picker = new ImageView(c);
        picker.setImageResource(R.drawable.reactions);
        picker.setScaleType(ImageView.ScaleType.FIT_XY);
        root.addView(picker, new FrameLayout.LayoutParams(w, h, Gravity.BOTTOM));

        // small label that follows the finger ("Love")
        final TextView tip = new TextView(c);
        tip.setBackgroundResource(R.drawable.reaction_description);
        tip.setTextColor(0xFFFFFFFF);
        tip.setTextSize(11);
        tip.setPadding(Math.round(8 * d), Math.round(2 * d), Math.round(8 * d), Math.round(2 * d));
        tip.setVisibility(View.INVISIBLE);
        FrameLayout.LayoutParams tl = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        root.addView(tip, tl);

        final PopupWindow pw = new PopupWindow(root, w, h + tipH, true);
        pw.setBackgroundDrawable(new ColorDrawable(0));
        pw.setOutsideTouchable(true);
        pw.setElevation(12 * d);

        final int width = w;
        picker.setOnTouchListener((v, e) -> {
            float f = Math.max(0f, Math.min(0.999f, e.getX() / (float) width));
            int type = 0;
            while (type < BOUNDS.length && f > BOUNDS[type]) type++;
            int action = e.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                tip.setText(Reactions.name(type));
                tip.setVisibility(View.VISIBLE);
                tip.setTranslationX(Math.max(0f, Math.min(width - tip.getWidth(), e.getX() - tip.getWidth() / 2f)));
                return true;
            }
            if (action == MotionEvent.ACTION_UP) {
                pw.dismiss();
                callback.onPicked(type);
                return true;
            }
            if (action == MotionEvent.ACTION_CANCEL) {
                tip.setVisibility(View.INVISIBLE);
                return true;
            }
            return true;
        });

        pw.showAsDropDown(anchor, 0, -(anchor.getHeight() + h + tipH));
    }
}
