package dev.wezlyk.client.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.wezlyk.client.config.Autosave;
import dev.wezlyk.client.setting.Setting;
import dev.wezlyk.client.ui.Anim;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class Module {
    public final String name;
    public final String description;
    public final Category category;
    /** Switch animation (0 = off, 1 = on) and row hover animation, driven by the ClickGUI. */
    public final Anim anim = new Anim(0);
    public final Anim hover = new Anim(0);

    private final boolean defaultEnabled;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;
    private int bind = GLFW.GLFW_KEY_UNKNOWN;
    private int defaultBind = GLFW.GLFW_KEY_UNKNOWN;
    private boolean prevDown;
    private boolean transientState;

    Module(String name, Category category, String description, boolean defaultEnabled) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.defaultEnabled = defaultEnabled;
        restore(defaultEnabled);
    }

    public <S extends Setting<?>> S add(S setting) {
        settings.add(setting);
        return setting;
    }

    public List<Setting<?>> settings() {
        return settings;
    }

    public List<Setting<?>> visibleSettings() {
        List<Setting<?>> out = new ArrayList<>();
        for (Setting<?> s : settings) {
            if (s.visible()) out.add(s);
        }
        return out;
    }

    public boolean hasSettings() {
        return !settings.isEmpty();
    }

    public boolean enabled() {
        return enabled;
    }

    void restore(boolean value) {
        enabled = value;
        anim.v = value ? 1f : 0f;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        if (!transientState) Autosave.markDirty();
    }

    /**
     * A module whose on/off state is not stored in profiles (it only mirrors something that is open
     * right now, such as the ClickGUI window). Its bind and settings are still saved.
     */
    public Module transientState() {
        transientState = true;
        restore(false);
        return this;
    }

    /** Key used when nothing is stored (and after a reset). */
    public Module defaultBind(int key) {
        defaultBind = key;
        bind = key;
        return this;
    }

    public int bind() {
        return bind;
    }

    public boolean hasBind() {
        return bind >= 0;
    }

    public void setBind(int key) {
        bind = key;
        prevDown = false;
        Autosave.markDirty();
    }

    public String bindName() {
        if (bind < 0) return "";
        return InputUtil.Type.KEYSYM.createFromCode(bind).getLocalizedText().getString();
    }

    /** Returns true once per physical key press. Always keeps the edge state current. */
    boolean pollBind(long window) {
        boolean down = bind >= 0 && InputUtil.isKeyPressed(window, bind);
        boolean edge = down && !prevDown;
        prevDown = down;
        return edge;
    }

    /** Back to factory state: disabled (or the module's default), no bind, default settings. */
    public void resetDefaults() {
        if (!transientState) restore(defaultEnabled);
        bind = defaultBind;
        prevDown = false;
        for (Setting<?> s : settings) {
            s.reset();
        }
    }

    public JsonObject save() {
        JsonObject o = new JsonObject();
        if (!transientState) o.addProperty("enabled", enabled);
        o.addProperty("bind", bind);
        JsonObject s = new JsonObject();
        for (Setting<?> setting : settings) {
            if (setting.persistent()) {
                s.add(setting.name, setting.save());
            }
        }
        o.add("settings", s);
        return o;
    }

    /** Reads whatever is valid; anything missing or malformed keeps the current (default) value. */
    public void load(JsonObject o) {
        try {
            JsonElement e = o.get("enabled");
            if (!transientState && e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean()) {
                restore(e.getAsBoolean());
            }
        } catch (Exception ignored) {
            // keep default
        }
        try {
            JsonElement e = o.get("bind");
            if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
                int k = e.getAsInt();
                if (k >= GLFW.GLFW_KEY_UNKNOWN && k <= GLFW.GLFW_KEY_LAST) {
                    bind = k;
                }
            }
        } catch (Exception ignored) {
            // keep default
        }
        JsonElement se = o.get("settings");
        if (se != null && se.isJsonObject()) {
            JsonObject so = se.getAsJsonObject();
            for (Setting<?> setting : settings) {
                JsonElement v = so.get(setting.name);
                if (v != null && setting.persistent()) {
                    try {
                        setting.load(v);
                    } catch (Exception ignored) {
                        // keep default
                    }
                }
            }
        }
    }
}
