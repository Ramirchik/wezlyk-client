package dev.wezlyk.client.hud;

import dev.wezlyk.client.module.Modules;
import dev.wezlyk.client.theme.Themes;
import dev.wezlyk.client.ui.Glass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** The original coordinates readout, now a movable plate. */
public final class CoordsElement extends HudElement {
    public CoordsElement() {
        super("coords", Modules.HUD_COORDS, 0, 0, 6, 24);
    }

    private static String text() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return "-  -  -";
        return mc.player.getBlockX() + "  " + mc.player.getBlockY() + "  " + mc.player.getBlockZ();
    }

    @Override
    public int width(TextRenderer tr) {
        return tr.getWidth(text()) + 12;
    }

    @Override
    public int height() {
        return 14;
    }

    @Override
    public void draw(DrawContext c, TextRenderer tr, int x, int y, boolean editing) {
        plate(c, x, y, width(tr), height(), Glass.RADIUS);
        c.drawText(tr, text(), x + 6, y + 3, Themes.current().hudText(), false);
    }
}
