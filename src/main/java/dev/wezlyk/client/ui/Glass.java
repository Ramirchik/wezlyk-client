package dev.wezlyk.client.ui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Drawing helpers for the Minimal Liquid Glass look.
 * Glass is imitated with simple fills (translucent graphite, a faint top sheen, a thin light border),
 * so there is no real blur pass and nothing that can destabilise rendering.
 */
public final class Glass {
    public static final int BG = 0xFF080808;
    public static final int PANEL = 0xE6151515;
    public static final int BORDER = 0xFF2B2B2D;
    public static final int TEXT = 0xFFF2F2F2;
    public static final int MUTED = 0xFF8C8C91;
    public static final int ACCENT = 0xFFB9C1CC;
    public static final int RADIUS = 6;

    private Glass() {}

    /** Filled rounded rectangle drawn as scanlines. */
    public static void round(DrawContext c, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            c.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
        c.fill(x, y + r, x + w, y + h - r, color);
    }

    /** One-pixel rounded outline. */
    public static void ring(DrawContext c, int x, int y, int w, int h, int r, int color) {
        r = Math.max(1, Math.min(r, Math.min(w, h) / 2));
        int[] in = new int[r];
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            in[i] = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
        }
        for (int i = 0; i < r; i++) {
            int a = in[i];
            int b = i == 0 ? r : Math.max(a + 1, in[i - 1]);
            c.fill(x + a, y + i, x + b, y + i + 1, color);
            c.fill(x + w - b, y + i, x + w - a, y + i + 1, color);
            c.fill(x + a, y + h - i - 1, x + b, y + h - i, color);
            c.fill(x + w - b, y + h - i - 1, x + w - a, y + h - i, color);
        }
        c.fill(x + r, y, x + w - r, y + 1, color);
        c.fill(x + r, y + h - 1, x + w - r, y + h, color);
        c.fill(x, y + r, x + 1, y + h - r, color);
        c.fill(x + w - 1, y + r, x + w, y + h - r, color);
    }

    /** Main glass surface: translucent graphite, soft top sheen, thin border. */
    public static void panel(DrawContext c, int x, int y, int w, int h) {
        int r = RADIUS + 2;
        round(c, x, y, w, h, r, PANEL);
        c.fillGradient(x + r, y + 1, x + w - r, y + 1 + Math.max(4, h / 5), 0x14FFFFFF, 0x00FFFFFF);
        c.fill(x + r, y + 1, x + w - r, y + 2, 0x22FFFFFF);
        ring(c, x, y, w, h, r, BORDER);
    }

    public static int mix(int a, int b, float t) {
        int out = 0;
        for (int s = 24; s >= 0; s -= 8) {
            int ca = (a >> s) & 255;
            int cb = (b >> s) & 255;
            out |= ((int) (ca + (cb - ca) * t) & 255) << s;
        }
        return out;
    }

    public static void text(DrawContext c, TextRenderer tr, String s, int x, int y, int color) {
        c.drawText(tr, s, x, y, color, false);
    }

    public static void centered(DrawContext c, TextRenderer tr, String s, int cx, int y, int color) {
        c.drawText(tr, s, cx - tr.getWidth(s) / 2, y, color, false);
    }

    /** Fade in from black over the given duration. */
    public static void fadeIn(DrawContext c, int w, int h, long openedNanos, float millis) {
        float t = Math.min(1f, (System.nanoTime() - openedNanos) / (millis * 1_000_000f));
        if (t < 1f) {
            c.fill(0, 0, w, h, ((int) ((1f - t) * 255f)) << 24);
        }
    }
}
