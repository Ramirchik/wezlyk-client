package dev.wezlyk.client.ui;

import dev.wezlyk.client.config.Profiles;
import dev.wezlyk.client.module.Category;
import dev.wezlyk.client.module.Module;
import dev.wezlyk.client.module.Modules;
import dev.wezlyk.client.setting.BoolSetting;
import dev.wezlyk.client.setting.LabelSetting;
import dev.wezlyk.client.setting.ModeSetting;
import dev.wezlyk.client.setting.Setting;
import dev.wezlyk.client.setting.SliderSetting;
import dev.wezlyk.client.theme.Theme;
import dev.wezlyk.client.theme.Themes;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compact centred panel with five sections: Combat, Movement, Render (modules), Themes and Configs.
 * <p>
 * Mouse: LMB toggles a module (or applies a theme / loads a profile), RMB opens the module's settings
 * window, MMB starts key binding. Keyboard: arrows move, Enter/Space activates, B binds, S opens settings,
 * Right Shift or Esc closes. Everything it shows is read live from the modules, settings and themes.
 */
public class ClickGuiScreen extends Screen {
    private enum Page {
        COMBAT("Combat", Category.COMBAT),
        MOVEMENT("Movement", Category.MOVEMENT),
        RENDER("Render", Category.RENDER),
        THEMES("Themes", null),
        CONFIGS("Configs", null);

        final String label;
        final Category category;

        Page(String label, Category category) {
            this.label = label;
            this.category = category;
        }
    }

    private record Row(Setting<?> setting, int y, int h) {}

    private static final Page[] PAGES = Page.values();
    private static final int ROW = 24;
    private static final int TABS_Y = 24;
    private static Page lastPage = Page.RENDER;

    private final Screen parent;
    private final long opened = System.nanoTime();
    private final Anim pill = new Anim(lastPage.ordinal());
    private final Map<String, Anim> hovers = new HashMap<>();
    private final List<String> profiles = new ArrayList<>();

    private Page page = lastPage;
    private int sel;
    private boolean kbFocus;
    private int scroll;

    private Module waiting;      // module that is waiting for a key press
    private Module popup;        // module whose settings window is open
    private long popupAt;
    private SliderSetting dragging;
    private int dragX, dragW;

    private String message = "";
    private long messageUntil;

    private TextFieldWidget field;
    private GlassButton save;
    private int px, py, pw, ph, cx0, cx1, cy0, cy1, footY, fieldY, tabW;

    public ClickGuiScreen(Screen parent) {
        super(Text.literal("Wezlyk"));
        this.parent = parent;
    }

    // ---- layout ---------------------------------------------------------------------------

    @Override
    protected void init() {
        pw = Math.min(360, width - 16);
        ph = Math.min(height - 12, 240);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        cx0 = px + 8;
        cx1 = px + pw - 8;
        cy0 = py + 50;
        footY = py + ph - 20;
        cy1 = footY - 4;
        tabW = (pw - 20) / PAGES.length;
        fieldY = cy1 - 22;

        String keep = field != null ? field.getText() : "";
        field = new TextFieldWidget(textRenderer, cx0 + 8, fieldY + 7, fieldWidth() - 16, 10, Text.literal("Profile"));
        field.setDrawsBackground(false);
        field.setMaxLength(24);
        field.setPlaceholder(Text.literal("Profile name"));
        field.setText(keep);
        field.setFocused(false);
        save = new GlassButton("Save", this::saveProfile).bounds(cx1 - 46, fieldY, 46, 22);

        refreshProfiles();
        clampScroll();
    }

    private int fieldWidth() {
        return (cx1 - cx0) - 46 - 6;
    }

    private int listBottom() {
        return page == Page.CONFIGS ? fieldY - 4 : cy1;
    }

    private List<Module> modules() {
        return page.category == null ? List.of() : Modules.in(page.category);
    }

