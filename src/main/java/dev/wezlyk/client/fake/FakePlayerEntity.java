package dev.wezlyk.client.fake;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;

/**
 * Local training dummy. It is a client-side player entity that only exists in the client world:
 * the server never hears about it, and attacks/interactions aimed at it are cancelled before any
 * packet is built (see FakePlayerManager). Health is a plain local number, not synced data.
 */
public final class FakePlayerEntity extends OtherClientPlayerEntity {
    public static final float MAX_HEALTH = 20f;
    private float simHealth = MAX_HEALTH;
    /** Ticks during which new hits are ignored (vanilla invulnerability window). */
    int invulnerableTicks;

    public FakePlayerEntity(ClientWorld world, GameProfile profile) {
        super(world, profile);
        setNoGravity(true);
    }

    @Override
    public float getHealth() {
        return simHealth;
    }

    void setSimHealth(float h) {
        simHealth = Math.max(0f, Math.min(MAX_HEALTH, h));
    }
}
