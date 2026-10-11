package dev.wezlyk.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wezlyk.client.hud.HudElement;
import dev.wezlyk.client.hud.HudManager;
import dev.wezlyk.client.module.Module;
import dev.wezlyk.client.module.Modules;
import dev.wezlyk.client.theme.Theme;
import dev.wezlyk.client.theme.Themes;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Configuration profiles, stored as JSON in the Minecraft config directory:
 * config/wezlyk-client/profiles/&lt;name&gt;.json.
 * <p>
 * A profile holds the theme, every module's state, key bind and settings (Aspect Ratio included)
 * and every HUD element's position. HUD visibility is the state of the module that owns the element.
 * The active profile is autosaved shortly after any change. Accounts are not part of profiles.
 */
public final class Profiles {
    public static final String DEFAULT = "default";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Profiles() {}

    // ---- paths and names -------------------------------------------------------------------

    private static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("wezlyk-client").resolve("profiles");
    }

    private static Path file(String name) {
        return dir().resolve(name + ".json");
    }

    public static boolean validName(String name) {
        return name != null && name.matches("[A-Za-z0-9_-]{1,24}");
    }

    public static String active() {
        String n = Config.get().profile;
        return validName(n) ? n : DEFAULT;
    }

    public static List<String> list() {
        List<String> names = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir())) {
            s.map(p -> p.getFileName().toString())
                .filter(n -> n.endsWith(".json"))
                .map(n -> n.substring(0, n.length() - 5))
                .filter(Profiles::validName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(names::add);
        } catch (IOException ignored) {
            // directory does not exist yet
        }
        names.remove(DEFAULT);
        names.add(0, DEFAULT);
        return names;
    }

    // ---- snapshot / apply ------------------------------------------------------------------

    private static JsonObject snapshot() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.addProperty("theme", Themes.current().name());
        JsonObject mods = new JsonObject();
        for (Module m : Modules.all()) {
            mods.add(m.name, m.save());
        }
        root.add("modules", mods);
        JsonObject hud = new JsonObject();
        for (HudElement e : HudManager.ELEMENTS) {
            hud.add(e.id, e.save());
        }
        root.add("hud", hud);
        return root;
    }

    private static JsonObject obj(JsonObject parent, String key) {
        if (parent == null) return null;
        JsonElement e = parent.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    private static void resetAll() {
        Themes.setSilently(Theme.DARK_CLASSIC);
        for (Module m : Modules.all()) m.resetDefaults();
        for (HudElement e : HudManager.ELEMENTS) e.resetPosition();
    }

    /** Resets to defaults first, then applies every valid value found, so old or partial files are safe. */
    private static void apply(JsonObject root) {
        resetAll();
        try {
            JsonElement t = root.get("theme");
            if (t != null && t.isJsonPrimitive()) {
                Themes.setSilently(Themes.byName(t.getAsString()));
            }
        } catch (Exception ignored) {
            // keep default theme
        }
        JsonObject mods = obj(root, "modules");
        for (Module m : Modules.all()) {
            JsonObject mo = obj(mods, m.name);
            if (mo != null) {
                try {
                    m.load(mo);
                } catch (Exception ignored) {
                    // keep defaults for this module
                }
            }
        }
        JsonObject hud = obj(root, "hud");
        for (HudElement e : HudManager.ELEMENTS) {
            JsonObject ho = obj(hud, e.id);
            if (ho != null) {
                try {
                    e.load(ho);
                } catch (Exception ignored) {
                    // keep default position
                }
            }
        }
    }

    // ---- file IO ---------------------------------------------------------------------------

    /** Returns null when the file is missing; a damaged file is moved aside to .bad and also gives null. */
    private static JsonObject read(String name) {
        Path p = file(name);
        if (!Files.isRegularFile(p)) return null;
        try (Reader r = Files.newBufferedReader(p)) {
            JsonElement e = JsonParser.parseReader(r);
            if (e != null && e.isJsonObject()) {
                return e.getAsJsonObject();
            }
        } catch (Exception ignored) {
            // fall through to quarantine
        }
        try {
            Files.move(p, dir().resolve(name + ".json.bad"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // best effort
        }
        return null;
    }

    private static boolean write(String name, JsonObject obj) {
        try {
            Files.createDirectories(dir());
            Path tmp = dir().resolve(name + ".json.tmp");
            try (Writer w = Files.newBufferedWriter(tmp)) {
                GSON.toJson(obj, w);
            }
            Files.move(tmp, file(name), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // ---- lifecycle -------------------------------------------------------------------------

    /** Loads the active profile; creates it from defaults (or migrated legacy settings) when absent. */
    public static void init() {
        String name = active();
        JsonObject o = read(name);
        if (o != null) {
            apply(o);
        } else {
            resetAll();
            Modules.load(); // legacy states, harmless when empty
            write(name, snapshot());
        }
        if (!name.equals(Config.get().profile)) {
            Config.get().profile = name;
            Config.save();
        }
    }

    /** Called every client tick: writes the active profile shortly after the last change. */
    public static void tick() {
        if (Autosave.settled(800)) {
            flush();
        }
    }

    public static void flush() {
        if (Autosave.isDirty()) {
            write(active(), snapshot());
            Autosave.clear();
        }
    }

    // ---- user actions ----------------------------------------------------------------------

    /** Creates (or overwrites) a profile from the current state and makes it the active one. */
    public static boolean saveAs(String name) {
        if (!validName(name)) return false;
        if (!write(name, snapshot())) return false;
        Autosave.clear();
        Config.get().profile = name;
        Config.save();
        return true;
    }

    /** Loads a profile. Returns false when the file is missing or damaged; the current state is kept. */
    public static boolean load(String name) {
        if (!validName(name)) return false;
        flush();
        JsonObject o = read(name);
        if (o == null) return false;
        apply(o);
        Autosave.clear();
        Config.get().profile = name;
        Config.save();
        return true;
    }

    /** Deletes a profile (never "default"). Deleting the active one falls back to "default". */
    public static boolean delete(String name) {
        if (!validName(name) || name.equals(DEFAULT)) return false;
        boolean wasActive = name.equals(active());
        try {
            Files.deleteIfExists(file(name));
        } catch (IOException e) {
            return false;
        }
        if (wasActive) {
            Autosave.clear();
            if (!load(DEFAULT)) {
                resetAll();
                saveAs(DEFAULT);
            }
        }
        return true;
    }
}
