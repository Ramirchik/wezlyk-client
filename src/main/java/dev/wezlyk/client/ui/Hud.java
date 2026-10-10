package dev.wezlyk.client.ui;

import dev.wezlyk.client.module.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/** Quiet text-only HUD lines for the enabled visual modules. */
public final class Hud {
    private Hud() {}

    public static void render(DrawContext c, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden || mc.getDebugHud().shouldShowDebugHud()) {
            return;
        }
        int y = 6;
        if (Modules.FPS.enabled()) {
            c.drawText(mc.textRenderer, mc.getCurrentFps() + " FPS", 6, y, 0xFFE6E6E8, true);
            y += 11;
        }
        if (Modules.COORDS.enabled()) {
            String s = mc.player.getBlockX() + "  " + mc.player.getBlockY() + "  " + mc.player.getBlockZ();
            c.drawText(mc.textRenderer, s, 6, y, 0xFFB9C1CC, true);
        }
    }
}
