package dev.wezlyk.client.mixin;

import dev.wezlyk.client.module.Modules;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
    /**
     * Module key binds are driven by the real key event instead of polling the key state. Polling
     * missed short taps (shorter than the poll interval, common with FCL's on-screen keys) and, after a
     * screen closed itself on the same key, saw the still-held key as a new press and reopened it.
     */
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void wezlyk$onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (action == GLFW.GLFW_PRESS && Modules.onKey(MinecraftClient.getInstance(), window, key)) {
            ci.cancel();
        }
    }
}
