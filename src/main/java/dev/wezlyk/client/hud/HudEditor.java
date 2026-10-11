package dev.wezlyk.client.hud;

import dev.wezlyk.client.config.Autosave;
import dev.wezlyk.client.theme.Themes;
import dev.wezlyk.client.ui.Glass;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;

import java.util.List;

/**
 * HUD editing while the chat is open: drag enabled elements with the left mouse button.
 * <p>
 * The editor only listens to mouse events of the chat screen and cancels a click only when it lands
 * on a HUD element, so typing, sending messages and clicks elsewhere in the chat are untouched.
 * Nothing can be moved during normal play, because the listeners exist only on the chat screen.
 */
public final class HudEditor {
    private static HudElement dragging;
    private static HudElement selected;
    private static int offX, offY;

    private HudEditor() {}

    public static boolean active(MinecraftClient mc) {
        return mc.currentScreen instanceof ChatScreen;
    }

    private static int[] bounds(HudElement e) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        int w = e.width(tr);
        int h = e.height();
        return new int[]{e.x(sw, w), e.y(sh, h), w, h};
    }

    /**
     * Topmost element under the cursor. Visible elements win over switched-off ones (which are shown as
     * outlines in edit mode so they can be positioned before they are enabled).
     */
    private static HudElement hit(double mx, double my) {
        List<HudElement> all = HudManager.ELEMENTS;
        for (int pass = 0; pass < 2; pass++) {
            boolean wantEnabled = pass == 0;
            for (int i = all.size() - 1; i >= 0; i--) {
                HudElement e = all.get(i);
                if (e.shown() != wantEnabled) continue;
                int[] b = bounds(e);
                if (mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3]) {
                    return e;
                }
            }
        }
        return null;
    }

    private static void finishDrag() {
        if (dragging != null) {
            dragging = null;
            Autosave.markDirty();
        }
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof ChatScreen)) return;

            ScreenMouseEvents.allowMouseClick(screen).register((s, mx, my, button) -> {
                if (button != 0) return true;
                HudElement e = hit(mx, my);
                if (e == null) return true; // not ours: let the chat handle it
                int[] b = bounds(e);
                dragging = e;
                selected = e;
                offX = (int) mx - b[0];
                offY = (int) my - b[1];
                return false;
            });

            ScreenMouseEvents.allowMouseRelease(screen).register((s, mx, my, button) -> {
                if (button == 0) finishDrag();
                return true;
            });

            ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> render(ctx, mx, my));

            ScreenEvents.remove(screen).register(s -> {
                finishDrag();
                selected = null;
            });
        });
    }

    private static void render(DrawContext c, int mx, int my) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        if (dragging != null) {
            dragging.dragTo(mx - offX, my - offY, sw, sh, dragging.width(tr), dragging.height());
        }
        HudElement hovered = dragging != null ? dragging : hit(mx, my);
        int accent = Themes.current().accent() & 0xFFFFFF;

        for (HudElement e : HudManager.ELEMENTS) {
            boolean on = e.shown();
            int[] b = bounds(e);
            int x = b[0] - 2, y = b[1] - 2, w = b[2] + 4, h = b[3] + 4;
            boolean hot = e == hovered || e == selected;
            if (hot) {
                Glass.round(c, x, y, w, h, 4, (0x26 << 24) | accent);
                Glass.ring(c, x, y, w, h, 4, (0xDD << 24) | accent);
            } else {
                Glass.ring(c, x, y, w, h, 4, ((on ? 0x66 : 0x33) << 24) | accent);
            }
            int ty = y >= 11 ? y - 10 : y + h + 2;
            c.drawText(tr, on ? e.label() : e.label() + " (off)", x, ty, on ? 0xFFFFFFFF : 0xFFB0B0B0, true);
        }
    }
}
