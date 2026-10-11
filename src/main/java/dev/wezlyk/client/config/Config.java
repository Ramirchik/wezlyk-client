package dev.wezlyk.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Plain JSON settings. Stores module states and offline profile names only: no passwords, no tokens. */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config instance;

    /** Legacy (pre-profile) module states. Only read once to migrate into the first profile. */
    public Map<String, Boolean> modules = new LinkedHashMap<>();
    public List<String> accounts = new ArrayList<>();
    /** Selected offline profile, or null when the launcher account is used. */
    public String selected;
    /** Name of the active configuration profile (see Profiles). */
    public String profile = "default";

    public static synchronized Config get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("wezlyk-client.json");
    }

    private static Config load() {
        Path p = file();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p)) {
                Config c = GSON.fromJson(r, Config.class);
                if (c != null) {
                    if (c.modules == null) c.modules = new LinkedHashMap<>();
                    if (c.accounts == null) c.accounts = new ArrayList<>();
                    return c;
                }
            } catch (Exception ignored) {
                // damaged file: keep a copy next to it and fall back to defaults
                try {
                    Files.move(p, p.resolveSibling("wezlyk-client.json.bad"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignoredToo) {
                    // best effort
                }
            }
        }
        return new Config();
    }

    public static synchronized void save() {
        try {
            Path target = file();
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling("wezlyk-client.json.tmp");
            try (Writer w = Files.newBufferedWriter(tmp)) {
                GSON.toJson(get(), w);
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // settings are best-effort
        }
    }
}
