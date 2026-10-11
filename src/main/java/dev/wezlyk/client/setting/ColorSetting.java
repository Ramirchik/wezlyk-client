package dev.wezlyk.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A real HSL colour: hue, saturation and lightness, each 0..1. get()/rgb() return 0xRRGGBB.
 * The three components are stored (not just the RGB result) so the palette round-trips exactly.
 */
public final class ColorSetting extends Setting<Integer> {
    private final float defH, defS, defL;
    private float h, s, l;

    public ColorSetting(String name, float h, float s, float l) {
        super(name, hsl(h, s, l));
        this.defH = h;
        this.defS = s;
        this.defL = l;
        this.h = h;
        this.s = s;
        this.l = l;
    }

    public float hue() {
        return h;
    }

    public float saturation() {
        return s;
    }

    public float lightness() {
        return l;
    }

    public int rgb() {
        return value;
    }

    /** component: 0 = H, 1 = S, 2 = L. */
    public float component(int component) {
        return component == 0 ? h : component == 1 ? s : l;
    }

    public void setComponent(int component, float v) {
        v = clamp(v);
        if (component == 0) h = v;
        else if (component == 1) s = v;
        else l = v;
        set(hsl(h, s, l));
    }

    @Override
    public void reset() {
        super.reset();
        h = defH;
        s = defS;
        l = defL;
    }

    private static float clamp(float v) {
        return Float.isNaN(v) ? 0f : Math.max(0f, Math.min(1f, v));
    }

    /** HSL (all 0..1) to 0xRRGGBB. No java.awt: it may be missing on Android runtimes. */
    public static int hsl(float h, float s, float l) {
        h = clamp(h);
        s = clamp(s);
        l = clamp(l);
        float c = (1f - Math.abs(2f * l - 1f)) * s;
        float hp = (h >= 1f ? 0f : h) * 6f;
        float x = c * (1f - Math.abs(hp % 2f - 1f));
        float r, g, b;
        int sector = (int) Math.floor(hp);
        switch (sector) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        float m = l - c / 2f;
        int ri = Math.round((r + m) * 255f);
        int gi = Math.round((g + m) * 255f);
        int bi = Math.round((b + m) * 255f);
        return (ri << 16) | (gi << 8) | bi;
    }

    @Override
    public JsonElement save() {
        JsonObject o = new JsonObject();
        o.addProperty("h", h);
        o.addProperty("s", s);
        o.addProperty("l", l);
        return o;
    }

    @Override
    public void load(JsonElement element) {
        if (!element.isJsonObject()) return;
        JsonObject o = element.getAsJsonObject();
        Float fh = num(o, "h"), fs = num(o, "s"), fl = num(o, "l");
        if (fh != null && fs != null && fl != null) {
            h = clamp(fh);
            s = clamp(fs);
            l = clamp(fl);
            value = hsl(h, s, l);
        }
    }

    private static Float num(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return null;
        float f = e.getAsFloat();
        return Float.isNaN(f) ? null : f;
    }
}
