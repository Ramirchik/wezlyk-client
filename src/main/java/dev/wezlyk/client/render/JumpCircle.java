package dev.wezlyk.client.render;

import dev.wezlyk.client.module.Modules;
import dev.wezlyk.client.ui.UiMotion;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.world.ClientWorld;
import org.joml.Matrix4f;

/**
 * Ring that appears under the feet when the player jumps.
 * - A circle is created only on a real take-off (ground to air with upward speed); walking off a
 *   ledge or being knocked up does not count, and landing simply re-arms the detector.
 * - Circles stay where they were created. A fixed pool of slots is reused, so repeated jumps never
 *   allocate and never pile up.
 * - Turning the module off, leaving the world or changing world clears every circle.
 */
public final class JumpCircle {
    private static final int SLOTS = 8;
    private static final int SEGMENTS = 48;

    private static final double[] X = new double[SLOTS];
    private static final double[] Y = new double[SLOTS];
    private static final double[] Z = new double[SLOTS];
    private static final long[] BORN = new long[SLOTS];
    private static final boolean[] LIVE = new boolean[SLOTS];
    private static int next;
    private static long ticks;
    private static boolean wasGround = true;
    private static ClientWorld lastWorld;

    private JumpCircle() {}

    private static void clear() {
        for (int i = 0; i < SLOTS; i++) LIVE[i] = false;
        next = 0;
    }

    public static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        ClientWorld w = mc.world;
        if (p == null || w == null) {
            clear();
            lastWorld = null;
            wasGround = true;
            return;
        }
        if (w != lastWorld) {
            clear();
            lastWorld = w;
            wasGround = p.isOnGround();
        }
        if (mc.isPaused()) return;
        ticks++;

        boolean ground = p.isOnGround();
        if (!Modules.JUMP_CIRCLE.enabled()) {
            clear();
            wasGround = ground;
            return;
        }
        boolean takeOff = wasGround && !ground && p.getVelocity().y > 0.2
            && !p.hasVehicle() && !p.isTouchingWater() && !p.getAbilities().flying;
        wasGround = ground;
        if (takeOff) {
            int i = next;
            next = (next + 1) % SLOTS;
            // previous tick position = where the feet were when the jump started
            X[i] = p.prevX;
            Y[i] = p.prevY;
            Z[i] = p.prevZ;
            BORN[i] = ticks;
            LIVE[i] = true;
        }
    }

    public static void render(Matrix4f m, double camX, double camY, double camZ, float tickDelta) {
        if (!Modules.JUMP_CIRCLE.enabled()) return;
        double lifeTicks = Math.max(1.0, Modules.JC_DURATION.value() * 20.0);
        boolean expand = Modules.JC_ANIM.get().equals("Expand");
        int rgb = FxColor.resolve(Modules.JC_COLOR_MODE, Modules.JC_COLOR);
        float opacity = (float) (Modules.JC_OPACITY.value() / 100.0);
        double radiusMax = Modules.JC_SIZE.value();

        boolean any = false;
        for (int i = 0; i < SLOTS; i++) {
            if (!LIVE[i]) continue;
            double age = (ticks - BORN[i]) + tickDelta;
            if (age >= lifeTicks) {
                LIVE[i] = false;
                continue;
            }
            if (!any) {
                Gfx.begin(VertexFormat.DrawMode.TRIANGLE_STRIP);
                any = true;
            }
            float t = (float) (age / lifeTicks);
            float e = UiMotion.ease(t);
            double r = expand ? radiusMax * e : radiusMax * (1.0 - e);
            float fade = expand ? (1f - t) : (1f - t * t);
            double width = Math.max(0.05, radiusMax * 0.12);
            double inner = Math.max(0.0, r - width);
            int outerCol = Gfx.argb(rgb, opacity * fade);
            int innerCol = Gfx.argb(rgb, opacity * fade * 0.15f);
            double cx = X[i] - camX, cy = Y[i] + 0.02 - camY, cz = Z[i] - camZ;
            for (int s = 0; s <= SEGMENTS; s++) {
                double a = (Math.PI * 2.0) * s / SEGMENTS;
                double cs = Math.cos(a), sn = Math.sin(a);
                Gfx.v(m, cx + cs * inner, cy, cz + sn * inner, innerCol);
                Gfx.v(m, cx + cs * r, cy, cz + sn * r, outerCol);
            }
            // break the strip between circles with a degenerate pair
            Gfx.v(m, cx + r, cy, cz, outerCol);
            Gfx.v(m, cx + r, cy, cz, outerCol);
        }
        if (any) Gfx.draw();
    }
}
