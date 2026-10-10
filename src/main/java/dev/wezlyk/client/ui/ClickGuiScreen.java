package dev.wezlyk.client.ui;

import dev.wezlyk.client.module.Category;
import dev.wezlyk.client.module.Module;
import dev.wezlyk.client.module.Modules;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Compact centred panel: two tabs, one row per module, one switch per row. */
public class ClickGuiScreen extends Screen {
    private static final int HEADER = 34;
    private static final int ROW = 26;

    private final Screen parent;
    private final long opened = System.nanoTime();
    private Category tab = Category.VISUAL;
    private int px, py, pw, ph;

    public ClickGuiScreen(Screen parent) {
        super(Text.literal("Wezlyk"));
        this.parent = parent;
    }

    private void layout(float slide) {
        pw = Math.min(300, width - 24);
        int maxRows = 0;
        for (Category cat : Category.values()) {
            maxRows = Math.max(maxRows, Modules.in(cat).size());
        }
        ph = HEADER + 8 + maxRows * ROW + 8;
        px = (width - pw) / 2;
        py = (height - ph) / 2 + Math.round(slide);
    }

    /** Right-aligned tab pill: returns {x, width}. */
    private int[] tabBounds(int index) {
        Category[] cats = Category.values();
        int x = px + pw - 12;
        for (int k = cats.length - 1; k >= 0; k--) {
            int w = textRenderer.getWidth(cats[k].label) + 16;
            x -= w;
            if (k == index) {
                return new int[]{x, w};
            }
            x -= 4;
        }
        return new int[]{x, 0};
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // no vanilla blur
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        float t = Math.min(1f, (System.nanoTime() - opened) / 180_000_000f);
        float e = 1f - (1f - t) * (1f - t);
        layout((1f - e) * 8f);

        c.fill(0, 0, width, height, (((int) (0x99 * e)) << 24) | 0x080808);
        Glass.panel(c, px, py, pw, ph);

        Glass.text(c, textRenderer, "Wezlyk", px + 14, py + 13, Glass.TEXT);

        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            int[] b = tabBounds(i);
            boolean sel = cats[i] == tab;
            if (sel) {
                Glass.round(c, b[0], py + 8, b[1], 18, Glass.RADIUS - 1, 0xFF242527);
            }
            Glass.centered(c, textRenderer, cats[i].label, b[0] + b[1] / 2, py + 13, sel ? Glass.TEXT : Glass.MUTED);
        }
        c.fill(px + 10, py + HEADER - 1, px + pw - 10, py + HEADER, 0x1FFFFFFF);

        List<Module> mods = Modules.in(tab);
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int rx = px + 6;
            int ry = py + HEADER + 6 + i * ROW;
            int rw = pw - 12;
            int rh = ROW - 2;
            boolean hov = mx >= rx && mx < rx + rw && my >= ry && my < ry + rh;
            float h = m.hover.to(hov ? 1f : 0f);
            if (h > 0.01f) {
                Glass.round(c, rx, ry, rw, rh, Glass.RADIUS - 1, (((int) (0x18 * h)) << 24) | 0xFFFFFF);
            }
            Glass.text(c, textRenderer, m.name, rx + 10, ry + (rh - 8) / 2, Glass.TEXT);

            float a = m.anim.to(m.enabled() ? 1f : 0f);
            int sw = 26, sh = 14;
            int sx = rx + rw - 10 - sw;
            int sy = ry + (rh - sh) / 2;
            Glass.round(c, sx, sy, sw, sh, 7, Glass.mix(0xFF2A2A2C, 0xFF7C8591, a));
            int kx = sx + 2 + Math.round((sw - sh) * a);
            Glass.round(c, kx, sy + 2, sh - 4, sh - 4, 5, Glass.mix(0xFF8C8C91, 0xFFF4F6F8, a));
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            layout(0f);
            Category[] cats = Category.values();
            for (int i = 0; i < cats.length; i++) {
                int[] b = tabBounds(i);
                if (mx >= b[0] && mx < b[0] + b[1] && my >= py + 8 && my < py + 26) {
                    tab = cats[i];
                    return true;
                }
            }
            List<Module> mods = Modules.in(tab);
            for (int i = 0; i < mods.size(); i++) {
                int ry = py + HEADER + 6 + i * ROW;
                if (mx >= px + 6 && mx < px + pw - 6 && my >= ry && my < ry + ROW - 2) {
                    mods.get(i).toggle();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
