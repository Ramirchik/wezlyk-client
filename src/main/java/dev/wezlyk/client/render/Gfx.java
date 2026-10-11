package dev.wezlyk.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

/**
 * Small immediate-mode helper for translucent world geometry.
 * All coordinates are CAMERA-RELATIVE (world position minus camera position), matching the matrix
 * stack handed out by WorldRenderEvents. Thick lines are camera-facing quads, not GL lines, because
 * GL line width is unreliable on translated GL layers such as the ones used by FCL.
 * Render thread only; one batch can be open at a time.
 */
public final class Gfx {
    private static BufferBuilder buffer;
    private static int count;

    private Gfx() {}

    /** Translucent, depth-tested, no depth writes (so overlapping faces cannot flicker), no culling. */
    public static void begin3d() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
    }

    public static void end3d() {
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void begin(VertexFormat.DrawMode mode) {
        buffer = Tessellator.getInstance().begin(mode, VertexFormats.POSITION_COLOR);
        count = 0;
    }

    public static void v(Matrix4f m, double x, double y, double z, int argb) {
        buffer.vertex(m, (float) x, (float) y, (float) z).color(argb);
        count++;
    }

    /** Submits the open batch. An empty batch is dropped (BufferBuilder.end() throws on empty). */
    public static void draw() {
        if (buffer == null) return;
        if (count > 0) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
        buffer = null;
        count = 0;
    }

    /** Packs 0xRRGGBB plus an alpha in 0..1 into ARGB. */
    public static int argb(int rgb, float alpha) {
        int a = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    /**
     * A flat ribbon between two points that always faces the camera (camera sits at the origin).
     * Needs an open QUADS batch.
     */
    public static void ribbon(Matrix4f m, double x0, double y0, double z0, double x1, double y1, double z1,
                              double halfWidth, int argb) {
        double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        double mx = (x0 + x1) * 0.5, my = (y0 + y1) * 0.5, mz = (z0 + z1) * 0.5;
        // side = dir x toCamera-ish; the camera is the origin, so the view vector is the midpoint itself
        double sx = dy * mz - dz * my;
        double sy = dz * mx - dx * mz;
        double sz = dx * my - dy * mx;
        double len = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (len < 1.0e-9) return;
        double k = halfWidth / len;
        sx *= k;
        sy *= k;
        sz *= k;
        v(m, x0 - sx, y0 - sy, z0 - sz, argb);
        v(m, x0 + sx, y0 + sy, z0 + sz, argb);
        v(m, x1 + sx, y1 + sy, z1 + sz, argb);
        v(m, x1 - sx, y1 - sy, z1 - sz, argb);
    }

    /** World-space size of one screen pixel at the given distance (vertical FOV in degrees). */
    public static double pixelSize(double distance, double fovDegrees, int framebufferHeight) {
        double h = Math.max(1, framebufferHeight);
        return distance * 2.0 * Math.tan(Math.toRadians(fovDegrees) * 0.5) / h;
    }
}
