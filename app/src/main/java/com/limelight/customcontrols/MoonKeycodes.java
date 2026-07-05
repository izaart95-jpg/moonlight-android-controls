package com.limelight.customcontrols;

import com.limelight.binding.input.KeyboardTranslator;
import com.limelight.nvstream.input.ControllerPacket;

/**
 * Virtual keycodes used by the custom on-screen controls.
 *
 * Negative values map to gamepad inputs (controller buttons, triggers, sticks).
 * Positive values are VK key codes forwarded to the host as keyboard input.
 */
public final class MoonKeycodes {

    private MoonKeycodes() {}

    // ─── Gamepad face buttons ────────────────────────────────────────────────
    public static final int MOON_BTN_A      = -101;
    public static final int MOON_BTN_B      = -102;
    public static final int MOON_BTN_X      = -103;
    public static final int MOON_BTN_Y      = -104;

    // ─── Shoulder buttons ────────────────────────────────────────────────────
    public static final int MOON_BTN_LB     = -105;
    public static final int MOON_BTN_RB     = -106;

    // ─── Triggers (digital, send max trigger value when pressed) ─────────────
    public static final int MOON_BTN_LT     = -107;
    public static final int MOON_BTN_RT     = -108;

    // ─── System buttons ──────────────────────────────────────────────────────
    public static final int MOON_BTN_START  = -109;
    public static final int MOON_BTN_BACK   = -110;
    public static final int MOON_BTN_GUIDE  = -111;

    // ─── Stick-click buttons ─────────────────────────────────────────────────
    public static final int MOON_BTN_L3     = -112;
    public static final int MOON_BTN_R3     = -113;

    // ─── D-Pad ───────────────────────────────────────────────────────────────
    public static final int MOON_DPAD_UP    = -114;
    public static final int MOON_DPAD_DOWN  = -115;
    public static final int MOON_DPAD_LEFT  = -116;
    public static final int MOON_DPAD_RIGHT = -117;

    // ─── Extended / Sunshine-only buttons ────────────────────────────────────
    public static final int MOON_BTN_PADDLE1   = -121;
    public static final int MOON_BTN_PADDLE2   = -122;
    public static final int MOON_BTN_PADDLE3   = -123;
    public static final int MOON_BTN_PADDLE4   = -124;
    public static final int MOON_BTN_TOUCHPAD  = -125;
    public static final int MOON_BTN_MISC      = -126;

    // ─── Analog sticks (used by MoonAnalogStick, not by MoonButton) ──────────
    public static final int MOON_STICK_LEFT  = -201;
    public static final int MOON_STICK_RIGHT = -202;

    // ─── Common keyboard VK shortcuts (positive = VK codes) ──────────────────
    public static final int VK_ESCAPE   = KeyboardTranslator.VK_ESCAPE;
    public static final int VK_TAB      = KeyboardTranslator.VK_TAB;
    public static final int VK_SPACE    = KeyboardTranslator.VK_SPACE;
    public static final int VK_BACK     = KeyboardTranslator.VK_BACK_SPACE;
    public static final int VK_ENTER    = 10;   // standard Java KeyEvent.VK_ENTER
    public static final int VK_F1       = KeyboardTranslator.VK_F1;   // F2-F12 = F1+n
    public static final int VK_UP       = KeyboardTranslator.VK_UP;
    public static final int VK_DOWN     = KeyboardTranslator.VK_DOWN;
    public static final int VK_LEFT     = KeyboardTranslator.VK_LEFT;
    public static final int VK_RIGHT    = KeyboardTranslator.VK_RIGHT;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** Returns true if this keycode refers to a gamepad button (not an axis). */
    public static boolean isGamepadButton(int keycode) {
        return keycode <= -101 && keycode >= -130;
    }

    /** Returns true if this keycode is an analog stick specifier. */
    public static boolean isAnalogStick(int keycode) {
        return keycode == MOON_STICK_LEFT || keycode == MOON_STICK_RIGHT;
    }

    /** Returns true if this keycode is a keyboard VK code. */
    public static boolean isKeyboardKey(int keycode) {
        return keycode > 0;
    }

    /**
     * Convert a gamepad button keycode to the matching ControllerPacket flag.
     * Returns 0 if the keycode is not a standard button flag.
     */
    public static int toControllerFlag(int keycode) {
        switch (keycode) {
            case MOON_BTN_A:       return ControllerPacket.A_FLAG;
            case MOON_BTN_B:       return ControllerPacket.B_FLAG;
            case MOON_BTN_X:       return ControllerPacket.X_FLAG;
            case MOON_BTN_Y:       return ControllerPacket.Y_FLAG;
            case MOON_BTN_LB:      return ControllerPacket.LB_FLAG;
            case MOON_BTN_RB:      return ControllerPacket.RB_FLAG;
            case MOON_BTN_START:   return ControllerPacket.PLAY_FLAG;
            case MOON_BTN_BACK:    return ControllerPacket.BACK_FLAG;
            case MOON_BTN_GUIDE:   return ControllerPacket.SPECIAL_BUTTON_FLAG;
            case MOON_BTN_L3:      return ControllerPacket.LS_CLK_FLAG;
            case MOON_BTN_R3:      return ControllerPacket.RS_CLK_FLAG;
            case MOON_DPAD_UP:     return ControllerPacket.UP_FLAG;
            case MOON_DPAD_DOWN:   return ControllerPacket.DOWN_FLAG;
            case MOON_DPAD_LEFT:   return ControllerPacket.LEFT_FLAG;
            case MOON_DPAD_RIGHT:  return ControllerPacket.RIGHT_FLAG;
            case MOON_BTN_PADDLE1: return ControllerPacket.PADDLE1_FLAG;
            case MOON_BTN_PADDLE2: return ControllerPacket.PADDLE2_FLAG;
            case MOON_BTN_PADDLE3: return ControllerPacket.PADDLE3_FLAG;
            case MOON_BTN_PADDLE4: return ControllerPacket.PADDLE4_FLAG;
            case MOON_BTN_TOUCHPAD:return ControllerPacket.TOUCHPAD_FLAG;
            case MOON_BTN_MISC:    return ControllerPacket.MISC_FLAG;
            default:               return 0;
        }
    }

    /** Human-readable label for a keycode, used when auto-labelling buttons. */
    public static String defaultLabel(int keycode) {
        switch (keycode) {
            case MOON_BTN_A:        return "A";
            case MOON_BTN_B:        return "B";
            case MOON_BTN_X:        return "X";
            case MOON_BTN_Y:        return "Y";
            case MOON_BTN_LB:       return "LB";
            case MOON_BTN_RB:       return "RB";
            case MOON_BTN_LT:       return "LT";
            case MOON_BTN_RT:       return "RT";
            case MOON_BTN_START:    return "START";
            case MOON_BTN_BACK:     return "BACK";
            case MOON_BTN_GUIDE:    return "GUIDE";
            case MOON_BTN_L3:       return "L3";
            case MOON_BTN_R3:       return "R3";
            case MOON_DPAD_UP:      return "↑";
            case MOON_DPAD_DOWN:    return "↓";
            case MOON_DPAD_LEFT:    return "←";
            case MOON_DPAD_RIGHT:   return "→";
            case MOON_STICK_LEFT:   return "L-Stick";
            case MOON_STICK_RIGHT:  return "R-Stick";
            default:
                if (keycode > 0) return "KEY";
                return "BTN";
        }
    }
}
