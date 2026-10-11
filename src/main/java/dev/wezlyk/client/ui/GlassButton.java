package dev.wezlyk.client.ui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Minimal pill button, drawn by hand so it matches the glass style and follows the theme. */
public final class GlassButton {
    public int x, y, w, h;
    public String label;
    private final Runnable action;
    private final Anim hover = new Anim(0);

    public GlassButton(String label, Runnable action) {
        this.label = label;
        this.action = action;
    }

    public GlassButton bounds(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        return this;
    }

    public boolean contains(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public void render(DrawContext c, TextRenderer tr, int mx, int my) {
        float t = hover.to(contains(mx, my) ? 1f : 0f);
        Glass.round(c, x, y, w, h, Glass.RADIUS, Glass.mix(Glass.BTN, Glass.BTN_HOVER, t));
        Glass.ring(c, x, y, w, h, Glass.RADIUS, Glass.mix(Glass.BTN_BORDER, Glass.BTN_BORDER_HOVER, t));
        Glass.centered(c, tr, label, x + w / 2, y + (h - 8) / 2, Glass.mix(Glass.BTN_TEXT, Glass.TEXT, t));
    }

    public boolean click(double mx, double my) {
        if (contains(mx, my)) {
            action.run();
            return true;
        }
        return false;
    }
}
