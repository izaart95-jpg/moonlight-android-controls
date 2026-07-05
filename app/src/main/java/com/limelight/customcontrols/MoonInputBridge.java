package com.limelight.customcontrols;

import com.limelight.binding.input.ControllerHandler;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.input.KeyboardPacket;

/**
 * Maintains the current virtual gamepad state and routes custom-control
 * input events to the Moonlight NvConnection / ControllerHandler pipeline.
 *
 * <p>All public methods are thread-safe (synchronized on the state object).</p>
 */
public class MoonInputBridge {

    /** Byte value sent for a fully-pressed trigger. */
    private static final byte TRIGGER_MAX = (byte) 0xFF;

    /** Axis range sent to moonlight (signed 16-bit). */
    private static final short AXIS_MAX = Short.MAX_VALUE;  // 32767

    // ─── Live references ─────────────────────────────────────────────────────
    private NvConnection conn;
    private ControllerHandler controllerHandler;

    // ─── Controller state ─────────────────────────────────────────────────────
    /** Bit-field of all currently-pressed controller button flags. */
    private int    buttonFlags   = 0;
    private byte   leftTrigger   = 0;
    private byte   rightTrigger  = 0;
    private short  leftStickX    = 0;
    private short  leftStickY    = 0;
    private short  rightStickX   = 0;
    private short  rightStickY   = 0;

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    public MoonInputBridge() {}

    /** Call this once the NvConnection and ControllerHandler are ready. */
    public synchronized void attach(NvConnection conn, ControllerHandler handler) {
        this.conn = conn;
        this.controllerHandler = handler;
    }

    /** Release references; called from Game.onDestroy(). */
    public synchronized void detach() {
        conn = null;
        controllerHandler = null;
    }

    // ─── Button events ────────────────────────────────────────────────────────

    /**
     * Called by {@link MoonButton} when a virtual button is pressed or released.
     *
     * @param moonKeycode One of the {@code MoonKeycodes.MOON_BTN_*} constants or a VK code.
     * @param down        {@code true} = press, {@code false} = release.
     */
    public synchronized void sendButton(int moonKeycode, boolean down) {
        if (MoonKeycodes.isGamepadButton(moonKeycode)) {
            handleGamepadButton(moonKeycode, down);
        } else if (moonKeycode == MoonKeycodes.MOON_BTN_LT) {
            leftTrigger = down ? TRIGGER_MAX : 0;
            sendControllerState();
        } else if (moonKeycode == MoonKeycodes.MOON_BTN_RT) {
            rightTrigger = down ? TRIGGER_MAX : 0;
            sendControllerState();
        } else if (MoonKeycodes.isKeyboardKey(moonKeycode)) {
            sendKeyboard((short) moonKeycode, down);
        }
    }

    /** Helper: apply flag + flush. */
    private void handleGamepadButton(int moonKeycode, boolean down) {
        int flag = MoonKeycodes.toControllerFlag(moonKeycode);
        if (flag == 0) return;
        if (down) buttonFlags |= flag;
        else      buttonFlags &= ~flag;
        sendControllerState();
    }

    // ─── Analog input ─────────────────────────────────────────────────────────

    /**
     * Set the left analog stick position.
     *
     * @param x Normalized [-1.0, 1.0] horizontal value.
     * @param y Normalized [-1.0, 1.0] vertical value (positive = down).
     */
    public synchronized void setLeftStick(float x, float y) {
        leftStickX = normalize(x);
        leftStickY = normalize(y);
        sendControllerState();
    }

    /**
     * Set the right analog stick position.
     *
     * @param x Normalized [-1.0, 1.0] horizontal value.
     * @param y Normalized [-1.0, 1.0] vertical value (positive = down).
     */
    public synchronized void setRightStick(float x, float y) {
        rightStickX = normalize(x);
        rightStickY = normalize(y);
        sendControllerState();
    }

    /**
     * Set the analog left trigger value.
     *
     * @param value Normalized [0.0, 1.0].
     */
    public synchronized void setLeftTrigger(float value) {
        leftTrigger = (byte)(value * 255f);
        sendControllerState();
    }

    /**
     * Set the analog right trigger value.
     *
     * @param value Normalized [0.0, 1.0].
     */
    public synchronized void setRightTrigger(float value) {
        rightTrigger = (byte)(value * 255f);
        sendControllerState();
    }

    // ─── Internal helpers ────────────────────────────────────────────────────

    /** Convert a normalized float axis [-1,1] to a signed short for Moonlight. */
    private static short normalize(float v) {
        if (v >  1f) v =  1f;
        if (v < -1f) v = -1f;
        return (short)(v * AXIS_MAX);
    }

    /** Push the complete controller state via ControllerHandler. */
    private void sendControllerState() {
        if (controllerHandler == null) return;
        controllerHandler.reportOscState(
                buttonFlags,
                leftStickX,  leftStickY,
                rightStickX, rightStickY,
                leftTrigger, rightTrigger
        );
    }

    /** Send a keyboard key press/release to the host. */
    private void sendKeyboard(short vkCode, boolean down) {
        if (conn == null) return;
        // Build the moonlight key by applying the GFE prefix (0x80xx)
        short moonKey = (short)(0x8000 | vkCode);
        byte action = down ? KeyboardPacket.KEY_DOWN : KeyboardPacket.KEY_UP;
        conn.sendKeyboardInput(moonKey, action, (byte) 0, (byte) 0);
    }

    /** Reset all axes and buttons to zero (e.g. when overlay is hidden). */
    public synchronized void resetAll() {
        buttonFlags  = 0;
        leftTrigger  = 0;
        rightTrigger = 0;
        leftStickX   = 0;
        leftStickY   = 0;
        rightStickX  = 0;
        rightStickY  = 0;
        sendControllerState();
    }
}
