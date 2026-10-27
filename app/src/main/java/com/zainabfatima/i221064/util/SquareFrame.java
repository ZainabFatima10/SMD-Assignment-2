package com.zainabfatima.i221064.util;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/** A FrameLayout that is always as tall as it is wide (gallery cells). */
public class SquareFrame extends FrameLayout {

    public SquareFrame(Context c) {
        super(c);
    }

    public SquareFrame(Context c, AttributeSet a) {
        super(c, a);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, widthSpec);
    }
}
