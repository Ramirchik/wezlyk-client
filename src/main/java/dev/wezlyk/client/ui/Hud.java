package dev.wezlyk.client.ui;

import dev.wezlyk.client.hud.HudManager;
import dev.wezlyk.client.module.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/** HUD entry point registered with Fabric; the elements live in dev.wezlyk.client.hud. */
public final class Hud {
    private Hud() {}

    public static void render(DrawContext c, RenderTickCounter tick) {
        // Key binds are also polled once per frame, so a short tap is not lost between two 50 ms ticks.
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            Modules.tickBinds(mc);
        }
        HudManager.render(c);
    }
}