    private int count() {
        if (page == Page.THEMES) return Themes.ALL.size();
        if (page == Page.CONFIGS) return profiles.size();
        return modules().size();
    }

    private void clampScroll() {
        int max = Math.max(0, count() * ROW - (listBottom() - cy0));
        scroll = Math.max(0, Math.min(scroll, max));
    }

    private int rowAt(double mx, double my) {
        if (mx < cx0 || mx >= cx1 || my < cy0 || my >= listBottom()) return -1;
        int local = (int) (my - cy0) + scroll;
        if (local % ROW >= ROW - 2) return -1;
        int i = local / ROW;
        return i >= 0 && i < count() ? i : -1;
    }

    private void refreshProfiles() {
        profiles.clear();
        profiles.addAll(Profiles.list());
    }

    private void switchPage(Page p) {
        page = p;
        lastPage = p;
        waiting = null;
        popup = null;
        dragging = null;
        scroll = 0;
        kbFocus = false;
        field.setFocused(false);
        sel = 0;
        if (p == Page.THEMES) {
            sel = Math.max(0, Themes.ALL.indexOf(Themes.current()));
        } else if (p == Page.CONFIGS) {
            refreshProfiles();
            sel = Math.max(0, profiles.indexOf(Profiles.active()));
        }
        clampScroll();
    }

    private void shiftPage(int dir) {
        int n = PAGES.length;
        switchPage(PAGES[((page.ordinal() + dir) % n + n) % n]);
    }

    private void say(String text) {
        message = text;
        messageUntil = System.currentTimeMillis() + 2600;
    }

    private float hover(String key, boolean on) {
        return hovers.computeIfAbsent(key, k -> new Anim(0)).to(on ? 1f : 0f);
    }

    // ---- rendering ------------------------------------------------------------------------

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
        c.fill(0, 0, width, height, Glass.alpha(Glass.DIM, 0.6f * e));

        c.getMatrices().push();
        c.getMatrices().translate(0f, (1f - e) * 8f, 0f);

        Glass.panel(c, px, py, pw, ph);
        Glass.text(c, textRenderer, "Wezlyk", px + 14, py + 9, Glass.TEXT);

        // Tabs with a sliding highlight
        int tx0 = px + 10;
        float pv = pill.to(page.ordinal());
        Glass.round(c, tx0 + Math.round(pv * tabW), py + TABS_Y, tabW, 18, Glass.RADIUS - 1, Glass.TAB_SEL);
        for (int i = 0; i < PAGES.length; i++) {
            String label = textRenderer.trimToWidth(PAGES[i].label, tabW - 6);
            Glass.centered(c, textRenderer, label, tx0 + i * tabW + tabW / 2, py + TABS_Y + 5,
                PAGES[i] == page ? Glass.TEXT : Glass.MUTED);
        }
        c.fill(px + 10, py + TABS_Y + 22, px + pw - 10, py + TABS_Y + 23, Glass.SEP);

        if (page == Page.THEMES) {
            renderThemes(c, mx, my);
        } else if (page == Page.CONFIGS) {
            renderConfigs(c, mx, my, delta);
        } else {
            renderModules(c, mx, my);
        }

        // Footer: hint, hovered module description, key binding prompt or the last message
        c.fill(px + 10, footY, px + pw - 10, footY + 1, Glass.SEP);
        boolean emph = System.currentTimeMillis() < messageUntil || (waiting != null && popup == null);
        Glass.text(c, textRenderer, textRenderer.trimToWidth(footer(mx, my), pw - 28), px + 14, footY + 6,
            emph ? Glass.ACCENT : Glass.MUTED);

        if (popup != null) {
            renderPopup(c, mx, my);
        }

