package com.Veibai.VNable.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.Veibai.VNable.R;
import com.Veibai.VNable.util.ThemeColors;

public class SoftSwitch extends View {

    public interface OnCheckedChangeListener {
        void onCheckedChanged(SoftSwitch view, boolean isChecked);
    }

    private static final float TRACK_W_DP = 46f;
    private static final float TRACK_H_DP = 27f;
    private static final float THUMB_DP = 21f;
    private static final float STROKE_DP = 1f;
    private static final long ANIM_MS = 180L;

    private boolean checked = false;
    private boolean animated = true;
    private float position = 0f;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbShade = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final GradientDrawable trackDrawable = new GradientDrawable();

    private ValueAnimator animator;
    private OnCheckedChangeListener listener;

    public SoftSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClickable(true);
        setFocusable(true);
        strokePaint.setStyle(Paint.Style.STROKE);
        thumbShade.setStyle(Paint.Style.FILL);
        applyColors();
        setOnClickListener(v -> setChecked(!checked, true));
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean value) {
        setChecked(value, false);
    }

    public void setChecked(boolean value, boolean fromUser) {
        if (checked == value) return;
        checked = value;
        if (listener != null && fromUser) listener.onCheckedChanged(this, checked);
        moveTo(checked ? 1f : 0f);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener l) {
        listener = l;
    }

    private void moveTo(float target) {
        if (animator != null) animator.cancel();
        if (!animated) {
            position = target;
            applyColors();
            invalidate();
            return;
        }
        animator = ValueAnimator.ofFloat(position, target);
        animator.setDuration(ANIM_MS);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            position = (float) a.getAnimatedValue();
            applyColors();
            invalidate();
        });
        animator.start();
    }

    private void applyColors() {
        int on = ThemeColors.resolve(getContext(), R.attr.vnAccent);
        int trackOff = ThemeColors.resolve(getContext(), R.attr.colorSurfaceContainerHigh);
        int strokeOff = ThemeColors.resolve(getContext(), R.attr.vnStrokeSoft);
        int thumbOff = ThemeColors.resolve(getContext(), R.attr.colorOnSurface);
        int thumbOn = ThemeColors.resolve(getContext(), R.attr.colorOnPrimary);

        int track = blend(trackOff, on, position);
        trackPaint.setColor(track);
        strokePaint.setColor(blend(strokeOff, 0x33000000, position));

        thumbPaint.setColor(blend(thumbOff, thumbOn, position));
        thumbShade.setColor(0x14000000);
        trackPaint.setAlpha(255);
    }

    private static int blend(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float density = getResources().getDisplayMetrics().density;
        int w = (int) (TRACK_W_DP * density);
        int h = (int) (TRACK_H_DP * density);
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float density = getResources().getDisplayMetrics().density;
        float stroke = STROKE_DP * density;
        float w = getWidth();
        float h = getHeight();
        float radius = h / 2f;

        trackDrawable.setShape(GradientDrawable.RECTANGLE);
        trackDrawable.setCornerRadius(radius);
        trackDrawable.setColor(trackPaint.getColor());
        trackDrawable.setStroke((int) stroke, strokePaint.getColor());
        trackDrawable.setBounds(0, 0, (int) w, (int) h);
        trackDrawable.draw(canvas);

        float thumb = THUMB_DP * density;
        float pad = (h - thumb) / 2f;
        float maxTravel = w - thumb - pad;
        float cx = pad + thumb / 2f + maxTravel * position;
        float cy = h / 2f;

        canvas.drawCircle(cx, cy + 1.2f * density, thumb / 2f, thumbShade);
        canvas.drawCircle(cx, cy, thumb / 2f, thumbPaint);
    }
}
