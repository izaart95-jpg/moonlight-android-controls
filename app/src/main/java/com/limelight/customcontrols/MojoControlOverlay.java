package com.limelight.customcontrols;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.limelight.binding.input.ControllerHandler;
import com.limelight.nvstream.NvConnection;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen transparent overlay that renders MojoLauncher-compatible
 * custom on-screen controls loaded from a JSON file.
 *
 * ┌─────────────────────────────────────────────────────┐
 * │  Tap  ⊕  once  → show/hide controls                │
 * │  Tap  ⊕  twice → enter/exit edit mode (drag)        │
 * │  In edit mode: drag any button to reposition         │
 * └─────────────────────────────────────────────────────┘
 */
public class MojoControlOverlay extends FrameLayout
        implements MojoInputBridge.SpecialButtonListener {

    private static final String TAG          = "MojoControlOverlay";
    private static final String PREFS_NAME   = "MojoControls";
    private static final String KEY_JSON     = "layout_json";
    private static final String KEY_SCALE    = "button_scale";

    // ─── State ────────────────────────────────────────────────────────────────
    private boolean mControlsVisible = false;
    private boolean mEditMode        = false;

    // Floating ⊕ button tap tracking for double-tap detection
    private long    mLastTapTime     = 0;
    private static final long DOUBLE_TAP_MS = 400;

    // ─── Child views ──────────────────────────────────────────────────────────
    private final List<MojoButtonView> mButtonViews = new ArrayList<>();
    private Button mFloatingBtn;

    // ─── Input pipeline ───────────────────────────────────────────────────────
    private final MojoInputBridge mBridge = new MojoInputBridge();

    // ─── Drag state (edit mode) ───────────────────────────────────────────────
    private View  mDragTarget  = null;
    private float mDragOffsetX = 0;
    private float mDragOffsetY = 0;

    // ─── Layout data ──────────────────────────────────────────────────────────
    private String mCurrentJson = null;
    private float  mButtonScale = 100f;

    // ─── Constructor ─────────────────────────────────────────────────────────

    public MojoControlOverlay(Context context) {
        super(context);
        setBackgroundColor(0x00000000);
        setClickable(false);
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    public void attach(NvConnection conn, ControllerHandler handler) {
        mBridge.attach(conn, handler, this);
    }

    public void detach() {
        releaseAll();
        mBridge.detach();
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Create the floating ⊕ button and add it to the given parent layout.
     * Call this once from Game.connectionStarted() BEFORE calling loadSavedLayout().
     */
    public void initFloatingButton(FrameLayout parent) {
        mFloatingBtn = new Button(getContext());
        mFloatingBtn.setText("⊕");
        mFloatingBtn.setTextSize(20);
        mFloatingBtn.setAlpha(0.6f);
        mFloatingBtn.setBackgroundColor(0xAA000000);
        mFloatingBtn.setTextColor(0xFFFFFFFF);
        mFloatingBtn.setPadding(16, 8, 16, 8);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.gravity   = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.topMargin = 6;
        parent.addView(mFloatingBtn, lp);

        mFloatingBtn.setOnClickListener(v -> handleFloatingButtonTap());
        mFloatingBtn.setOnLongClickListener(v -> { showOverlayMenu(v); return true; });
    }

    /**
     * Load a saved JSON layout from SharedPreferences.
     * @return true if a layout was found and applied.
     */
    public boolean loadSavedLayout() {
        SharedPreferences prefs = getContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_JSON, null);
        if (json == null) return false;
        mButtonScale = prefs.getFloat(KEY_SCALE, 100f);
        applyJson(json);
        return true;
    }

    /**
     * Parse and display a new JSON layout string.
     * Also saves it to SharedPreferences for future sessions.
     */
    public void applyJson(String json) {
        mCurrentJson = json;
        clearButtons();
        DisplayMetrics dm = getResources().getDisplayMetrics();

        List<MojoJsonParser.MojoButton> buttons;
        try {
            buttons = MojoJsonParser.parse(json, dm, mButtonScale);
        } catch (JSONException e) {
            Log.e(TAG, "JSON parse error", e);
            Toast.makeText(getContext(), "Invalid layout JSON: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            return;
        }

        for (MojoJsonParser.MojoButton data : buttons) {
            MojoButtonView view = new MojoButtonView(getContext(), data, mBridge);
            view.setEditMode(mEditMode);
            mButtonViews.add(view);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    Math.round(data.w), Math.round(data.h));
            lp.leftMargin = Math.round(data.x);
            lp.topMargin  = Math.round(data.y);
            addView(view, lp);
        }

        // Save for next session
        if (mCurrentJson != null) {
            getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_JSON, mCurrentJson)
                    .putFloat(KEY_SCALE, mButtonScale)
                    .apply();
        }

        Log.d(TAG, "Layout applied: " + buttons.size() + " buttons");
    }

    /** Show the file-picker dialog so the user can import a JSON layout. */
    public void showImportDialog() {
        // On Android without a file picker dependency we use a text-paste dialog
        android.widget.EditText input = new android.widget.EditText(getContext());
        input.setHint("Paste your MojoLauncher JSON layout here");
        input.setMinLines(4);
        input.setMaxLines(12);

        new AlertDialog.Builder(getContext())
                .setTitle("Import Layout JSON")
                .setMessage("Paste the contents of your .json control layout file:")
                .setView(input)
                .setPositiveButton("Apply", (d, w) -> {
                    String text = input.getText().toString().trim();
                    if (text.isEmpty()) return;
                    applyJson(text);
                    showControls();
                    Toast.makeText(getContext(), "Layout imported!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─── Visibility ──────────────────────────────────────────────────────────

    private void showOverlayMenu(View anchor) {
        PopupMenu popup = new PopupMenu(getContext(), anchor);
        popup.getMenu().add(0, 10, 0, "Import Layout from JSON");
        if (mControlsVisible) {
            popup.getMenu().add(0, 11, 1, "Hide Controls");
            popup.getMenu().add(0, 12, 2, mEditMode ? "Exit Edit Mode" : "Edit Layout");
        } else {
            popup.getMenu().add(0, 11, 1, "Show Controls");
        }
        popup.getMenu().add(0, 13, 3, "Clear Saved Layout");
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 10) { showImportDialog(); return true; }
            if (id == 11) { if (mControlsVisible) hideControls(); else showControls(); return true; }
            if (id == 12) { if (mEditMode) exitEditMode(); else enterEditMode(); return true; }
            if (id == 13) {
                getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit().remove(KEY_JSON).apply();
                clearButtons();
                Toast.makeText(getContext(), "Layout cleared", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        popup.show();
    }

    public void showControls() {
        mControlsVisible = true;
        setVisibility(VISIBLE);
        updateFloatingLabel();
    }

    public void hideControls() {
        mControlsVisible = false;
        releaseAll();
        setVisibility(INVISIBLE);
        updateFloatingLabel();
    }

    public boolean areControlsVisible() { return mControlsVisible; }

    // ─── Edit mode ───────────────────────────────────────────────────────────

    public void enterEditMode() {
        mEditMode = true;
        releaseAll();
        for (MojoButtonView v : mButtonViews) v.setEditMode(true);
        updateFloatingLabel();
        Toast.makeText(getContext(), "Edit mode — drag buttons to reposition",
                Toast.LENGTH_SHORT).show();
    }

    public void exitEditMode() {
        mEditMode = false;
        for (MojoButtonView v : mButtonViews) v.setEditMode(false);
        updateFloatingLabel();
        // Persist updated positions back into the JSON
        persistCurrentPositions();
        Toast.makeText(getContext(), "Layout saved", Toast.LENGTH_SHORT).show();
    }

    public boolean isEditMode() { return mEditMode; }

    // ─── Floating button logic ────────────────────────────────────────────────

    private void handleFloatingButtonTap() {
        long now = System.currentTimeMillis();
        boolean isDoubleTap = (now - mLastTapTime) < DOUBLE_TAP_MS;
        mLastTapTime = now;

        if (isDoubleTap) {
            // Double-tap: toggle edit mode (controls must be visible first)
            if (!mControlsVisible) showControls();
            if (mEditMode) exitEditMode();
            else           enterEditMode();
        } else {
            // Single tap: show/hide controls
            if (mControlsVisible) hideControls();
            else                  showControls();
        }
    }

    private void updateFloatingLabel() {
        if (mFloatingBtn == null) return;
        if (mEditMode)              mFloatingBtn.setText("✓ Done");
        else if (mControlsVisible)  mFloatingBtn.setText("⊕ ON");
        else                        mFloatingBtn.setText("⊕ OFF");
    }

    // ─── Edit-mode drag ───────────────────────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mEditMode; // intercept all touches in edit mode
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mEditMode) return false;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                float ex = event.getX(), ey = event.getY();
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

    private View findChildAt(float ex, float ey) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (child.getVisibility() != VISIBLE) continue;
            if (ex >= child.getX() && ex < child.getX() + child.getWidth() &&
                    ey >= child.getY() && ey < child.getY() + child.getHeight())
                return child;
        }
        return null;
    }

    // ─── Position persistence ─────────────────────────────────────────────────

    /**
     * After dragging, update the in-memory button positions and save a
     * simplified JSON so positions persist across sessions.
     */
    private void persistCurrentPositions() {
        if (mButtonViews.isEmpty()) return;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float sw = getWidth(), sh = getHeight();
        if (sw == 0 || sh == 0) return;

        // Build a simple JSON array with absolute fractional positions
        try {
            org.json.JSONArray arr = new org.json.JSONArray();
            for (MojoButtonView bv : mButtonViews) {
                MojoJsonParser.MojoButton d = bv.getData();
                // Update position from current view translation
                float nx = bv.getX();
                float ny = bv.getY();

                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("name",         d.name);
                obj.put("bgColor",      d.bgColor);
                obj.put("strokeColor",  d.strokeColor);
                obj.put("strokeWidth",  d.strokeWidth);
                obj.put("cornerRadius", d.cornerRadius);
                obj.put("opacity",      d.opacity);
                obj.put("isToggle",     d.isToggle);
                obj.put("isSwipeable",  d.isSwipeable);
                obj.put("displayInGame",d.displayInGame);
                obj.put("displayInMenu",d.displayInMenu);
                // Store as absolute fractional positions (simpler, device-independent)
                obj.put("dynamicX",     (nx / sw) + " * ${screen_width}");
                obj.put("dynamicY",     (ny / sh) + " * ${screen_height}");
                // Convert px back to dp for storage
                obj.put("width",  d.w / dm.density / mButtonScale * 250f);
                obj.put("height", d.h / dm.density / mButtonScale * 250f);
                org.json.JSONArray kc = new org.json.JSONArray();
                for (int k : d.keycodes) kc.put(k);
                obj.put("keycodes", kc);
                arr.put(obj);
            }
            org.json.JSONObject root = new org.json.JSONObject();
            root.put("mControlDataList", arr);
            root.put("mDrawerDataList",  new org.json.JSONArray());
            root.put("mJoystickDataList",new org.json.JSONArray());
            root.put("scaledAt",         250);
            root.put("version",          8);
            mCurrentJson = root.toString();
            getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putString(KEY_JSON, mCurrentJson).apply();
        } catch (org.json.JSONException e) {
            Log.e(TAG, "Failed to persist positions", e);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void clearButtons() {
        releaseAll();
        removeAllViews();
        mButtonViews.clear();
    }

    private void releaseAll() {
        int[][] allCodes = new int[mButtonViews.size()][];
        for (int i = 0; i < mButtonViews.size(); i++)
            allCodes[i] = mButtonViews.get(i).getData().keycodes;
        mBridge.releaseAll(allCodes);
    }

    // ─── SpecialButtonListener ────────────────────────────────────────────────

    @Override
    public void onToggleKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.toggleSoftInput(0, 0);
    }

    @Override
    public void onToggleControls() {
        if (mControlsVisible) hideControls();
        else                  showControls();
    }

    @Override
    public void onToggleVirtualMouse() {
        // Virtual mouse is a moonlight-side concept; show a toast for now
        Toast.makeText(getContext(), "Virtual mouse toggle", Toast.LENGTH_SHORT).show();
    }
}

