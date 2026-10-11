package dev.wezlyk.client.render;

import dev.wezlyk.client.module.Modules;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Registers the 3D effects: one world-render hook for all of them, one tick hook for their state. */
public final class WorldFx {
    private WorldFx() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(JumpCircle::tick);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(WorldFx::render);
    }

    private static void render(WorldRenderContext ctx) {
        if (!Modules.JUMP_CIRCLE.enabled() && !Modules.CHINA_HAT.enabled() && !Modules.PREDICTIONS.enabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        MatrixStack stack = ctx.matrixStack();
        if (stack == null || mc.player == null || mc.world == null) return;

        Vec3d cam = ctx.camera().getPos();
        Matrix4f m = stack.peek().getPositionMatrix();
        float td = ctx.tickCounter().getTickDelta(true);

        Gfx.begin3d();
        try {
            JumpCircle.render(m, cam.x, cam.y, cam.z, td);
            ChinaHat.render(mc, m, cam.x, cam.y, cam.z, td);
            Predictions.render(mc, m, cam.x, cam.y, cam.z, td);
        } finally {
            Gfx.end3d();
        }
    }
}
