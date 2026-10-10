package dev.wezlyk.client.module;

import dev.wezlyk.client.config.Config;
import dev.wezlyk.client.ui.Anim;

public final class Module {
    public final String name;
    public final Category category;
    /** Switch animation (0 = off, 1 = on) and row hover animation, driven by the ClickGUI. */
    public final Anim anim = new Anim(0);
    public final Anim hover = new Anim(0);
    private boolean enabled;

    Module(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    public boolean enabled() {
        return enabled;
    }

    void restore(boolean value) {
        enabled = value;
        anim.v = value ? 1f : 0f;
    }

    public void toggle() {
        enabled = !enabled;
        Config.get().modules.put(name, enabled);
        Config.save();
    }
}