        c.getMatrices().pop();
    }

    private String footer(int mx, int my) {
        if (System.currentTimeMillis() < messageUntil) return message;
        if (popup != null) return popup.name + " settings   Esc or click outside to close";
        if (waiting != null) return "Press a key for " + waiting.name + "   Esc cancel   Del clear";
        if (page.category != null) {
            List<Module> mods = modules();
            int idx = kbFocus ? sel : rowAt(mx, my);
            if (idx >= 0 && idx < mods.size()) {
                Module m = mods.get(idx);
                return m.name + " - " + m.description;
            }
            return "LMB toggle   RMB settings   MMB bind";
        }
        if (page == Page.THEMES) return "Click a theme to apply it everywhere";
        return "A profile stores modules, binds, settings, theme and HUD layout";
    }

    private void drawSwitch(DrawContext c, int x, int y, float a) {
        Glass.round(c, x, y, 26, 14, 7, Glass.mix(Glass.SWITCH_OFF, Glass.SWITCH_ON, a));
        Glass.round(c, x + 2 + Math.round(12 * a), y + 2, 10, 10, 5, Glass.mix(Glass.KNOB_OFF, Glass.KNOB_ON, a));
    }

    private void renderModules(DrawContext c, int mx, int my) {
        List<Module> mods = modules();
        if (mods.isEmpty()) {
            Glass.centered(c, textRenderer, "No modules in this section", px + pw / 2, cy0 + 12, Glass.MUTED);
            return;
        }
        int bottom = listBottom();
        int hoverIdx = popup == null ? rowAt(mx, my) : -1;
        int rw = cx1 - cx0;
        int rh = ROW - 2;
        c.enableScissor(cx0, cy0, cx1, bottom);
        for (int i = 0; i < mods.size(); i++) {
            int ry = cy0 + i * ROW - scroll;
            if (ry + ROW < cy0 || ry > bottom) continue;
            Module m = mods.get(i);
            boolean lit = popup == null && (i == hoverIdx || (kbFocus && i == sel));
            float h = m.hover.to(lit ? 1f : 0f);
            if (h > 0.01f) {
                Glass.round(c, cx0, ry, rw, rh, Glass.RADIUS - 1, Glass.alpha(Glass.CARD_HOVER, h));
            }

            int ty = ry + (rh - 8) / 2;
            int sx = cx1 - 10 - 26;
            float a = m.anim.to(m.enabled() ? 1f : 0f);
            drawSwitch(c, sx, ry + (rh - 14) / 2, a);

            int right = sx - 8;
            String tag = waiting == m ? "..." : (m.hasBind() ? "[" + m.bindName() + "]" : "");
            if (!tag.isEmpty()) {
                int tw = textRenderer.getWidth(tag);
                Glass.text(c, textRenderer, tag, right - tw, ty, waiting == m ? Glass.ACCENT : Glass.MUTED);
                right -= tw + 8;
            }
            Glass.text(c, textRenderer, textRenderer.trimToWidth(m.name, right - cx0 - 14), cx0 + 10, ty, Glass.TEXT);
            if (waiting == m) {
                Glass.ring(c, cx0, ry, rw, rh, Glass.RADIUS - 1, Glass.SEL_RING);
            }
        }
        c.disableScissor();
    }

    private void renderThemes(DrawContext c, int mx, int my) {
        int hoverIdx = popup == null ? rowAt(mx, my) : -1;
        int rw = cx1 - cx0;
        int rh = ROW - 2;
        c.enableScissor(cx0, cy0, cx1, listBottom());
        for (int i = 0; i < Themes.ALL.size(); i++) {
            int ry = cy0 + i * ROW - scroll;
            Theme th = Themes.ALL.get(i);
            boolean active = th == Themes.current();
            float h = hover("theme:" + th.name(), popup == null && (i == hoverIdx || (kbFocus && i == sel)));
            Glass.round(c, cx0, ry, rw, rh, Glass.RADIUS - 1, Glass.mix(Glass.CARD, Glass.CARD_HOVER, h));
            if (active) {
                Glass.ring(c, cx0, ry, rw, rh, Glass.RADIUS - 1, Glass.SEL_RING);
            }
            int ty = ry + (rh - 8) / 2;
            Glass.text(c, textRenderer, th.name(), cx0 + 10, ty, Glass.TEXT);

            // Swatches come from the theme being listed, so each row previews its own palette.
            int[] colors = {th.bg(), th.card(), th.accent(), th.text()};
            int sx = cx1 - 10 - colors.length * 14 + 4;
            for (int k = 0; k < colors.length; k++) {
                int x = sx + k * 14;
                Glass.round(c, x, ry + (rh - 10) / 2, 10, 10, 3, colors[k]);
                Glass.ring(c, x, ry + (rh - 10) / 2, 10, 10, 3, Glass.BORDER);
            }
            if (active) {
                String tag = "Active";
                Glass.text(c, textRenderer, tag, sx - 10 - textRenderer.getWidth(tag), ty, Glass.ACCENT);
            }
        }
        c.disableScissor();
    }

    private void renderConfigs(DrawContext c, int mx, int my, float delta) {
        int bottom = listBottom();
        int hoverIdx = popup == null ? rowAt(mx, my) : -1;
        String active = Profiles.active();
        int rw = cx1 - cx0;
        int rh = ROW - 2;
        c.enableScissor(cx0, cy0, cx1, bottom);
        for (int i = 0; i < profiles.size(); i++) {
            int ry = cy0 + i * ROW - scroll;
            if (ry + ROW < cy0 || ry > bottom) continue;
            String name = profiles.get(i);
            boolean isActive = name.equals(active);
            boolean lit = popup == null && (i == hoverIdx || (kbFocus && i == sel));
            float h = hover("profile:" + name, lit);
            Glass.round(c, cx0, ry, rw, rh, Glass.RADIUS - 1, Glass.mix(Glass.CARD, Glass.CARD_HOVER, h));
            if (isActive) {
                Glass.ring(c, cx0, ry, rw, rh, Glass.RADIUS - 1, Glass.SEL_RING);
            }
            int ty = ry + (rh - 8) / 2;
            int right = cx1 - 10;
            if (!name.equals(Profiles.DEFAULT)) {
                boolean overX = popup == null && mx >= cx1 - 26 && mx < cx1 && my >= ry && my < ry + rh;
                Glass.centered(c, textRenderer, "×", cx1 - 15, ty, overX ? Glass.TEXT : Glass.MUTED);
                right = cx1 - 28;
            }
            String tag = isActive ? "Active" : "Load";
            int tw = textRenderer.getWidth(tag);
            Glass.text(c, textRenderer, tag, right - tw, ty,
                isActive ? Glass.ACCENT : Glass.mix(Glass.FAINT, Glass.TEXT, h));
            Glass.text(c, textRenderer, textRenderer.trimToWidth(name, right - tw - cx0 - 24), cx0 + 10, ty, Glass.TEXT);
        }
        c.disableScissor();

        Glass.round(c, cx0, fieldY, fieldWidth(), 22, Glass.RADIUS, Glass.FIELD);
        Glass.ring(c, cx0, fieldY, fieldWidth(), 22, Glass.RADIUS, field.isFocused() ? Glass.SEL_RING : Glass.BORDER);
        field.render(c, mx, my, delta);
        save.render(c, textRenderer, mx, my);
    }

    // ---- settings window ------------------------------------------------------------------

    private static int rowHeight(Setting<?> s) {
        if (s instanceof ModeSetting) return 40;
        if (s instanceof SliderSetting) return 34;
        if (s instanceof LabelSetting) return 20;
        return 24;
    }

    private List<Row> popupRows() {
        List<Row> rows = new ArrayList<>();
        if (popup == null) return rows;
        int y = 30;
        for (Setting<?> s : popup.visibleSettings()) {
            int h = rowHeight(s);
            rows.add(new Row(s, y, h));
            y += h;
        }
        return rows;
    }

    private int popupW() {
        return Math.min(230, pw - 16);
    }

    private int popupH(List<Row> rows) {
        int end = 30;
        for (Row r : rows) {
            end = r.y() + r.h();
        }
        return end + 10;
    }

    private int popupX() {
        return px + (pw - popupW()) / 2;
    }

    private int popupY(int h) {
        return Math.max(py + 4, py + (ph - h) / 2);
    }

    private void openPopup(Module m) {
        popup = m;
        popupAt = System.nanoTime();
        dragging = null;
        waiting = null;
    }

    private void renderPopup(DrawContext c, int mx, int my) {
        List<Row> rows = popupRows();
        int w = popupW();
        int h = popupH(rows);
        int x = popupX();
        int y = popupY(h);
        float f = Math.min(1f, (System.nanoTime() - popupAt) / 140_000_000f);

        Glass.round(c, px, py, pw, ph, Glass.RADIUS + 2, Glass.alpha(Glass.DIM, 0.75f * f));
        Glass.round(c, x, y, w, h, Glass.RADIUS + 2, Glass.BG);
        Glass.panel(c, x, y, w, h);

        Glass.text(c, textRenderer, textRenderer.trimToWidth(popup.name, w - 40), x + 12, y + 9, Glass.TEXT);
        boolean overClose = mx >= x + w - 26 && mx < x + w && my >= y && my < y + 24;
        Glass.centered(c, textRenderer, "×", x + w - 15, y + 9, overClose ? Glass.TEXT : Glass.MUTED);
        c.fill(x + 10, y + 25, x + w - 10, y + 26, Glass.SEP);

        for (Row r : rows) {
            Setting<?> s = r.setting();
            int ry = y + r.y();
            int ty = ry + (r.h() - 8) / 2;
            if (s instanceof BoolSetting b) {
                Glass.text(c, textRenderer, b.name, x + 12, ty, Glass.TEXT);
                float a = hover("set:" + popup.name + ":" + b.name, b.get());
                drawSwitch(c, x + w - 12 - 26, ry + (r.h() - 14) / 2, a);
            } else if (s instanceof ModeSetting m) {
                Glass.text(c, textRenderer, m.name, x + 12, ry + 4, Glass.MUTED);
                int n = m.options.size();
                int cw = (w - 24) / n;
                for (int k = 0; k < n; k++) {
                    int chipX = x + 12 + k * cw;
                    boolean on = m.options.get(k).equals(m.get());
                    boolean over = mx >= chipX && mx < chipX + cw - 2 && my >= ry + 16 && my < ry + 34;
                    Glass.round(c, chipX, ry + 16, cw - 2, 18, Glass.RADIUS - 2,
                        on ? Glass.TAB_SEL : (over ? Glass.CARD_HOVER : Glass.CARD));
                    if (on) {
                        Glass.ring(c, chipX, ry + 16, cw - 2, 18, Glass.RADIUS - 2, Glass.SEL_RING);
                    }
                    Glass.centered(c, textRenderer, textRenderer.trimToWidth(m.options.get(k), cw - 6),
                        chipX + (cw - 2) / 2, ry + 21, on ? Glass.TEXT : Glass.MUTED);
                }
            } else if (s instanceof SliderSetting sl) {
                Glass.text(c, textRenderer, sl.name, x + 12, ry + 4, Glass.MUTED);
                String v = sl.display();
                Glass.text(c, textRenderer, v, x + w - 12 - textRenderer.getWidth(v), ry + 4, Glass.TEXT);
                int tx = x + 12;
                int tw = w - 24;
                float frac = (float) ((sl.value() - sl.min) / (sl.max - sl.min));
                frac = Math.max(0f, Math.min(1f, frac));
                Glass.round(c, tx, ry + 20, tw, 4, 2, Glass.SWITCH_OFF);
                Glass.round(c, tx, ry + 20, Math.max(4, Math.round(tw * frac)), 4, 2, Glass.SWITCH_ON);
                Glass.round(c, tx + Math.round((tw - 8) * frac), ry + 18, 8, 8, 4, Glass.KNOB_ON);
            } else if (s instanceof LabelSetting l) {
                Glass.text(c, textRenderer, l.name, x + 12, ty, Glass.MUTED);
                String v = textRenderer.trimToWidth(l.text(), w - 24 - textRenderer.getWidth(l.name) - 8);
                Glass.text(c, textRenderer, v, x + w - 12 - textRenderer.getWidth(v), ty, Glass.TEXT);
            }
        }
    }

    private boolean popupClick(double mx, double my, int button) {
        List<Row> rows = popupRows();
        int w = popupW();
        int h = popupH(rows);
        int x = popupX();
        int y = popupY(h);
        boolean inside = mx >= x && mx < x + w && my >= y && my < y + h;
        if (!inside) {
            popup = null;
            dragging = null;
            return true;
        }
        if (button != 0) return true;
        if (mx >= x + w - 26 && my < y + 24) {
            popup = null;
            return true;
        }
        for (Row r : rows) {
            int ry = y + r.y();
            if (my < ry || my >= ry + r.h()) continue;
            Setting<?> s = r.setting();
            if (s instanceof BoolSetting b) {
                b.toggle();
            } else if (s instanceof ModeSetting m) {
                if (my >= ry + 16 && my < ry + 34) {
                    int n = m.options.size();
                    int idx = (int) ((mx - (x + 12)) / ((w - 24) / n));
                    if (idx >= 0 && idx < n) {
                        m.choose(m.options.get(idx));
                    }
                }
            } else if (s instanceof SliderSetting sl) {
                dragging = sl;
                dragX = x + 12;
                dragW = w - 24;
                sliderTo(mx);
            }
            break;
        }
        return true;
    }

    private void sliderTo(double mx) {
        double f = Math.max(0.0, Math.min(1.0, (mx - dragX) / dragW));
        dragging.setSnapped(dragging.min + f * (dragging.max - dragging.min));
    }

    // ---- actions --------------------------------------------------------------------------

    private void startBind(Module m) {
        waiting = m;
        messageUntil = 0;
        kbFocus = false;
    }

    private void bindKey(int key) {
        Module m = waiting;
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            waiting = null;
            say("Binding cancelled");
            return;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
            m.setBind(GLFW.GLFW_KEY_UNKNOWN);
            waiting = null;
            say(m.name + ": bind cleared");
            return;
        }
        if (key == GLFW.GLFW_KEY_UNKNOWN) return;
        String problem = Modules.conflict(key, m, client);
        if (problem != null) {
            say(problem + " - press another key"); // stay in waiting mode
            return;
        }
        m.setBind(key);
        waiting = null;
        say(m.name + ": bound to " + m.bindName());
    }

    private void saveProfile() {
        String name = field.getText().trim();
        if (!Profiles.validName(name)) {
            say("Name: 1-24 characters, letters, digits, - and _");
            return;
        }
        if (Profiles.saveAs(name)) {
            field.setText("");
            refreshProfiles();
            sel = Math.max(0, profiles.indexOf(name));
            say("Saved " + name + " and made it active");
        } else {
            say("Could not write the profile file");
        }
    }

    private void loadProfile(String name) {
        if (Profiles.load(name)) {
            say("Loaded " + name);
        } else {
            say("Profile " + name + " is missing or damaged");
            refreshProfiles();
            clampScroll();
        }
    }

    private void deleteProfile(String name) {
        if (Profiles.delete(name)) {
            say("Deleted " + name);
        } else {
            say("Could not delete " + name);
        }
        refreshProfiles();
        sel = Math.min(sel, Math.max(0, profiles.size() - 1));
        clampScroll();
    }

    private void activate(int idx) {
        if (idx < 0 || idx >= count()) return;
        if (page == Page.THEMES) {
            Themes.set(Themes.ALL.get(idx));
        } else if (page == Page.CONFIGS) {
            loadProfile(profiles.get(idx));
        } else {
            modules().get(idx).toggle();
        }
    }

    private void move(int dir) {
        int n = count();
        if (n == 0) return;
        sel = Math.max(0, Math.min(n - 1, sel + dir));
        kbFocus = true;
        int top = sel * ROW;
        int viewH = listBottom() - cy0;
        if (top < scroll) {
            scroll = top;
        } else if (top + ROW > scroll + viewH) {
            scroll = top + ROW - viewH;
        }
        clampScroll();
    }

    // ---- input ----------------------------------------------------------------------------

    @Override
    public void mouseMoved(double mx, double my) {
        int idx = rowAt(mx, my);
        if (idx >= 0 && popup == null) {
            sel = idx;
            kbFocus = false;
        }
        super.mouseMoved(mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (waiting != null) {
            waiting = null;
            say("Binding cancelled");
            return true;
        }
        if (popup != null) {
            return popupClick(mx, my, button);
        }
        if (button == 0 && mx >= px + 10 && mx < px + 10 + tabW * PAGES.length
            && my >= py + TABS_Y && my < py + TABS_Y + 18) {
            switchPage(PAGES[(int) ((mx - px - 10) / tabW)]);
            return true;
        }

        int idx = rowAt(mx, my);
        if (page.category != null) {
            if (idx >= 0) {
                sel = idx;
                kbFocus = false;
                Module m = modules().get(idx);
                if (button == 0) {
                    m.toggle();
                } else if (button == 1) {
                    if (m.hasSettings()) {
                        openPopup(m);
                    } else {
                        say(m.name + " has no settings");
                    }
                } else if (button == 2) {
                    startBind(m);
                }
                return true;
            }
        } else if (page == Page.THEMES) {
            if (idx >= 0 && button == 0) {
                sel = idx;
                Themes.set(Themes.ALL.get(idx));
                return true;
            }
        } else if (button == 0) {
            if (save.click(mx, my)) return true;
            boolean inField = mx >= cx0 && mx < cx0 + fieldWidth() && my >= fieldY && my < fieldY + 22;
            field.setFocused(inField);
            if (inField) {
                field.mouseClicked(mx, my, button);
                return true;
            }
            if (idx >= 0) {
                sel = idx;
                String name = profiles.get(idx);
                if (!name.equals(Profiles.DEFAULT) && mx >= cx1 - 26) {
                    deleteProfile(name);
                } else {
                    loadProfile(name);
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null && button == 0) {
            sliderTo(mx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (popup != null) return true;
        scroll -= (int) (vertical * ROW);
        clampScroll();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (waiting != null) {
            bindKey(keyCode);
            return true;
        }
        if (popup != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_S) {
                popup = null;
                dragging = null;
            }
            return true;
        }
        if (page == Page.CONFIGS && field.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                saveProfile();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                close();
                return true;
            }
            // Typing: Right Shift must not close the screen while a capital letter is being entered.
            return field.keyPressed(keyCode, scanCode, modifiers);
        }

        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            shiftPage(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_TAB) {
            shiftPage(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            move(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            move(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
            kbFocus = true;
            activate(sel);
            return true;
        }
        if (page.category != null && sel >= 0 && sel < modules().size()) {
            Module m = modules().get(sel);
            if (keyCode == GLFW.GLFW_KEY_B) {
                kbFocus = true;
                startBind(m);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_S) {
                if (m.hasSettings()) {
                    openPopup(m);
                } else {
                    say(m.name + " has no settings");
                }
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (popup == null && waiting == null && page == Page.CONFIGS && field.isFocused()
            && field.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void removed() {
        Profiles.flush();
        super.removed();
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
