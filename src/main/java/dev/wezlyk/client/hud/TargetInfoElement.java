package dev.wezlyk.client.hud;

import dev.wezlyk.client.fake.FakePlayerManager;
import dev.wezlyk.client.module.Modules;
import dev.wezlyk.client.theme.Theme;
import dev.wezlyk.client.theme.Themes;
import dev.wezlyk.client.ui.Glass;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Locale;

/** Small rounded panel: name, "18 / 20 HP" and a health bar of the last player hit. */
public final class TargetInfoElement extends HudElement {
    private static final int H = 32;

    public TargetInfoElement() {
        super("target_info", Modules.HUD_TARGET, 1, 1, 0, 40);
    }

    private static String fmt(float v) {
        return Math.abs(v - Math.round(v)) < 0.05f ? Integer.toString(Math.round(v)) : String.format(Locale.ROOT, "%.1f", v);
    }

    private static String hpText(PlayerEntity p) {
        return p == null ? "-- / -- HP" : fmt(p.getHealth()) + " / " + fmt(p.getMaxHealth()) + " HP";
    }

    private static String nameText(PlayerEntity p) {
        if (p == null) return "Target";
        return FakePlayerManager.isFake(p) ? "Fake Player (test)" : p.getName().getString();
    }

    @Override
    public boolean hasContent() {
        return TargetTracker.current() != null;
    }

    @Override
    public int width(TextRenderer tr) {
        PlayerEntity p = TargetTracker.current();
        return Math.max(110, Math.max(tr.getWidth(nameText(p)), tr.getWidth(hpText(p))) + 20);
    }

    @Override
    public int height() {
        return H;
    }

    @Override
    public void draw(DrawContext c, TextRenderer tr, int x, int y, boolean editing) {
        PlayerEntity p = TargetTracker.current();
        Theme t = Themes.current();
        int w = width(tr);
        plate(c, x, y, w, H, Glass.RADIUS);
        c.drawText(tr, nameText(p), x + 10, y + 6, t.hudText(), false);
        // Without a real target (editing preview) only placeholders are shown, never made-up health.
        c.drawText(tr, hpText(p), x + 10, y + 17, t.hudMuted(), false);
        int bx = x + 10, by = y + H - 6, bw = w - 20;
        Glass.round(c, bx, by, bw, 3, 1, t.hudBorder());
        if (p != null) {
            float ratio = Math.max(0f, Math.min(1f, p.getHealth() / Math.max(1f, p.getMaxHealth())));
            int fw = Math.round(bw * ratio);
            if (fw > 0) {
                Glass.round(c, bx, by, Math.max(2, fw), 3, 1, t.accent());
            }
        }
    }
}
