package com.limelight.customcontrols;

import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.limelight.preferences.PreferenceConfiguration;

/**
 * Factory class that populates a {@link MoonControlOverlay} with a sensible
 * default gamepad layout.
 *
 * <p>All positions are expressed as fractions of the screen dimensions so the
 * layout adapts to different device sizes and aspect ratios.</p>
 *
 * <p>Layout overview (landscape 16:9):</p>
 * <pre>
 *   [LT][LB]                          [RB][RT]
 *   [LS]   [BACK]       [START]   [RS]
 *
 *   [D-Pad]                        [Y]
 *                               [X]   [B]
 *                                  [A]
 * </pre>
 */
public final class MoonDefaultLayout {

    private MoonDefaultLayout() {}

    /**
     * Populate the given overlay with the default gamepad layout.
     *
     * @param overlay    Target overlay (must already be attached to a window so
     *                   dimensions are available, or deferred via post()).
     * @param prefConfig Used to check flip-face-buttons and onlyL3R3 prefs.
     */
    public static void apply(MoonControlOverlay overlay, PreferenceConfiguration prefConfig) {
        overlay.clearControls();

        if (!prefConfig.onlyL3R3) {
            addFullLayout(overlay, prefConfig);
        } else {
            addL3R3Only(overlay);
        }
    }

    // ─── Full layout ──────────────────────────────────────────────────────────

    private static void addFullLayout(MoonControlOverlay overlay,
                                       PreferenceConfiguration prefConfig) {

        final boolean flip = prefConfig.flipFaceButtons;

        // ─ Left analog stick ──────────────────────────────────────────────────
        AnalogStickData ls = new AnalogStickData(MoonKeycodes.MOON_STICK_LEFT,
                0.04f, 0.35f, 0.20f);
        ls.baseColor = 0x66FFFFFF;
        ls.knobColor = 0xAAFFFFFF;
        overlay.addStick(ls);

        // ─ Right analog stick ─────────────────────────────────────────────────
        AnalogStickData rs = new AnalogStickData(MoonKeycodes.MOON_STICK_RIGHT,
                0.65f, 0.50f, 0.20f);
        rs.baseColor = 0x66FFFFFF;
        rs.knobColor = 0xAAFFFFFF;
        overlay.addStick(rs);

        // ─ D-Pad (4 separate buttons in a cross) ──────────────────────────────
        float dpadCX = 0.07f;  float dpadCY = 0.58f;
        float dpadSW  = 0.055f; float dpadSH = 0.10f;
        float gap = dpadSW + 0.005f;

        overlay.addButton(btn("↑",  MoonKeycodes.MOON_DPAD_UP,    dpadCX, dpadCY - dpadSH - 0.01f, dpadSW, dpadSH));
        overlay.addButton(btn("↓",  MoonKeycodes.MOON_DPAD_DOWN,  dpadCX, dpadCY + dpadSH + 0.01f, dpadSW, dpadSH));
        overlay.addButton(btn("←",  MoonKeycodes.MOON_DPAD_LEFT,  dpadCX - gap, dpadCY, dpadSW, dpadSH));
        overlay.addButton(btn("→",  MoonKeycodes.MOON_DPAD_RIGHT, dpadCX + gap, dpadCY, dpadSW, dpadSH));

        // ─ Face buttons ───────────────────────────────────────────────────────
        float faceR  = 0.88f; float faceM  = 0.40f;
        float fW = 0.055f;  float fH = 0.10f;
        float fGap = fW + 0.008f;
        int kA = flip ? MoonKeycodes.MOON_BTN_B : MoonKeycodes.MOON_BTN_A;
        int kB = flip ? MoonKeycodes.MOON_BTN_A : MoonKeycodes.MOON_BTN_B;
        int kX = flip ? MoonKeycodes.MOON_BTN_Y : MoonKeycodes.MOON_BTN_X;
        int kY = flip ? MoonKeycodes.MOON_BTN_X : MoonKeycodes.MOON_BTN_Y;
        String lA = flip ? "B" : "A"; String lB = flip ? "A" : "B";
        String lX = flip ? "Y" : "X"; String lY = flip ? "X" : "Y";

        // Cross layout: A bottom, B right, X left, Y top
        overlay.addButton(btn(lA, kA, faceR,        faceM + fGap,  fW, fH));
        overlay.addButton(btn(lB, kB, faceR + fGap, faceM,          fW, fH));
        overlay.addButton(btn(lX, kX, faceR - fGap, faceM,          fW, fH));
        overlay.addButton(btn(lY, kY, faceR,        faceM - fGap,  fW, fH));

        // ─ Shoulder buttons ───────────────────────────────────────────────────
        float sW = 0.07f; float sH = 0.09f;
        overlay.addButton(btn("LB", MoonKeycodes.MOON_BTN_LB, 0.04f, 0.25f, sW, sH));
        overlay.addButton(btn("LT", MoonKeycodes.MOON_BTN_LT, 0.04f, 0.14f, sW, sH));
        overlay.addButton(btn("RB", MoonKeycodes.MOON_BTN_RB, 0.89f, 0.25f, sW, sH));
        overlay.addButton(btn("RT", MoonKeycodes.MOON_BTN_RT, 0.89f, 0.14f, sW, sH));

        // ─ Back / Start ───────────────────────────────────────────────────────
        float mW = 0.07f; float mH = 0.075f;
        overlay.addButton(btn("BACK",  MoonKeycodes.MOON_BTN_BACK,  0.36f, 0.62f, mW, mH));
        overlay.addButton(btn("START", MoonKeycodes.MOON_BTN_START, 0.57f, 0.62f, mW, mH));

        if (prefConfig.showGuideButton) {
            overlay.addButton(btn("⊙", MoonKeycodes.MOON_BTN_GUIDE, 0.465f, 0.62f, 0.055f, 0.075f));
        }
    }

    // ─── L3 / R3 only layout ──────────────────────────────────────────────────

    private static void addL3R3Only(MoonControlOverlay overlay) {
        overlay.addButton(btn("L3", MoonKeycodes.MOON_BTN_L3, 0.04f, 0.75f, 0.07f, 0.10f));
        overlay.addButton(btn("R3", MoonKeycodes.MOON_BTN_R3, 0.89f, 0.75f, 0.07f, 0.10f));
    }

    // ─── Builder helper ───────────────────────────────────────────────────────

    private static ControlButtonData btn(String label, int keycode,
                                          float x, float y, float w, float h) {
        ControlButtonData d = new ControlButtonData(label, new int[]{keycode});
        d.x = x; d.y = y; d.w = w; d.h = h;
        d.bgColor      = 0x99000000;
        d.fgColor      = 0xFFFFFFFF;
        d.cornerRadius = 0.35f;
        d.opacity      = 1f;
        return d;
    }
}

