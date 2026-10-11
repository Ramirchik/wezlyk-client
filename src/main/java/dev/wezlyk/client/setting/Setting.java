package dev.wezlyk.client.setting;

import com.google.gson.JsonElement;
import dev.wezlyk.client.config.Autosave;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** One configurable value of a module. Changes through set() are autosaved into the active profile. */
public abstract class Setting<T> {
    public final String name;
    protected T value;
    private final T defaultValue;
    private BooleanSupplier visible = () -> true;

    protected Setting(String name, T defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public T get() {
        return value;
    }

    public void set(T v) {
        if (!Objects.equals(value, v)) {
            value = v;
            Autosave.markDirty();
        }
    }

    public void reset() {
        value = defaultValue;
    }

    public Setting<T> visibleWhen(BooleanSupplier supplier) {
        this.visible = supplier;
        return this;
    }

    public boolean visible() {
        return visible.getAsBoolean();
    }

    /** Read-only display rows are not written to profiles. */
    public boolean persistent() {
        return true;
    }

    public abstract JsonElement save();

    /** Must validate input; throwing is fine, the caller keeps the default. */
    public abstract void load(JsonElement element);
}
