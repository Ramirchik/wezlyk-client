package dev.wezlyk.client.mixin;

import dev.wezlyk.client.module.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.Window;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    /**
     * Aspect Ratio: the world projection is built with the window's aspect. Rescaling its X axis by
     * windowAspect / targetAspect renders the 3D image as if the window had the target aspect, while
     * the window, GUI, crosshair and HUD keep their normal size and layout (they are not drawn with
     * this matrix). The crosshair stays exact because the scaling is symmetric around the screen centre.
     */
    @Inject(method = "getBasicProjectionMatrix", at = @At("RETURN"))
    private void wezlyk$applyAspectRatio(CallbackInfoReturnable<Matrix4f> cir) {
        double target = Modules.aspectTarget();
        if (target <= 0.0) {
            return;
        }
        Window window = MinecraftClient.getInstance().getWindow();
        double windowAspect = (double) window.getFramebufferWidth() / Math.max(1, window.getFramebufferHeight());
        Matrix4f matrix = cir.getReturnValue();
        matrix.m00((float) (matrix.m00() * windowAspect / target));
    }
}
