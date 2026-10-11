package dev.wezlyk.client.config;

/**
 * Leaf class with no dependencies: settings, modules, themes and the HUD editor call markDirty()
 * when something changes, and Profiles writes the active profile a moment later.
 */
public final class Autosave {
    private static boolean dirty;
    private static long lastChange;

    private Autosave() {}

    public static void markDirty() {
        dirty = true;
        lastChange = System.currentTimeMillis();
    }

    static boolean isDirty() {
        return dirty;
    }

    static boolean settled(long millis) {
        return dirty && System.currentTimeMillis() - lastChange > millis;
    }

    static void clear() {
        dirty = false;
    }
}
