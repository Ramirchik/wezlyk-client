package dev.wezlyk.client;

import dev.wezlyk.client.account.Accounts;
import dev.wezlyk.client.config.Config;
import dev.wezlyk.client.config.Profiles;
import dev.wezlyk.client.fake.FakePlayerManager;
import dev.wezlyk.client.hud.HudEditor;
import dev.wezlyk.client.hud.TargetTracker;
import dev.wezlyk.client.module.Modules;
import dev.wezlyk.client.render.WorldFx;
import dev.wezlyk.client.ui.ClickGuiHost;
import dev.wezlyk.client.ui.Hud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

public class WezlykClient implements ClientModInitializer {
    public static KeyBinding openGui;

    @Override
    public void onInitializeClient() {
        Config.get();
        // Loads the active configuration profile (theme, modules, binds, settings, HUD layout).
        // On the first run it migrates the module states of the old single-file config.
        Profiles.init();

        openGui = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.wezlyk.clickgui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.wezlyk"));
        // The ClickGUI module owns the real key (default Right Shift, changeable in the menu). This extra
        // vanilla binding is unbound by default and exists so the key can be mapped in Controls (FCL).

        ClientLifecycleEvents.CLIENT_STARTED.register(Accounts::init);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Profiles.flush());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Profiles.tick();
            Modules.tickBinds(client);
            while (openGui.wasPressed()) {
                if (client.currentScreen == null) {
                    ClickGuiHost.open(client, null);
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

        // HUD editor: dragging elements while the chat is open (listeners exist only on ChatScreen).
        HudEditor.register();

        // 3D effects (Jump Circle, China Hat, Predictions) and the local Fake Player.
        WorldFx.register();
        FakePlayerManager.register(); // registered before the callback below: it consumes hits on the dummy

        // Target Info: remember the last player this client attacked.
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient && entity instanceof PlayerEntity target && target != player) {
                TargetTracker.hit(target);
            }
            return ActionResult.PASS;
        });

        // Touch-friendly entry point (FCL has no easy Right Shift): small button in the pause menu.
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof GameMenuScreen) {
                Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("Wezlyk"),
                    b -> ClickGuiHost.open(client, screen))
                    .dimensions(w - 62, 6, 56, 20).build());
            }
        });
    }
}
