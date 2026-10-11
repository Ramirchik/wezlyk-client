package dev.wezlyk.client.render;

import dev.wezlyk.client.module.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Optional;

/**
 * Trajectory preview for the Ender Pearl and the Splash Potion while one is held.
 * Uses the vanilla projectile numbers (launch speed, gravity, drag, water drag) and steps tick by
 * tick, testing the path against block collision shapes and nearby entity boxes.
 * It is an estimate: random spread, latency and server-side differences are not modelled.
 */
public final class Predictions {
    private static final int MAX_STEPS = 120;
    private static final double[] PTS = new double[3 * (MAX_STEPS + 2)];
    private static int n;
    private static boolean hit;
    private static double hx, hy, hz;
    private static int nx, ny, nz;

    private Predictions() {}

    public static void render(MinecraftClient mc, Matrix4f m, double camX, double camY, double camZ, float tickDelta) {
        if (!Modules.PREDICTIONS.enabled()) return;
        ClientPlayerEntity p = mc.player;
        ClientWorld w = mc.world;
        if (p == null || w == null) return;

        Item held = null;
        Item main = p.getMainHandStack().getItem();
        Item off = p.getOffHandStack().getItem();
        if (isEnabledProjectile(main)) held = main;
        else if (isEnabledProjectile(off)) held = off;
        if (held == null) return;

        simulate(mc, w, p, held, tickDelta);
        if (n < 2) return;

        int rgb = FxColor.resolve(Modules.PRED_COLOR_MODE, Modules.PRED_COLOR);
        float opacity = (float) (Modules.PRED_OPACITY.value() / 100.0);
        double lineWidth = Modules.PRED_WIDTH.value();
        double fov = mc.options.getFov().getValue();
        int fbh = mc.getWindow().getFramebufferHeight();

        Gfx.begin(VertexFormat.DrawMode.QUADS);
        for (int i = 0; i < n - 1; i++) {
            double x0 = PTS[3 * i] - camX, y0 = PTS[3 * i + 1] - camY, z0 = PTS[3 * i + 2] - camZ;
            double x1 = PTS[3 * i + 3] - camX, y1 = PTS[3 * i + 4] - camY, z1 = PTS[3 * i + 5] - camZ;
            double dist = Math.sqrt(x0 * x0 + y0 * y0 + z0 * z0);
            double half = Gfx.pixelSize(Math.max(0.5, dist), fov, fbh) * lineWidth * 0.5;
            float fade = 1f - 0.5f * i / (float) n;
            Gfx.ribbon(m, x0, y0, z0, x1, y1, z1, half, Gfx.argb(rgb, opacity * fade));
        }
        Gfx.draw();

        if (hit && Modules.PRED_IMPACT.get()) {
            drawImpact(m, hx - camX, hy - camY, hz - camZ, rgb, opacity, fov, fbh, lineWidth);
        }
    }

    private static boolean isEnabledProjectile(Item item) {
        if (item == Items.ENDER_PEARL) return Modules.PRED_PEARL.get();
        if (item == Items.SPLASH_POTION) return Modules.PRED_POTION.get();
        return false;
    }

    private static void simulate(MinecraftClient mc, ClientWorld w, ClientPlayerEntity p, Item item, float td) {
        boolean potion = item == Items.SPLASH_POTION;
        double speed = potion ? 0.5 : 1.5;
        double gravity = potion ? 0.05 : 0.03;
        float roll = potion ? -20f : 0f;

        double yaw = Math.toRadians(p.getYaw(td));
        double pitch = Math.toRadians(p.getPitch(td));
        double dx = -Math.sin(yaw) * Math.cos(pitch);
        double dy = -Math.sin(pitch + Math.toRadians(roll));
        double dz = Math.cos(yaw) * Math.cos(pitch);
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-6) {
            n = 0;
            return;
        }
        Vec3d pv = p.getVelocity();
        double vx = dx / len * speed + pv.x;
        double vy = dy / len * speed + (p.isOnGround() ? 0.0 : pv.y);
        double vz = dz / len * speed + pv.z;

        double x = p.prevX + (p.getX() - p.prevX) * td;
        double y = p.prevY + (p.getY() - p.prevY) * td + p.getEyeHeight(p.getPose()) - 0.1;
        double z = p.prevZ + (p.getZ() - p.prevZ) * td;

        List<Entity> candidates = w.getOtherEntities(p, p.getBoundingBox().expand(64.0),
            e -> e.isAlive() && !e.isSpectator() && e.canHit());

