package com.limelight.customcontrols;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.limelight.binding.input.ControllerHandler;
import com.limelight.nvstream.NvConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen transparent overlay that hosts on-screen buttons and analog
 * sticks for game streaming.
 *
 * <h3>Modes</h3>
 * <ul>
 *   <li><b>Active</b> – touches are forwarded to individual controls.</li>
 *   <li><b>Edit</b>   – controls can be dragged to new positions; a long-press
 *       on a control opens a simple property toast (future: property dialog).</li>
 * </ul>
 *
 * <h3>Layout persistence</h3>
 * The current layout is saved to {@link SharedPreferences} in JSON format via
 * {@link #saveLayout()} and reloaded on the next call to {@link #loadLayout()}.
 */
public class MoonControlOverlay extends FrameLayout {

    private static final String TAG = "MoonControlOverlay";
    static final String PREFS_NAME  = "MoonCustomControls";
    static final String KEY_BUTTONS = "buttons";
    static final String KEY_STICKS  = "sticks";

    // ─── Data ─────────────────────────────────────────────────────────────────
    private final List<ControlButtonData> mButtonDataList = new ArrayList<>();
    private final List<AnalogStickData>   mStickDataList  = new ArrayList<>();

    // ─── Views ────────────────────────────────────────────────────────────────
    private final List<MoonButton>      mButtons = new ArrayList<>();
    private final List<MoonAnalogStick> mSticks  = new ArrayList<>();

    // ─── Input bridge ─────────────────────────────────────────────────────────
    private final MoonInputBridge mBridge;

    // ─── Mode ─────────────────────────────────────────────────────────────────
    private boolean mEditMode = false;

    // ─── Edit drag state ──────────────────────────────────────────────────────
    private View  mDragTarget   = null;
    private float mDragOffsetX  = 0;
    private float mDragOffsetY  = 0;

    // ─── Constructor ─────────────────────────────────────────────────────────

    public MoonControlOverlay(Context context) {
        super(context);
        mBridge = new MoonInputBridge();
        setBackgroundColor(0x00000000); // fully transparent
        setClickable(false);
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    /** Call once the NvConnection and ControllerHandler are ready. */
    public void attach(NvConnection conn, ControllerHandler handler) {
        mBridge.attach(conn, handler);
    }

    /** Release references; call from Game.onDestroy(). */
    public void detach() {
        mBridge.resetAll();
        mBridge.detach();
    }

    // ─── Layout management ───────────────────────────────────────────────────

    /**
     * Add a button to the overlay from its data.  The view is positioned
     * immediately based on the current overlay dimensions.
     */
    public void addButton(ControlButtonData data) {
        mButtonDataList.add(data);
        MoonButton btn = new MoonButton(getContext(), data, mBridge);
        btn.setEditMode(mEditMode);
        mButtons.add(btn);
        addView(btn);
        positionButton(btn, data);
    }

    /**
     * Add an analog stick to the overlay from its data.
     */
    public void addStick(AnalogStickData data) {
        mStickDataList.add(data);
        MoonAnalogStick stick = new MoonAnalogStick(getContext(), data, mBridge);
        stick.setEditMode(mEditMode);
        mSticks.add(stick);
        addView(stick);
        positionStick(stick, data);
    }

    /** Remove all existing controls. */
    public void clearControls() {
        mBridge.resetAll();
        removeAllViews();
        mButtons.clear();
        mSticks.clear();
        mButtonDataList.clear();
        mStickDataList.clear();
    }

    // ─── Position helpers ────────────────────────────────────────────────────

    private void positionButton(MoonButton btn, ControlButtonData d) {
        int ow = getWidth();
        int oh = getHeight();
        if (ow == 0 || oh == 0) {
            // Layout hasn't happened yet; defer
            btn.post(() -> positionButton(btn, d));
            return;
        }
        int px = Math.round(d.x * ow);
        int py = Math.round(d.y * oh);
        int bw = Math.round(d.w * ow);
        int bh = Math.round(d.h * oh);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(bw, bh);
        lp.leftMargin = px;
        lp.topMargin  = py;
        btn.setLayoutParams(lp);
    }

    private void positionStick(MoonAnalogStick stick, AnalogStickData d) {
        int ow = getWidth();
        int oh = getHeight();
        if (ow == 0 || oh == 0) {
            stick.post(() -> positionStick(stick, d));
            return;
        }
        int px = Math.round(d.x * ow);
        int py = Math.round(d.y * oh);
        int sz = Math.round(d.diameter * oh);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(sz, sz);
        lp.leftMargin = px;
        lp.topMargin  = py;
        stick.setLayoutParams(lp);
    }

    /** Re-apply all positions (call after rotation or resize). */
    public void refreshLayout() {
        for (int i = 0; i < mButtons.size(); i++) positionButton(mButtons.get(i), mButtonDataList.get(i));
        for (int i = 0; i < mSticks.size();  i++) positionStick (mSticks.get(i),  mStickDataList.get(i));
    }

    // ─── Edit mode ───────────────────────────────────────────────────────────

    /** Toggle between active and edit modes. */
    public void toggleEditMode() {
        setEditMode(!mEditMode);
    }

    public boolean isEditMode() { return mEditMode; }

    private void setEditMode(boolean edit) {
        mEditMode = edit;
        mDragTarget = null;
        for (MoonButton btn   : mButtons) btn.setEditMode(edit);
        for (MoonAnalogStick s : mSticks) s.setEditMode(edit);
        if (!edit) saveLayout();
        String msg = edit ? "Edit mode ON – drag to reposition, tap again to exit"
                          : "Layout saved";
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    // ─── Touch dispatch (edit mode drag) ─────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        // Only intercept in edit mode to handle drag
        return mEditMode;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mEditMode) return false;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                float ex = event.getX();
                float ey = event.getY();
                mDragTarget = findChildAt(ex, ey);
                if (mDragTarget != null) {
                    mDragOffsetX = ex - mDragTarget.getX();
                    mDragOffsetY = ey - mDragTarget.getY();
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (mDragTarget != null) {
                    float nx = event.getX() - mDragOffsetX;
                    float ny = event.getY() - mDragOffsetY;
                    nx = Math.max(0, Math.min(nx, getWidth()  - mDragTarget.getWidth()));
                    ny = Math.max(0, Math.min(ny, getHeight() - mDragTarget.getHeight()));
                    mDragTarget.setX(nx);
                    mDragTarget.setY(ny);
                    updateDataPosition(mDragTarget, nx, ny);
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mDragTarget = null;
                return true;
        }
        return false;
    }

    /** Find the topmost visible child that contains the point (ex, ey). */
    private View findChildAt(float ex, float ey) {
        // Iterate in reverse (top-most drawn last in FrameLayout)
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (child.getVisibility() != VISIBLE) continue;
            if (ex >= child.getX() && ex < child.getX() + child.getWidth() &&
                ey >= child.getY() && ey < child.getY() + child.getHeight()) {
                return child;
            }
        }
        return null;
    }

    /** Update the fractional position stored in ControlButtonData / AnalogStickData. */
    private void updateDataPosition(View v, float px, float py) {
        int ow = getWidth();
        int oh = getHeight();
        if (ow == 0 || oh == 0) return;
        float fx = px / ow;
        float fy = py / oh;
        int btnIdx = mButtons.indexOf(v);
        if (btnIdx >= 0) {
            mButtonDataList.get(btnIdx).x = fx;
            mButtonDataList.get(btnIdx).y = fy;
            return;
        }
        int stkIdx = mSticks.indexOf(v);
        if (stkIdx >= 0) {
            mStickDataList.get(stkIdx).x = fx;
            mStickDataList.get(stkIdx).y = fy;
        }
    }

    // ─── Save / Load ─────────────────────────────────────────────────────────

    /** Persist the current layout to SharedPreferences. */
    public void saveLayout() {
        try {
            JSONArray buttons = new JSONArray();
            for (ControlButtonData d : mButtonDataList) buttons.put(d.toJson());

            JSONArray sticks = new JSONArray();
            for (AnalogStickData d : mStickDataList) sticks.put(d.toJson());

            JSONObject root = new JSONObject();
            root.put(KEY_BUTTONS, buttons);
            root.put(KEY_STICKS,  sticks);

            SharedPreferences.Editor ed = getContext()
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
            ed.putString("layout", root.toString());
            ed.apply();
            Log.d(TAG, "Layout saved.");
        } catch (JSONException e) {
            Log.e(TAG, "Failed to save layout", e);
        }
    }

    /**
     * Load a previously saved layout.
     * @return true if a saved layout was found and applied; false if the
     *         caller should apply a default layout instead.
     */
    public boolean loadLayout() {
        SharedPreferences prefs = getContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString("layout", null);
        if (json == null) return false;

        try {
            JSONObject root    = new JSONObject(json);
            JSONArray  buttons = root.optJSONArray(KEY_BUTTONS);
            JSONArray  sticks  = root.optJSONArray(KEY_STICKS);

            clearControls();

            if (buttons != null) {
                for (int i = 0; i < buttons.length(); i++) {
                    addButton(ControlButtonData.fromJson(buttons.getJSONObject(i)));
                }
            }
            if (sticks != null) {
                for (int i = 0; i < sticks.length(); i++) {
                    addStick(AnalogStickData.fromJson(sticks.getJSONObject(i)));
                }
            }
            Log.d(TAG, "Layout loaded.");
            return true;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to load layout", e);
            return false;
        }
    }

    /** Delete the saved layout from SharedPreferences. */
    public void resetLayout() {
        getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().remove("layout").apply();
    }

    // ─── Visibility helpers ──────────────────────────────────────────────────

    public void showControls() {
        setVisibility(VISIBLE);
    }

    public void hideControls() {
        mBridge.resetAll();
        setVisibility(INVISIBLE);
    }
}
