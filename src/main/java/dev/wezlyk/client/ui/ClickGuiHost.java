package dev.wezlyk.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

/**
 * Single entry point for opening the ClickGUI (key bind of the ClickGUI module, the vanilla key
 * binding used by FCL, the pause-menu button). Guarantees that only one window exists.
 */
public final class ClickGuiHost {
    private ClickGuiHost() {}

    public static void open(MinecraftClient mc, Screen parent) {
        if (mc.currentScreen instanceof ClickGuiScreen) return;
        mc.setScreen(new ClickGuiScreen(parent));
    }
}
