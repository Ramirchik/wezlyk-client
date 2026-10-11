package dev.wezlyk.client.hud;

import dev.wezlyk.client.module.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

import java.util.UUID;

/**
 * Remembers the last player the local player hit. Only an id and a timestamp are stored; health is
 * always read live from the entity, so what the HUD shows is what the client currently knows.
 */
public final class TargetTracker {
    private static UUID id;
    private static long lastHit;

    private TargetTracker() {}

    public static void hit(PlayerEntity target) {
        id = target.getUuid();
        lastHit = System.currentTimeMillis();
    }

    public static void clear() {
        id = null;
    }

    /**
     * The tracked player, or null when there is no target, the timeout ran out, the player left the
     * world (or is no longer loaded) or died.
     */
    public static PlayerEntity current() {
        if (id == null) return null;
        MinecraftClient mc = MinecraftClient.getInstance();
        long timeoutMs = (long) (Modules.TARGET_TIMEOUT.get() * 1000.0);
        if (mc.world == null || System.currentTimeMillis() - lastHit > timeoutMs) {
            id = null;
            return null;
        }
        PlayerEntity p = mc.world.getPlayerByUuid(id);
        if (p == null || p.isRemoved() || !p.isAlive() || p.getHealth() <= 0f) {
            id = null;
            return null;
        }
        return p;
    }
}
