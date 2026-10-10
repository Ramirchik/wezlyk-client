package dev.wezlyk.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Plain JSON settings. Stores module states and offline profile names only: no passwords, no tokens. */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config instance;

    public Map<String, Boolean> modules = new LinkedHashMap<>();
    public List<String> accounts = new ArrayList<>();
    /** Selected offline profile, or null when the launcher account is used. */
    public String selected;

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
                // fall back to defaults
            }
        }
        return new Config();
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(file().getParent());
            try (Writer w = Files.newBufferedWriter(file())) {
                GSON.toJson(get(), w);
            }
        } catch (IOException ignored) {
            // settings are best-effort
        }
    }
}
