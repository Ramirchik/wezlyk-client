package dev.wezlyk.client.hud;

import dev.wezlyk.client.module.Modules;
import dev.wezlyk.client.theme.Theme;
import dev.wezlyk.client.theme.Themes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Oval pill, top centre by default: client name and (optionally) the FPS counter. */
public final class WatermarkElement extends HudElement {
    private static final String NAME = "Wezlyk Client";
    private static final int H = 16;

    public WatermarkElement() {
        super("watermark", Modules.HUD_WATERMARK, 1, 0, 0, 6);
    }

    private static String fps() {
        return MinecraftClient.getInstance().getCurrentFps() + " FPS";
    }

    @Override
    public int width(TextRenderer tr) {
        int w = 12 + tr.getWidth(NAME);
        if (Modules.WATERMARK_FPS.get()) {
            w += 13 + tr.getWidth(fps());
        }
        return w;
    }

    @Override
    public int height() {
        return H;
    }

    @Override
    public void draw(DrawContext c, TextRenderer tr, int x, int y, boolean editing) {
        Theme t = Themes.current();
        int w = width(tr);
        plate(c, x, y, w, H, H / 2);
        int tx = x + 6 + 2;
        c.drawText(tr, NAME, tx, y + (H - 8) / 2, t.accent(), false);
        if (Modules.WATERMARK_FPS.get()) {
            int sx = tx + tr.getWidth(NAME) + 6;
            c.fill(sx, y + 4, sx + 1, y + H - 4, t.hudBorder());
            c.drawText(tr, fps(), sx + 7, y + (H - 8) / 2, t.hudMuted(), false);
        }
    }
}
