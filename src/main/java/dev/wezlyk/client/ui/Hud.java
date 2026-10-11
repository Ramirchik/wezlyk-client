package dev.wezlyk.client.ui;

import dev.wezlyk.client.hud.HudManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/** HUD entry point registered with Fabric; the elements live in dev.wezlyk.client.hud. */
public final class Hud {
    private Hud() {}

    public static void render(DrawContext c, RenderTickCounter tick) {
        HudManager.render(c);
    }
}
