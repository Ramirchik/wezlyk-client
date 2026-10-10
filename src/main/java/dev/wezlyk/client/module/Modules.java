package dev.wezlyk.client.module;

import dev.wezlyk.client.config.Config;

import java.util.List;

/** Deliberately small set of harmless client-side utilities. */
public final class Modules {
    public static final Module FPS = new Module("FPS", Category.VISUAL);
    public static final Module COORDS = new Module("Coordinates", Category.VISUAL);
    public static final Module SPRINT = new Module("Toggle Sprint", Category.MOVEMENT);

    private static final List<Module> ALL = List.of(FPS, COORDS, SPRINT);

    private Modules() {}

    public static List<Module> all() {
        return ALL;
    }

    public static List<Module> in(Category category) {
        return ALL.stream().filter(m -> m.category == category).toList();
    }

    public static void load() {
        Config cfg = Config.get();
        for (Module m : ALL) {
            Boolean saved = cfg.modules.get(m.name);
            if (saved != null) {
                m.restore(saved);
            }
        }
    }
}
