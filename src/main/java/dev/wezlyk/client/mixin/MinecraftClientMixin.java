package dev.wezlyk.client.mixin;

import dev.wezlyk.client.ui.WezlykTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    /** Swaps the vanilla title screen for the Wezlyk one wherever the game opens it. */
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen wezlyk$replaceTitleScreen(Screen screen) {
        if (screen instanceof TitleScreen) {
            return new WezlykTitleScreen();
        }
        return screen;
    }
}
