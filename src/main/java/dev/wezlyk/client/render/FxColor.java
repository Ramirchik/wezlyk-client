package dev.wezlyk.client.render;

import dev.wezlyk.client.setting.ColorSetting;
import dev.wezlyk.client.setting.ModeSetting;
import dev.wezlyk.client.ui.Glass;

/** Resolves the "Color: Auto / Custom" pair used by every render module. */
public final class FxColor {
    private FxColor() {}

    /** 0xRRGGBB: the theme accent for Auto, the picked colour for Custom. */
    public static int resolve(ModeSetting mode, ColorSetting custom) {
        if (mode.get().equals("Custom")) return custom.rgb() & 0xFFFFFF;
        return Glass.ACCENT & 0xFFFFFF;
    }
}
