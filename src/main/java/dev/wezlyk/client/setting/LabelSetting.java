package dev.wezlyk.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import java.util.function.Supplier;

/** Read-only line in a settings window showing a live value (for example the current aspect ratio). */
public final class LabelSetting extends Setting<String> {
    private final Supplier<String> text;

    public LabelSetting(String name, Supplier<String> text) {
        super(name, "");
        this.text = text;
    }

    public String text() {
        return text.get();
    }

    @Override
    public boolean persistent() {
        return false;
    }

    @Override
    public JsonElement save() {
        return JsonNull.INSTANCE;
    }

    @Override
    public void load(JsonElement element) {
        // nothing to load
    }
}
