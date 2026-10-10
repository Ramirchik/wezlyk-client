package dev.wezlyk.client.ui;

import dev.wezlyk.client.account.Accounts;
import dev.wezlyk.client.config.Config;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Plain list of profiles: the launcher account on top, saved offline profiles below. */
public class AccountManagerScreen extends Screen {
    private static final int ROW = 26;
    private static final String LAUNCHER_KEY = "\0launcher";

    private final Screen parent;
    private final long opened = System.nanoTime();
    private final Map<String, Anim> hovers = new HashMap<>();
    private TextFieldWidget field;
    private GlassButton done, add;
    private int px, py, pw, ph, listTop, listBottom, fieldY, fieldW;
    private int scroll;
    private boolean error;

    public AccountManagerScreen(Screen parent) {
        super(Text.literal("Accounts"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        pw = Math.min(300, width - 24);
        ph = Math.min(height - 16, 250);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        listTop = py + 34 + 28 + 6;
        fieldY = py + ph - 48;
        listBottom = fieldY - 6;
        fieldW = pw - 24 - 44 - 6;

        done = new GlassButton("Done", this::close).bounds(px + pw - 12 - 44, py + 8, 44, 18);
        add = new GlassButton("Add", this::addCurrent).bounds(px + pw - 12 - 44, fieldY, 44, 22);

        field = new TextFieldWidget(textRenderer, px + 12 + 8, fieldY + 7, fieldW - 16, 10, Text.literal("Name"));
        field.setDrawsBackground(false);
        field.setMaxLength(16);
        field.setPlaceholder(Text.literal("Offline name"));
        addSelectableChild(field);
        setFocused(field);
        field.setFocused(true);
        clampScroll();
    }

    private void addCurrent() {
        String name = field.getText().trim();
        if (Accounts.add(name)) {
            Accounts.select(name);
            field.setText("");
            error = false;
            clampScroll();
        } else {
            error = true;
        }
    }

    private void clampScroll() {
        int count = Config.get().accounts.size();
        int max = Math.max(0, count * ROW - (listBottom - listTop));
        scroll = Math.max(0, Math.min(scroll, max));
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // no vanilla blur
    }

    private float hover(String key, boolean on) {
        return hovers.computeIfAbsent(key, k -> new Anim(0)).to(on ? 1f : 0f);
    }

    private void card(DrawContext c, int x, int y, int w, int h, boolean active, float hov,
                      String name, String tag, boolean removable, int mx, int my) {
        Glass.round(c, x, y, w, h, Glass.RADIUS - 1, Glass.mix(0xFF1A1A1B, 0xFF222325, hov));
        if (active) {
            Glass.ring(c, x, y, w, h, Glass.RADIUS - 1, 0xFF5B616A);
        }
        int ty = y + (h - 8) / 2;
        int right = x + w - 10;
        if (removable) {
            boolean overX = mx >= x + w - 26 && mx < x + w && my >= y && my < y + h;
            Glass.centered(c, textRenderer, "×", x + w - 15, ty, overX ? Glass.TEXT : Glass.MUTED);
            right = x + w - 28;
        }
        int tagW = textRenderer.getWidth(tag);
        Glass.text(c, textRenderer, tag, right - tagW, ty, active ? Glass.ACCENT : Glass.MUTED);
        Glass.text(c, textRenderer, textRenderer.trimToWidth(name, right - tagW - x - 24), x + 10, ty, Glass.TEXT);
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        c.fill(0, 0, width, height, Glass.BG);
        c.fillGradient(0, 0, width, height / 2, 0xFF101012, Glass.BG);
        Glass.panel(c, px, py, pw, ph);

        Glass.text(c, textRenderer, "Accounts", px + 14, py + 13, Glass.TEXT);
        done.render(c, textRenderer, mx, my);
        c.fill(px + 10, py + 33, px + pw - 10, py + 34, 0x1FFFFFFF);

        // Launcher account (read-only identity, click to restore)
        int cx = px + 10, cw = pw - 20;
        boolean overL = mx >= cx && mx < cx + cw && my >= py + 34 + 2 && my < py + 34 + 28;
        card(c, cx, py + 36, cw, 24, Accounts.launcherActive(), hover(LAUNCHER_KEY, overL),
            Accounts.launcherName(), Accounts.launcherActive() ? "Active" : "Launcher", false, mx, my);

        // Offline profiles
        List<String> names = Config.get().accounts;
        if (names.isEmpty()) {
            Glass.centered(c, textRenderer, "No offline profiles", px + pw / 2, listTop + 8, Glass.MUTED);
        }
        c.enableScissor(px + 6, listTop, px + pw - 6, listBottom);
        for (int i = 0; i < names.size(); i++) {
            int ry = listTop + i * ROW - scroll;
            if (ry + ROW < listTop || ry > listBottom) continue;
            String n = names.get(i);
            boolean over = my >= listTop && my < listBottom && mx >= cx && mx < cx + cw && my >= ry && my < ry + ROW - 2;
            card(c, cx, ry, cw, ROW - 2, Accounts.isSelected(n), hover(n, over),
                n, Accounts.isSelected(n) ? "Active" : "Offline", true, mx, my);
        }
        c.disableScissor();

        // Add row
        int fx = px + 12;
        Glass.round(c, fx, fieldY, fieldW, 22, Glass.RADIUS, 0xFF101011);
        Glass.ring(c, fx, fieldY, fieldW, 22, Glass.RADIUS, field.isFocused() ? 0xFF4A4F57 : 0xFF262628);
        field.render(c, mx, my, delta);
        add.render(c, textRenderer, mx, my);

        String hint = error
            ? "3-16 characters: letters, digits, _"
            : "Offline profiles only. Microsoft sign-in stays in your launcher.";
        Glass.text(c, textRenderer, textRenderer.trimToWidth(hint, pw - 24), px + 12, py + ph - 18,
            error ? Glass.ACCENT : 0xFF5F5F64);

        Glass.fadeIn(c, width, height, opened, 200f);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (done.click(mx, my) || add.click(mx, my)) return true;

            boolean inField = mx >= px + 12 && mx < px + 12 + fieldW && my >= fieldY && my < fieldY + 22;
            setFocused(inField ? field : null);
            field.setFocused(inField);
            if (inField) {
                field.mouseClicked(mx, my, button);
                return true;
            }

            int cx = px + 10, cw = pw - 20;
            if (mx >= cx && mx < cx + cw && my >= py + 36 && my < py + 60) {
                Accounts.useLauncher();
                return true;
            }

            List<String> names = Config.get().accounts;
            if (my >= listTop && my < listBottom && mx >= cx && mx < cx + cw) {
                int idx = (int) ((my - listTop + scroll) / ROW);
                if (idx >= 0 && idx < names.size()) {
                    String n = names.get(idx);
                    if (mx >= cx + cw - 26) {
                        Accounts.remove(n);
                        clampScroll();
                    } else {
                        Accounts.select(n);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        scroll -= (int) (vertical * ROW);
        clampScroll();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && field.isFocused()) {
            addCurrent();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
