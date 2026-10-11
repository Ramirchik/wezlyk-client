package dev.wezlyk.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/** Owns the HUD elements and draws the enabled ones. Drawing order = list order. */
public final class HudManager {
    public static final List<HudElement> ELEMENTS = List.of(
        new FpsElement(),
        new CoordsElement(),
        new WatermarkElement(),
        new KeybindsElement(),
        new TargetInfoElement()
    );

    private HudManager() {}

    public static void render(DrawContext c) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) {
            return;
        }
        boolean editing = HudEditor.active(mc);
        if (!editing && mc.getDebugHud().shouldShowDebugHud()) {
            return;
        }
        TextRenderer tr = mc.textRenderer;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        for (HudElement e : ELEMENTS) {
            if (!e.module.enabled()) continue;
            if (!editing && !e.hasContent()) continue;
            int w = e.width(tr);
            int h = e.height();
            e.draw(c, tr, e.x(sw, w), e.y(sh, h), editing);
        }
    }
}
