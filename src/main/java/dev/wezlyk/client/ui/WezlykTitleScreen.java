package dev.wezlyk.client.ui;

import dev.wezlyk.client.account.Accounts;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Black, quiet title screen with the four vanilla actions. */
public class WezlykTitleScreen extends Screen {
    private final long opened = System.nanoTime();
    private final List<GlassButton> buttons = new ArrayList<>();
    private GlassButton accountChip;
    private int logoY;
    private int logoScale;

    public WezlykTitleScreen() {
        super(Text.literal("Wezlyk Client"));
    }

    @Override
    protected void init() {
        buttons.clear();
        int bw = Math.min(180, width - 40);
        int bh = height < 240 ? 18 : 22;
        int gap = 6;
        logoScale = height < 260 ? 2 : 3;
        int logoBlock = 9 * logoScale + 26;
        int total = 4 * bh + 3 * gap;
        int top = Math.max(8, (height - (logoBlock + 16 + total)) / 2);
        logoY = top;
        int bx = (width - bw) / 2;
        int by = top + logoBlock + 16;

        String[] labels = {"Singleplayer", "Multiplayer", "Options", "Quit"};
        Runnable[] actions = {
            () -> client.setScreen(new SelectWorldScreen(this)),
            () -> client.setScreen(client.options.skipMultiplayerWarning
                ? new MultiplayerScreen(this)
                : new MultiplayerWarningScreen(this)),
            () -> client.setScreen(new OptionsScreen(this, client.options)),
            () -> client.scheduleStop()
        };
        for (int i = 0; i < labels.length; i++) {
            buttons.add(new GlassButton(labels[i], actions[i]).bounds(bx, by + i * (bh + gap), bw, bh));
        }

        String name = Accounts.activeName();
        int cw = Math.min(width - 16, textRenderer.getWidth(name) + 24);
        accountChip = new GlassButton(textRenderer.trimToWidth(name, cw - 24), () -> client.setScreen(new AccountManagerScreen(this)))
            .bounds(8, height - 26, cw, 18);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // no vanilla panorama or blur
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        c.fill(0, 0, width, height, Glass.BG);
        c.fillGradient(0, 0, width, height / 2, Glass.BG_TOP, Glass.BG);

        // Wordmark
        String word = "WEZLYK";
        int spacing = 3;
        int tw = spacing * (word.length() - 1);
        for (char ch : word.toCharArray()) {
            tw += textRenderer.getWidth(String.valueOf(ch));
        }
        c.getMatrices().push();
        c.getMatrices().translate((width - tw * logoScale) / 2f, logoY, 0f);
        c.getMatrices().scale(logoScale, logoScale, 1f);
        int cx = 0;
        for (char ch : word.toCharArray()) {
            String s = String.valueOf(ch);
            c.drawText(textRenderer, s, cx, 0, Glass.TEXT, false);
            cx += textRenderer.getWidth(s) + spacing;
        }
        c.getMatrices().pop();

        // Sub-label, letter-spaced
        String sub = "CLIENT";
        int sw = 4 * (sub.length() - 1);
        for (char ch : sub.toCharArray()) {
            sw += textRenderer.getWidth(String.valueOf(ch));
        }
        int sx = (width - sw) / 2;
        int sy = logoY + 9 * logoScale + 8;
        for (char ch : sub.toCharArray()) {
            String s = String.valueOf(ch);
            c.drawText(textRenderer, s, sx, sy, Glass.MUTED, false);
            sx += textRenderer.getWidth(s) + 4;
        }

        // Thin silver accent line with a slow pulse
        double t = (System.nanoTime() - opened) / 1_000_000_000.0;
        int alpha = 70 + (int) (40 * Math.sin(t * 1.4));
        c.fill(width / 2 - 10, sy + 14, width / 2 + 10, sy + 15, (alpha << 24) | (Glass.ACCENT & 0xFFFFFF));

        for (GlassButton b : buttons) {
            b.render(c, textRenderer, mx, my);
        }
        accountChip.render(c, textRenderer, mx, my);

        String ver = "Minecraft 1.21.4";
        Glass.text(c, textRenderer, ver, width - textRenderer.getWidth(ver) - 8, height - 16, Glass.FAINT);

        Glass.fadeIn(c, width, height, opened, 220f);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (GlassButton b : buttons) {
                if (b.click(mx, my)) return true;
            }
            if (accountChip.click(mx, my)) return true;
        }
        return super.mouseClicked(mx, my, button);
    }
}
