package dev.wezlyk.client;

import dev.wezlyk.client.account.Accounts;
import dev.wezlyk.client.config.Config;
import dev.wezlyk.client.module.Modules;
import dev.wezlyk.client.ui.ClickGuiScreen;
import dev.wezlyk.client.ui.Hud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class WezlykClient implements ClientModInitializer {
    public static KeyBinding openGui;

    @Override
    public void onInitializeClient() {
        Config.get();
        Modules.load();

        openGui = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.wezlyk.clickgui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.wezlyk"));

        ClientLifecycleEvents.CLIENT_STARTED.register(Accounts::init);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGui.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new ClickGuiScreen(null));
                }
            }
            if (Modules.SPRINT.enabled() && client.player != null && client.currentScreen == null
                && client.options.forwardKey.isPressed()
                && !client.player.isSneaking()
                && !client.player.horizontalCollision
                && client.player.getHungerManager().getFoodLevel() > 6) {
                client.player.setSprinting(true);
            }
        });

        HudRenderCallback.EVENT.register(Hud::render);

        // Touch-friendly entry point (FCL has no easy Right Shift): small button in the pause menu.
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof GameMenuScreen) {
                Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("Wezlyk"),
                    b -> client.setScreen(new ClickGuiScreen(screen)))
                    .dimensions(w - 62, 6, 56, 20).build());
            }
        });
    }
}