        n = 0;
        hit = false;
        PTS[0] = x;
        PTS[1] = y;
        PTS[2] = z;
        n = 1;
        for (int step = 0; step < MAX_STEPS; step++) {
            Vec3d from = new Vec3d(x, y, z);
            Vec3d to = new Vec3d(x + vx, y + vy, z + vz);

            BlockHitResult bh = w.raycast(new RaycastContext(from, to,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
            Vec3d end = to;
            double best = Double.MAX_VALUE;
            boolean blockHit = bh.getType() != HitResult.Type.MISS;
            if (blockHit) {
                end = bh.getPos();
                best = from.squaredDistanceTo(end);
            }
            boolean entityHit = false;
            for (Entity e : candidates) {
                Optional<Vec3d> r = e.getBoundingBox().expand(0.3).raycast(from, to);
                if (r.isPresent()) {
                    double d = from.squaredDistanceTo(r.get());
                    if (d < best) {
                        best = d;
                        end = r.get();
                        entityHit = true;
                    }
                }
            }
            if (blockHit || entityHit) {
                PTS[3 * n] = end.x;
                PTS[3 * n + 1] = end.y;
                PTS[3 * n + 2] = end.z;
                n++;
                hit = true;
                hx = end.x;
                hy = end.y;
                hz = end.z;
                if (entityHit) {
                    nx = 0;
                    ny = 1;
                    nz = 0;
                } else {
                    Direction side = bh.getSide();
                    nx = side.getOffsetX();
                    ny = side.getOffsetY();
                    nz = side.getOffsetZ();
                }
                return;
            }
            x += vx;
            y += vy;
            z += vz;
            PTS[3 * n] = x;
            PTS[3 * n + 1] = y;
            PTS[3 * n + 2] = z;
            n++;

            boolean water = w.getFluidState(BlockPos.ofFloored(x, y, z)).isIn(FluidTags.WATER);
            double drag = water ? 0.8 : 0.99;
            vx *= drag;
            vy = vy * drag - gravity;
            vz *= drag;
            if (y < w.getBottomY() - 8) return; // fell out of the world, no impact
        }
    }

    private static void drawImpact(Matrix4f m, double cx, double cy, double cz, int rgb, float opacity,
                                   double fov, int fbh, double lineWidth) {
        double ux, uy, uz, vx, vy, vz;
        if (Math.abs(ny) > 0) {
            ux = 1; uy = 0; uz = 0; vx = 0; vy = 0; vz = 1;
        } else if (Math.abs(nx) > 0) {
            ux = 0; uy = 1; uz = 0; vx = 0; vy = 0; vz = 1;
        } else {
            ux = 1; uy = 0; uz = 0; vx = 0; vy = 1; vz = 0;
        }
        cx += nx * 0.01;
        cy += ny * 0.01;
        cz += nz * 0.01;
        double r = 0.2;
        int segs = 20;

        Gfx.begin(VertexFormat.DrawMode.TRIANGLE_FAN);
        Gfx.v(m, cx, cy, cz, Gfx.argb(rgb, opacity * 0.45f));
        for (int i = 0; i <= segs; i++) {
            double a = Math.PI * 2.0 * i / segs;
            double c = Math.cos(a) * r, s = Math.sin(a) * r;
            Gfx.v(m, cx + ux * c + vx * s, cy + uy * c + vy * s, cz + uz * c + vz * s,
                Gfx.argb(rgb, opacity * 0.45f));
        }
        Gfx.draw();

        double dist = Math.sqrt(cx * cx + cy * cy + cz * cz);
        double half = Gfx.pixelSize(Math.max(0.5, dist), fov, fbh) * lineWidth * 0.5;
        Gfx.begin(VertexFormat.DrawMode.QUADS);
        double px = cx + ux * r, py = cy + uy * r, pz = cz + uz * r;
        for (int i = 1; i <= segs; i++) {
            double a = Math.PI * 2.0 * i / segs;
            double c = Math.cos(a) * r, s = Math.sin(a) * r;
            double qx = cx + ux * c + vx * s, qy = cy + uy * c + vy * s, qz = cz + uz * c + vz * s;
            Gfx.ribbon(m, px, py, pz, qx, qy, qz, half, Gfx.argb(rgb, opacity));
            px = qx;
            py = qy;
            pz = qz;
        }
        Gfx.draw();
    }
}
