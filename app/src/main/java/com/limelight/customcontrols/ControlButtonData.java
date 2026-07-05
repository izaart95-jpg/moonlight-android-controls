package com.limelight.customcontrols;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Serializable data describing a single on-screen button.
 * Positions are stored as fractions [0,1] of the screen dimension so layouts
 * scale correctly across different display sizes.
 */
public class ControlButtonData {

    // ─── Appearance ──────────────────────────────────────────────────────────
    /** Display label painted on the button. */
    public String label;

    /** Background fill colour (ARGB). */
    public int bgColor = 0x99000000;

    /** Text / border colour (ARGB). */
    public int fgColor = 0xFFFFFFFF;

    /** Corner radius as a fraction of the smaller button dimension (0 = square, 1 = pill). */
    public float cornerRadius = 0.3f;

    /** Opacity multiplier applied on top of bgColor alpha [0,1]. */
    public float opacity = 1f;

    // ─── Position & size (fractions of screen width/height) ──────────────────
    /** Left edge as fraction of screen width. */
    public float x = 0.1f;

    /** Top edge as fraction of screen height. */
    public float y = 0.1f;

    /** Button width as fraction of screen width. */
    public float w = 0.07f;

    /** Button height as fraction of screen height. */
    public float h = 0.10f;

    // ─── Input ───────────────────────────────────────────────────────────────
    /**
     * Array of keycodes to send when this button is pressed.
     * Each entry is a {@link MoonKeycodes} constant or a VK key code.
     */
    public int[] keycodes;

    /** When true the button behaves as a toggle (press once = on, press again = off). */
    public boolean toggle = false;

    // ─── Constructors ────────────────────────────────────────────────────────

    public ControlButtonData() {
        this("BTN", new int[0]);
    }

    public ControlButtonData(String label, int[] keycodes) {
        this.label   = label;
        this.keycodes = (keycodes != null) ? keycodes : new int[0];
    }

    // ─── JSON round-trip ─────────────────────────────────────────────────────

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("label",        label);
        obj.put("bgColor",      bgColor);
        obj.put("fgColor",      fgColor);
        obj.put("cornerRadius", cornerRadius);
        obj.put("opacity",      opacity);
        obj.put("x",            x);
        obj.put("y",            y);
        obj.put("w",            w);
        obj.put("h",            h);
        obj.put("toggle",       toggle);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keycodes.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(keycodes[i]);
        }
        obj.put("keycodes", sb.toString());
        return obj;
    }

    public static ControlButtonData fromJson(JSONObject obj) throws JSONException {
        ControlButtonData d = new ControlButtonData();
        d.label        = obj.optString("label", "BTN");
        d.bgColor      = obj.optInt("bgColor",      0x99000000);
        d.fgColor      = obj.optInt("fgColor",      0xFFFFFFFF);
        d.cornerRadius = (float) obj.optDouble("cornerRadius", 0.3);
        d.opacity      = (float) obj.optDouble("opacity",      1.0);
        d.x            = (float) obj.optDouble("x", 0.1);
        d.y            = (float) obj.optDouble("y", 0.1);
        d.w            = (float) obj.optDouble("w", 0.07);
        d.h            = (float) obj.optDouble("h", 0.10);
        d.toggle       = obj.optBoolean("toggle", false);

        String kc = obj.optString("keycodes", "");
        if (kc.isEmpty()) {
            d.keycodes = new int[0];
        } else {
            String[] parts = kc.split(",");
            d.keycodes = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                try { d.keycodes[i] = Integer.parseInt(parts[i].trim()); }
                catch (NumberFormatException ignored) {}
            }
        }
        return d;
    }

    // ─── Utility ─────────────────────────────────────────────────────────────

    /** Deep copy. */
    public ControlButtonData copy() {
        ControlButtonData c = new ControlButtonData(label, keycodes.clone());
        c.bgColor      = bgColor;
        c.fgColor      = fgColor;
        c.cornerRadius = cornerRadius;
        c.opacity      = opacity;
        c.x = x; c.y = y; c.w = w; c.h = h;
        c.toggle       = toggle;
        return c;
    }
}

