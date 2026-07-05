package com.limelight.customcontrols;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/**
 * A single touchable on-screen button that sends input via {@link MoonInputBridge}.
 *
 * <p>Supports:
 * <ul>
 *   <li>Multiple keycodes fired simultaneously.</li>
 *   <li>Toggle mode (first touch = press; second touch = release).</li>
 *   <li>Visual highlight when pressed.</li>
 *   <li>Drag-to-move in edit mode (controlled by the parent overlay).</li>
 * </ul>
 */
@SuppressLint("ViewConstructor")
public class MoonButton extends View {

    private static final float TEXT_SIZE_FRACTION = 0.30f;  // of button height

    // ─── Data & bridge ────────────────────────────────────────────────────────
    private ControlButtonData mData;
    private final MoonInputBridge mBridge;

    // ─── State ────────────────────────────────────────────────────────────────
    private boolean mPressed  = false;
    private boolean mToggled  = false;
    /** Pointer id that started the current press (-1 = none). */
    private int     mPointerId = -1;

    // ─── Drawing ─────────────────────────────────────────────────────────────
    private final Paint mBgPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mRectF     = new RectF();

    // ─── Constructor ─────────────────────────────────────────────────────────

    public MoonButton(Context context, ControlButtonData data, MoonInputBridge bridge) {
        super(context);
        mBridge = bridge;
        setData(data);
        setFocusable(false);
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    public ControlButtonData getData() { return mData; }

    public void setData(ControlButtonData data) {
        mData = data;
        mBgPaint.setColor(mData.bgColor);
        mBgPaint.setAlpha(Math.round(Color.alpha(mData.bgColor) * mData.opacity));
        mTextPaint.setColor(mData.fgColor);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(1); // will be recalculated on draw
        invalidate();
    }

    /** Whether the overlay is in edit mode (touch = drag, not press). */
    private boolean mEditMode = false;

    public void setEditMode(boolean edit) {
        mEditMode = edit;
        if (edit && mPressed) releaseButton();
    }

    // ─── Touch handling ──────────────────────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mEditMode) return false; // let parent handle drag

        final int action = event.getActionMasked();
        final int actionIdx = event.getActionIndex();
        final int pointerId = event.getPointerId(actionIdx);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (mPointerId == -1) {
                    mPointerId = pointerId;
                    pressButton();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (pointerId == mPointerId) {
                    mPointerId = -1;
                    if (!mData.toggle) {
                        releaseButton();
                    } else {
                        // Toggle: alternating press / release on each tap
                        if (mToggled) {
                            mToggled = false;
                            releaseButton();
                        } else {
                            mToggled = true;
                            // keep pressed visual & input state
                        }
                    }
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                mPointerId = -1;
                if (mData.toggle) mToggled = false;
                releaseButton();
                return true;
        }
        return false;
    }

    private void pressButton() {
        mPressed = true;
        for (int kc : mData.keycodes) mBridge.sendButton(kc, true);
        invalidate();
    }

    private void releaseButton() {
        mPressed = false;
        for (int kc : mData.keycodes) mBridge.sendButton(kc, false);
        invalidate();
    }

    // ─── Drawing ─────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        // Background
        float radius = mData.cornerRadius * Math.min(w, h) / 2f;
        boolean highlighted = mPressed || mToggled;
        if (highlighted) {
            mBgPaint.setColor(blendHighlight(mData.bgColor));
        } else {
            mBgPaint.setColor(mData.bgColor);
        }
        mBgPaint.setAlpha(Math.round(Color.alpha(mData.bgColor) * mData.opacity));

        mRectF.set(0, 0, w, h);
        canvas.drawRoundRect(mRectF, radius, radius, mBgPaint);

        // Stroke in edit mode
        if (mEditMode) {
            Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setColor(0xFFFF6600);
            stroke.setStrokeWidth(3f);
            canvas.drawRoundRect(mRectF, radius, radius, stroke);
        }

        // Label
        mTextPaint.setTextSize(h * TEXT_SIZE_FRACTION);
        mTextPaint.setColor(mData.fgColor);
        float ty = h / 2f - (mTextPaint.ascent() + mTextPaint.descent()) / 2f;
        canvas.drawText(mData.label, w / 2f, ty, mTextPaint);
    }

    /** Blend the bgColor toward white to produce a pressed highlight. */
    private static int blendHighlight(int color) {
        int r = Math.min(255, (Color.red(color)   + 80));
        int g = Math.min(255, (Color.green(color) + 80));
        int b = Math.min(255, (Color.blue(color)  + 80));
        return Color.argb(Color.alpha(color), r, g, b);
    }
}
