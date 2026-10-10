package dev.wezlyk.client.account;

import dev.wezlyk.client.config.Config;
import dev.wezlyk.client.mixin.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Account switching without touching authentication.
 * <p>
 * The launcher's own session (Microsoft sign-in is done by the launcher, e.g. FCL) is kept untouched
 * and can be restored at any time. Saved entries are offline profiles: a name only, with the
 * standard offline UUID and a dummy token. They work in singleplayer and on offline-mode servers
 * and cannot authenticate against online-mode servers. Nothing secret is stored.
 */
public final class Accounts {
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private static Session launcher;

    private Accounts() {}

    public static void init(MinecraftClient mc) {
        launcher = mc.getSession();
        String selected = Config.get().selected;
        if (selected != null) {
            apply(mc, selected);
        }
    }

    public static String launcherName() {
        return launcher != null ? launcher.getUsername() : MinecraftClient.getInstance().getSession().getUsername();
    }

    public static String activeName() {
        return MinecraftClient.getInstance().getSession().getUsername();
    }

    public static boolean launcherActive() {
        return Config.get().selected == null;
    }

    public static boolean isSelected(String name) {
        return name.equals(Config.get().selected);
    }

    public static boolean isValid(String name) {
        return name != null && VALID.matcher(name).matches();
    }

    /** Adds an offline profile name. Returns false when the name is invalid. */
    public static boolean add(String name) {
        if (!isValid(name)) return false;
        Config cfg = Config.get();
        if (!cfg.accounts.contains(name)) {
            cfg.accounts.add(name);
            Config.save();
        }
        return true;
    }

    public static void select(String name) {
        if (!Config.get().accounts.contains(name)) return;
        apply(MinecraftClient.getInstance(), name);
        Config.get().selected = name;
        Config.save();
    }

    public static void useLauncher() {
        if (launcher != null) {
            ((MinecraftClientAccessor) MinecraftClient.getInstance()).wezlyk$setSession(launcher);
        }
        Config.get().selected = null;
        Config.save();
    }

    public static void remove(String name) {
        Config cfg = Config.get();
        if (name.equals(cfg.selected)) {
            useLauncher();
        }
        cfg.accounts.remove(name);
        Config.save();
    }

    private static void apply(MinecraftClient mc, String name) {
        UUID id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        Session offline = new Session(name, id, "0", Optional.empty(), Optional.empty(), Session.AccountType.MOJANG);
        ((MinecraftClientAccessor) mc).wezlyk$setSession(offline);
    }
}
