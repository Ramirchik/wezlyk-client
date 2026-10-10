package dev.wezlyk.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.Mutable;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {

    @Mutable
    @Accessor("session")
    void wezlyk$setSession(Session session);
}
