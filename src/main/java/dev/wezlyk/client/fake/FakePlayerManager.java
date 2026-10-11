package dev.wezlyk.client.fake;

import com.mojang.authlib.GameProfile;
import dev.wezlyk.client.hud.TargetTracker;
import dev.wezlyk.client.module.Modules;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.UUID;

/**
 * Creates, removes, resets and simulates the local Fake Player. Everything here is client-only:
 * no packets are sent, the server world is never touched, and any attack or use aimed at the dummy
 * is cancelled locally and turned into simulated damage.
 */
public final class FakePlayerManager {
    private static final UUID ID = UUID.nameUUIDFromBytes("wezlyk-fake-player".getBytes());
    private static final int ENTITY_ID = -1_572_864;

    private static FakePlayerEntity fake;
    private static ClientWorld fakeWorld;

    private FakePlayerManager() {}

    public static void register() {
        Modules.FAKE_CREATE.setAction(FakePlayerManager::create);
        Modules.FAKE_REMOVE.setAction(FakePlayerManager::removeAction);
        Modules.FAKE_RESET.setAction(FakePlayerManager::reset);
        Modules.FAKE_PLACE.setAction(FakePlayerManager::placeAtCrosshair);
        Modules.FAKE_HIT.setAction(FakePlayerManager::testHit);
        ClientTickEvents.END_CLIENT_TICK.register(FakePlayerManager::tick);
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (isFake(entity)) {
                if (world.isClient) hit((FakePlayerEntity) entity, player);
                return ActionResult.FAIL; // cancels the attack packet
            }
            return ActionResult.PASS;
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
            isFake(entity) ? ActionResult.FAIL : ActionResult.PASS); // no interaction packet either
    }

    public static boolean isFake(Entity e) {
        return e != null && e == fake;
    }

    public static boolean isFakeId(UUID id) {
        return fake != null && ID.equals(id);
    }

    public static FakePlayerEntity get() {
        return fake;
    }

    public static UUID id() {
        return ID;
    }

    public static boolean exists() {
        return fake != null;
    }

    // ---- lifecycle -------------------------------------------------------------------------

    private static void tick(MinecraftClient mc) {
        ClientWorld w = mc.world;
        if (w == null || mc.player == null) {
            dropReference();
            return;
        }
        if (fake != null && (fakeWorld != w || fake.isRemoved())) {
            dropReference(); // world changed or the entity vanished: forget it without touching the new world
        }
        boolean want = Modules.FAKE_PLAYER.enabled();
        if (want && fake == null) {
            spawnNear(mc);
        } else if (!want && fake != null) {
            remove(mc);
        }
        if (fake != null) {
            if (fake.invulnerableTicks > 0) fake.invulnerableTicks--;
            fake.setCustomName(Text.literal("Fake Player  " + health(fake) + " HP"));
            fake.setCustomNameVisible(true);
        }
    }

    private static String health(FakePlayerEntity f) {
        float h = f.getHealth();
        return Math.abs(h - Math.round(h)) < 0.05f ? Integer.toString(Math.round(h)) : String.format(java.util.Locale.ROOT, "%.1f", h);
    }

    private static void dropReference() {
        fake = null;
        fakeWorld = null;
    }

    private static FakePlayerEntity build(ClientWorld w) {
        FakePlayerEntity f = new FakePlayerEntity(w, new GameProfile(ID, "FakePlayer"));
        f.setId(ENTITY_ID); // negative id: can never collide with a server entity id
        f.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        f.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        f.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        f.equipStack(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
        f.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        return f;
    }

    private static void place(MinecraftClient mc, FakePlayerEntity f, double x, double y, double z) {
        f.refreshPositionAndAngles(x, y, z, mc.player.getYaw() + 180f, 0f);
        f.setHeadYaw(f.getYaw());
        f.setBodyYaw(f.getYaw());
    }

    /** Creates the dummy 3 blocks in front of the player, standing on the ground below that spot. */
    private static void spawnNear(MinecraftClient mc) {
        ClientWorld w = mc.world;
        double yaw = Math.toRadians(mc.player.getYaw());
        double x = mc.player.getX() - Math.sin(yaw) * 3.0;
        double z = mc.player.getZ() + Math.cos(yaw) * 3.0;
        double y = groundY(mc, x, mc.player.getY(), z);
        FakePlayerEntity f = build(w);
        place(mc, f, x, y, z);
        attach(mc, f);
    }

    private static double groundY(MinecraftClient mc, double x, double y, double z) {
        BlockHitResult r = mc.world.raycast(new RaycastContext(new Vec3d(x, y + 2.0, z), new Vec3d(x, y - 8.0, z),
            RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return r.getType() == HitResult.Type.MISS ? y : r.getPos().y;
    }

    private static void attach(MinecraftClient mc, FakePlayerEntity f) {
        mc.world.addEntity(f);
        fake = f;
        fakeWorld = mc.world;
    }

    private static void remove(MinecraftClient mc) {
        FakePlayerEntity f = fake;
        dropReference();
        if (f != null && mc.world != null) {
            mc.world.removeEntity(f.getId(), Entity.RemovalReason.DISCARDED);
        }
    }

    // ---- actions bound to the settings buttons --------------------------------------------

    public static String create() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return "Join a world first";
        if (fake != null) return "Already created";
        Modules.FAKE_PLAYER.setEnabled(true);
        spawnNear(mc);
        return "Fake Player created";
    }

    public static String removeAction() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (fake == null) return "No Fake Player";
        remove(mc);
        Modules.FAKE_PLAYER.setEnabled(false);
        return "Fake Player removed";
    }

    /** Full health again; if it does not exist yet this is the same as create. */
    public static String reset() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return "Join a world first";
        if (fake == null) return create();
        fake.setSimHealth(FakePlayerEntity.MAX_HEALTH);
        fake.invulnerableTicks = 0;
        fake.hurtTime = 0;
        return "Fake Player reset to 20 HP";
    }

    /** Moves the dummy to the block the player is looking at, if there is room for it. */
    public static String placeAtCrosshair() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return "Join a world first";
        Vec3d eye = mc.player.getEyePos();
        Vec3d end = eye.add(mc.player.getRotationVec(1f).multiply(24.0));
        BlockHitResult r = mc.world.raycast(new RaycastContext(eye, end,
            RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        if (r.getType() == HitResult.Type.MISS || r.getSide() != Direction.UP) return "Look at the top of a block";
        Vec3d p = r.getPos();
        if (fake == null) {
            FakePlayerEntity f = build(mc.world);
            place(mc, f, p.x, p.y, p.z);
            if (!mc.world.isSpaceEmpty(f.getBoundingBox())) return "Not enough room there";
            attach(mc, f);
            Modules.FAKE_PLAYER.setEnabled(true);
            return "Fake Player placed";
        }
        Vec3d old = fake.getPos();
        place(mc, fake, p.x, p.y, p.z);
        if (!mc.world.isSpaceEmpty(fake.getBoundingBox())) {
            place(mc, fake, old.x, old.y, old.z);
            return "Not enough room there";
        }
        return "Fake Player moved";
    }

    /** Simulated test hit of 4 HP (two hearts) that skips the weapon model. */
    public static String testHit() {
        if (fake == null) return "No Fake Player";
        damage(fake, 4f);
        return "Test hit: " + health(fake) + " HP left";
    }

    // ---- local damage model ----------------------------------------------------------------

    private static void hit(FakePlayerEntity f, PlayerEntity attacker) {
        TargetTracker.hit(f);
        float base = (float) attacker.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
        float cooldown = attacker.getAttackCooldownProgress(0.5f);
        float dmg = base * (0.2f + cooldown * cooldown * 0.8f);
        boolean crit = cooldown > 0.9f && attacker.fallDistance > 0f && !attacker.isOnGround()
            && !attacker.isClimbing() && !attacker.isTouchingWater() && !attacker.hasVehicle() && !attacker.isSprinting();
        if (crit) dmg *= 1.5f;
        damage(f, dmg);
        attacker.swingHand(net.minecraft.util.Hand.MAIN_HAND, false);
    }

    /**
     * Local approximation of vanilla reduction for a full netherite set (armor 20, toughness 12),
     * plus optional Protection IV on every piece (16 protection points, 4 % each, capped at 80 %).
     * Not an exact copy of the server formula (no resistance, absorption, shield or knockback).
     */
    static float reduce(float raw) {
        float armor = 20f, toughness = 12f;
        float eff = Math.min(20f, Math.max(armor / 5f, armor - raw / (2f + toughness / 4f)));
        float out = raw * (1f - eff / 25f);
        if (Modules.FAKE_PROT.get()) {
            out *= 1f - Math.min(20f, 16f) / 25f;
        }
        return out;
    }

    public static void damage(FakePlayerEntity f, float raw) {
        if (f.invulnerableTicks > 0 || raw <= 0f) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        f.invulnerableTicks = 10;
        f.hurtTime = f.maxHurtTime = 10;
        float left = f.getHealth() - reduce(raw);
        sound(mc, f, SoundEvents.ENTITY_PLAYER_HURT);
        if (left > 0f) {
            f.setSimHealth(left);
            return;
        }
        // simulated death: totem pop, then back to full health
        f.setSimHealth(FakePlayerEntity.MAX_HEALTH);
        sound(mc, f, SoundEvents.ITEM_TOTEM_USE);
        mc.particleManager.addEmitter(f, ParticleTypes.TOTEM_OF_UNDYING, 30);
    }

    private static void sound(MinecraftClient mc, Entity e, SoundEvent s) {
        if (mc.world != null) {
            mc.world.playSound(e.getX(), e.getY(), e.getZ(), s, SoundCategory.PLAYERS, 1.0f, 1.0f, false);
        }
    }
}
