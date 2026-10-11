package dev.wezlyk.client.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.wezlyk.client.module.Modules;
import dev.wezlyk.client.setting.BoolSetting;
import dev.wezlyk.client.theme.Theme;
import dev.wezlyk.client.theme.Themes;
import dev.wezlyk.client.ui.Glass;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * One independent HUD element with its own position.
 * <p>
 * Position = anchor (left/centre/right, top/middle/bottom) plus a pixel offset from that anchor,
 * in GUI-scaled pixels. The anchor is chosen from the screen third the element sits in, so an element
 * stays where the player put it when the window size or GUI scale changes, and is clamped on screen.
 * An element is visible exactly when the master HUD module is on AND its own switch in the HUD menu is on.
 */
public abstract class HudElement {
    public final String id;
    public final BoolSetting toggle;
    private final int defAx, defAy, defDx, defDy;
    private int ax, ay, dx, dy;

    protected HudElement(String id, BoolSetting toggle, int ax, int ay, int dx, int dy) {
        this.id = id;
        this.toggle = toggle;
        this.defAx = ax;
        this.defAy = ay;
        this.defDx = dx;
        this.defDy = dy;
        resetPosition();
    }

    public final String label() {
        return toggle.name;
    }

    /** Master HUD switch and this element's own switch. */
    public final boolean shown() {
        return Modules.HUD.enabled() && toggle.get();
    }

    public abstract int width(TextRenderer tr);

    public abstract int height();

    /** Draws the element at x, y. When editing is true a placeholder preview may be shown. */
    public abstract void draw(DrawContext c, TextRenderer tr, int x, int y, boolean editing);

    /** False when there is nothing to show during normal play (editing mode ignores this). */
    public boolean hasContent() {
        return true;
    }

    public int x(int sw, int w) {
        return clamp(ax * (sw - w) / 2 + dx, 0, Math.max(0, sw - w));
    }

    public int y(int sh, int h) {
        return clamp(ay * (sh - h) / 2 + dy, 0, Math.max(0, sh - h));
    }

    /** Moves the element so its top-left corner is at (newX, newY) and re-derives anchor and offset. */
    public void dragTo(int newX, int newY, int sw, int sh, int w, int h) {
        newX = clamp(newX, 0, Math.max(0, sw - w));
        newY = clamp(newY, 0, Math.max(0, sh - h));
        int cx = newX + w / 2;
        int cy = newY + h / 2;
        ax = cx < sw / 3 ? 0 : (cx > sw * 2 / 3 ? 2 : 1);
        ay = cy < sh / 3 ? 0 : (cy > sh * 2 / 3 ? 2 : 1);
        dx = newX - ax * (sw - w) / 2;
        dy = newY - ay * (sh - h) / 2;
    }

    public void resetPosition() {
        ax = defAx;
        ay = defAy;
        dx = defDx;
        dy = defDy;
    }

    public JsonObject save() {
        JsonObject o = new JsonObject();
        o.addProperty("ax", ax);
        o.addProperty("ay", ay);
        o.addProperty("dx", dx);
        o.addProperty("dy", dy);
        return o;
    }

    public void load(JsonObject o) {
        ax = read(o, "ax", ax, 0, 2);
        ay = read(o, "ay", ay, 0, 2);
        dx = read(o, "dx", dx, -4000, 4000);
        dy = read(o, "dy", dy, -4000, 4000);
    }

    private static int read(JsonObject o, String key, int fallback, int lo, int hi) {
        try {
            JsonElement e = o.get(key);
            if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
                return clamp(e.getAsInt(), lo, hi);
            }
        } catch (Exception ignored) {
            // use fallback
        }
        return fallback;
    }

    protected static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Themed background plate with a thin border. r = h / 2 gives an oval pill. */
    protected static void plate(DrawContext c, int x, int y, int w, int h, int r) {
        Theme t = Themes.current();
        Glass.round(c, x, y, w, h, r, t.hudBg());
        Glass.ring(c, x, y, w, h, r, t.hudBorder());
    }
}
