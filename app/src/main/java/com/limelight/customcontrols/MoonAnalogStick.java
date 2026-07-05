package com.limelight.customcontrols;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/**
 * A virtual analog stick drawn as two concentric circles:
 * <ul>
 *   <li>Outer ring — the "base" (fixed or touch-follow depending on
 *       {@link AnalogStickData#relativeMode}).</li>
 *   <li>Inner knob — moves with the user's finger, clamped inside the base.</li>
 * </ul>
 *
 * <p>Normalised [-1, 1] axis values are forwarded to {@link MoonInputBridge} on
 * every finger move, and zeroed on release.</p>
 */
@SuppressLint("ViewConstructor")
public class MoonAnalogStick extends View {

    // ─── Data & bridge ────────────────────────────────────────────────────────
    private AnalogStickData mData;
    private final MoonInputBridge mBridge;

    // ─── State ────────────────────────────────────────────────────────────────
    private int   mPointerId = -1;
    /** Centre of the base circle in view-local pixels. */
    private float mBaseCx, mBaseCy;
    /** Current knob position in view-local pixels. */
    private float mKnobCx, mKnobCy;
    /** Radius of the usable movement area (base radius minus knob radius). */
    private float mMoveRadius;
    private boolean mEditMode = false;

    // ─── Drawing ─────────────────────────────────────────────────────────────
    private final Paint mBasePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mKnobPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mRingPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ─── Constructor ─────────────────────────────────────────────────────────

    public MoonAnalogStick(Context context, AnalogStickData data, MoonInputBridge bridge) {
        super(context);
        mBridge = bridge;
        setData(data);
        setFocusable(false);
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    public AnalogStickData getData() { return mData; }

    public void setData(AnalogStickData data) {
        mData = data;
        applyOpacity(mBasePaint, mData.baseColor, mData.opacity);
        applyOpacity(mKnobPaint, mData.knobColor, mData.opacity);
        mRingPaint.setStyle(Paint.Style.STROKE);
        mRingPaint.setStrokeWidth(3f);
        applyOpacity(mRingPaint, mData.baseColor, mData.opacity * 1.5f);
        invalidate();
    }

    public void setEditMode(boolean edit) {
        mEditMode = edit;
        if (edit) resetKnob();
    }

    // ─── Size change ─────────────────────────────────────────────────────────

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        resetBase();
    }

    private void resetBase() {
        mBaseCx = getWidth()  / 2f;
        mBaseCy = getHeight() / 2f;
        float baseRadius = Math.min(getWidth(), getHeight()) / 2f;
        float knobRadius = baseRadius * 0.40f;
        mMoveRadius = baseRadius - knobRadius;
        resetKnob();
    }

    private void resetKnob() {
        mKnobCx = mBaseCx;
        mKnobCy = mBaseCy;
        invalidate();
    }

    // ─── Touch handling ──────────────────────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mEditMode) return false;

        final int action    = event.getActionMasked();
        final int actionIdx = event.getActionIndex();
        final int pointerId = event.getPointerId(actionIdx);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (mPointerId == -1) {
                    mPointerId = pointerId;
                    if (mData.relativeMode) {
                        // Re-centre the base on the initial touch position
                        mBaseCx = event.getX(actionIdx);
                        mBaseCy = event.getY(actionIdx);
                    }
                    updateKnob(event, actionIdx);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (mPointerId != -1) {
                    int idx = event.findPointerIndex(mPointerId);
                    if (idx >= 0) updateKnob(event, idx);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (pointerId == mPointerId) {
                    mPointerId = -1;
                    if (mData.relativeMode) resetBase();
                    else                    resetKnob();
                    sendAxis(0f, 0f);
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                mPointerId = -1;
                if (mData.relativeMode) resetBase();
                else                    resetKnob();
                sendAxis(0f, 0f);
                return true;
        }
        return false;
    }

    private void updateKnob(MotionEvent event, int pointerIdx) {
        float dx = event.getX(pointerIdx) - mBaseCx;
        float dy = event.getY(pointerIdx) - mBaseCy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist > mMoveRadius) {
            dx = dx / dist * mMoveRadius;
            dy = dy / dist * mMoveRadius;
        }

        mKnobCx = mBaseCx + dx;
        mKnobCy = mBaseCy + dy;
        invalidate();

        // Normalise with deadzone
        float nx = (mMoveRadius > 0) ? dx / mMoveRadius : 0f;
        float ny = (mMoveRadius > 0) ? dy / mMoveRadius : 0f;
        nx = applyDeadzone(nx, mData.deadzone);
        ny = applyDeadzone(ny, mData.deadzone);
        sendAxis(nx, ny);
    }

    private void sendAxis(float nx, float ny) {
        if (mData.stickId == MoonKeycodes.MOON_STICK_LEFT) {
            mBridge.setLeftStick(nx, ny);
        } else {
            mBridge.setRightStick(nx, ny);
        }
    }

    /** Apply a circular deadzone: if magnitude < threshold, output 0. */
    private static float applyDeadzone(float v, float dz) {
        if (Math.abs(v) < dz) return 0f;
        // Re-normalise so the full [dz,1] range maps to [0,1]
        return (v - Math.signum(v) * dz) / (1f - dz);
    }

    // ─── Drawing ─────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        if (getWidth() == 0) return;
        float baseR = Math.min(getWidth(), getHeight()) / 2f;
        float knobR = baseR * 0.40f;

        // Base ring
        canvas.drawCircle(mBaseCx, mBaseCy, baseR, mBasePaint);
        canvas.drawCircle(mBaseCx, mBaseCy, baseR, mRingPaint);

        // Knob
        canvas.drawCircle(mKnobCx, mKnobCy, knobR, mKnobPaint);

        // Edit mode selection indicator
        if (mEditMode) {
            Paint editPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            editPaint.setStyle(Paint.Style.STROKE);
            editPaint.setColor(0xFFFF6600);
            editPaint.setStrokeWidth(4f);
            canvas.drawCircle(mBaseCx, mBaseCy, baseR - 2, editPaint);
        }
    }

    // ─── Utility ─────────────────────────────────────────────────────────────

    private static void applyOpacity(Paint paint, int color, float opacity) {
        paint.setColor(color);
        paint.setAlpha(Math.min(255, Math.round(Color.alpha(color) * opacity)));
    }
}

