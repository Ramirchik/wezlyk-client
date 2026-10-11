package dev.wezlyk.client.ui;

import net.minecraft.client.gui.DrawContext;

/**
 * The one switch widget used by the whole ClickGUI (module rows, settings window, linked HUD toggles).
 * It only draws: the caller passes the animated state (0..1) and the animated press amount (0..1).
 * Colours come from the active theme through Glass, so a theme change recolours it immediately.
 */
public final class Toggle {
    public static final int W = 26;
    public static final int H = 14;

    private Toggle() {}

    /**
     * @param on    animated state, 0 = off, 1 = on (any value in between while moving)
     * @param press animated press amount, 0 = idle, 1 = held down
     */
    public static void draw(DrawContext c, int x, int y, float on, float press) {
        float e = UiMotion.ease(on);
        int track = Glass.mix(Glass.SWITCH_OFF, Glass.SWITCH_ON, e);
        if (press > 0.01f) {
            track = Glass.mix(track, Glass.TEXT, 0.10f * press); // subtle tint, no glow
        }
        Glass.round(c, x, y, W, H, H / 2, track);

        int knobW = 10 + Math.round(2f * press); // the knob stretches slightly while pressed
        int travel = W - 4 - knobW;
        int kx = x + 2 + Math.round(travel * e);
        Glass.round(c, kx, y + 2, knobW, H - 4, (H - 4) / 2, Glass.mix(Glass.KNOB_OFF, Glass.KNOB_ON, e));
    }
}
