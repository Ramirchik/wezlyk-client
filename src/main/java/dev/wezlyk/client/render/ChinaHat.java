package dev.wezlyk.client.render;

import dev.wezlyk.client.module.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

/**
 * Translucent 3D cone above the local player's head. Real geometry (apex, side faces, base disc),
 * drawn with depth test and no depth write so the faces blend without flicker. Only shown from
 * outside the head (third person / freecam style views), because in first person it would sit
 * inside the camera.
 */
public final class ChinaHat {
    private static final int SEGMENTS = 32;
    private static final int RIBS = 8;

    private ChinaHat() {}

    public static void render(MinecraftClient mc, Matrix4f m, double camX, double camY, double camZ, float tickDelta) {
        if (!Modules.CHINA_HAT.enabled()) return;
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.options.getPerspective().isFirstPerson()) return;

        double size = Modules.HAT_SIZE.value();
        double radius = 0.42 * size;
        double height = 0.34 * size;
        double px = MathHelper.lerp(tickDelta, p.prevX, p.getX());
        double py = MathHelper.lerp(tickDelta, p.prevY, p.getY());
        double pz = MathHelper.lerp(tickDelta, p.prevZ, p.getZ());
        double baseY = py + p.getHeight() - 0.05 * size - camY;
        double cx = px - camX, cz = pz - camZ;
        double apexY = baseY + height;

        int rgb = FxColor.resolve(Modules.HAT_COLOR_MODE, Modules.HAT_COLOR);
        float fill = (float) (Modules.HAT_OPACITY.value() / 100.0);
        int sideCol = Gfx.argb(rgb, fill);
        int apexCol = Gfx.argb(rgb, Math.min(1f, fill * 1.15f));
        int baseCol = Gfx.argb(rgb, fill * 0.6f);

        // side faces: fan from the apex
        Gfx.begin(VertexFormat.DrawMode.TRIANGLE_FAN);
        Gfx.v(m, cx, apexY, cz, apexCol);
        for (int i = 0; i <= SEGMENTS; i++) {
            double a = (Math.PI * 2.0) * i / SEGMENTS;
            Gfx.v(m, cx + Math.cos(a) * radius, baseY, cz + Math.sin(a) * radius, sideCol);
        }
        Gfx.draw();

        // base disc, so the hat does not look hollow from below
        Gfx.begin(VertexFormat.DrawMode.TRIANGLE_FAN);
        Gfx.v(m, cx, baseY, cz, baseCol);
        for (int i = 0; i <= SEGMENTS; i++) {
            double a = (Math.PI * 2.0) * i / SEGMENTS;
            Gfx.v(m, cx + Math.cos(a) * radius, baseY, cz + Math.sin(a) * radius, baseCol);
        }
        Gfx.draw();

        if (Modules.HAT_OUTLINE.get()) {
            int lineCol = Gfx.argb(rgb, (float) (Modules.HAT_OUTLINE_OPACITY.value() / 100.0));
            double dist = Math.sqrt(cx * cx + baseY * baseY + cz * cz);
            double half = Gfx.pixelSize(Math.max(0.5, dist), mc.options.getFov().getValue(),
                mc.getWindow().getFramebufferHeight()) * 1.0;
            Gfx.begin(VertexFormat.DrawMode.QUADS);
            double lx = cx + radius, lz = cz;
            for (int i = 1; i <= SEGMENTS; i++) {
                double a = (Math.PI * 2.0) * i / SEGMENTS;
                double nx = cx + Math.cos(a) * radius, nz = cz + Math.sin(a) * radius;
                Gfx.ribbon(m, lx, baseY, lz, nx, baseY, nz, half, lineCol);
                lx = nx;
                lz = nz;
            }
            for (int i = 0; i < RIBS; i++) {
                double a = (Math.PI * 2.0) * i / RIBS;
                Gfx.ribbon(m, cx, apexY, cz, cx + Math.cos(a) * radius, baseY, cz + Math.sin(a) * radius, half, lineCol);
            }
            Gfx.draw();
        }
    }
}
