package dev.wezlyk.client.theme;

import dev.wezlyk.client.config.Autosave;
import dev.wezlyk.client.ui.Glass;

import java.util.List;

public final class Themes {
    public static final List<Theme> ALL = List.of(Theme.DARK_CLASSIC, Theme.SAKURA, Theme.LIGHT);
    private static Theme current = Theme.DARK_CLASSIC;

    private Themes() {}

    public static Theme current() {
        return current;
    }

    /** User-initiated change: applies and autosaves into the active profile. */
    public static void set(Theme theme) {
        if (theme != current) {
            setSilently(theme);
            Autosave.markDirty();
        }
    }

    /** Used when loading profiles. */
    public static void setSilently(Theme theme) {
        current = theme != null ? theme : Theme.DARK_CLASSIC;
        Glass.apply(current);
    }

    public static Theme byName(String name) {
        for (Theme t : ALL) {
            if (t.name().equals(name)) return t;
        }
        return null;
    }
}
