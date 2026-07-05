package com.limelight.customcontrols;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Serializable data describing a virtual analog stick.
 * Uses the same fractional-screen-size convention as {@link ControlButtonData}.
 */
public class AnalogStickData {

    // ─── Which stick ─────────────────────────────────────────────────────────
    /**
     * {@link MoonKeycodes#MOON_STICK_LEFT} or {@link MoonKeycodes#MOON_STICK_RIGHT}.
     */
    public int stickId = MoonKeycodes.MOON_STICK_LEFT;

    // ─── Position & size ─────────────────────────────────────────────────────
    public float x = 0.05f;
    public float y = 0.40f;
    /** Diameter as fraction of screen height (used for both width and height). */
    public float diameter = 0.20f;

    // ─── Appearance ──────────────────────────────────────────────────────────
    public int   baseColor = 0x66FFFFFF;
    public int   knobColor = 0xAAFFFFFF;
    public float opacity   = 1f;

    // ─── Behaviour ───────────────────────────────────────────────────────────
    /**
     * When true the stick base follows the initial touch point (relative mode).
     * When false the base stays fixed (absolute mode).
     */
    public boolean relativeMode = false;

    /** Deadzone as a fraction of the stick radius [0, 0.5]. */
    public float deadzone = 0.15f;

    // ─── Constructors ────────────────────────────────────────────────────────

    public AnalogStickData() {}

    public AnalogStickData(int stickId, float x, float y, float diameter) {
        this.stickId  = stickId;
        this.x        = x;
        this.y        = y;
        this.diameter = diameter;
    }

    // ─── JSON round-trip ─────────────────────────────────────────────────────

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("stickId",      stickId);
        obj.put("x",            x);
        obj.put("y",            y);
        obj.put("diameter",     diameter);
        obj.put("baseColor",    baseColor);
        obj.put("knobColor",    knobColor);
        obj.put("opacity",      opacity);
        obj.put("relativeMode", relativeMode);
        obj.put("deadzone",     deadzone);
        return obj;
    }

    public static AnalogStickData fromJson(JSONObject obj) throws JSONException {
        AnalogStickData d = new AnalogStickData();
        d.stickId      = obj.optInt("stickId",      MoonKeycodes.MOON_STICK_LEFT);
        d.x            = (float) obj.optDouble("x",        0.05);
        d.y            = (float) obj.optDouble("y",        0.40);
        d.diameter     = (float) obj.optDouble("diameter", 0.20);
        d.baseColor    = obj.optInt("baseColor",    0x66FFFFFF);
        d.knobColor    = obj.optInt("knobColor",    0xAAFFFFFF);
        d.opacity      = (float) obj.optDouble("opacity",  1.0);
        d.relativeMode = obj.optBoolean("relativeMode", false);
        d.deadzone     = (float) obj.optDouble("deadzone", 0.15);
        return d;
    }

    /** Deep copy. */
    public AnalogStickData copy() {
        AnalogStickData c = new AnalogStickData(stickId, x, y, diameter);
        c.baseColor    = baseColor;
        c.knobColor    = knobColor;
        c.opacity      = opacity;
        c.relativeMode = relativeMode;
        c.deadzone     = deadzone;
        return c;
    }
}

