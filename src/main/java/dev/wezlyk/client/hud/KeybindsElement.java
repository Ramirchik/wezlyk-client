package dev.wezlyk.client.hud;

import dev.wezlyk.client.module.Module;
import dev.wezlyk.client.module.Modules;
import dev.wezlyk.client.theme.Theme;
import dev.wezlyk.client.theme.Themes;
import dev.wezlyk.client.ui.Glass;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Lists enabled modules that have a key assigned. Recomputed every frame, so it follows toggles. */
public final class KeybindsElement extends HudElement {
    private static final String TITLE = "Keybinds";
    private static final int ROW = 11;

    public KeybindsElement() {
        super("keybinds", Modules.KEYBINDS, 2, 0, -6, 40);
    }

    private static List<Module> rows() {
        List<Module> out = new ArrayList<>();
        for (Module m : Modules.all()) {
            if (m.enabled() && m.hasBind()) out.add(m);
        }
        return out;
    }

    private static String key(Module m) {
        return "[" + m.bindName() + "]";
    }

    @Override
    public boolean hasContent() {
        return !rows().isEmpty();
    }

    @Override
    public int width(TextRenderer tr) {
        int w = tr.getWidth(TITLE);
        List<Module> rows = rows();
        if (rows.isEmpty()) {
            w = Math.max(w, tr.getWidth("No binds"));
        }
        for (Module m : rows) {
            w = Math.max(w, tr.getWidth(m.name) + 12 + tr.getWidth(key(m)));
        }
        return w + 16;
    }

    @Override
    public int height() {
        return 18 + Math.max(1, rows().size()) * ROW;
    }

    @Override
    public void draw(DrawContext c, TextRenderer tr, int x, int y, boolean editing) {
        Theme t = Themes.current();
        int w = width(tr);
        List<Module> rows = rows();
        plate(c, x, y, w, height(), Glass.RADIUS);
        c.drawText(tr, TITLE, x + 8, y + 5, t.hudMuted(), false);
        int ry = y + 17;
        if (rows.isEmpty()) {
            c.drawText(tr, "No binds", x + 8, ry, t.hudMuted(), false);
            return;
        }
        for (Module m : rows) {
            c.drawText(tr, m.name, x + 8, ry, t.hudText(), false);
            String k = key(m);
            c.drawText(tr, k, x + w - 8 - tr.getWidth(k), ry, t.hudMuted(), false);
            ry += ROW;
        }
    }
}
