package dev.wezlyk.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

public final class ModeSetting extends Setting<String> {
    public final List<String> options;

    public ModeSetting(String name, String defaultValue, String... options) {
        super(name, defaultValue);
        this.options = List.of(options);
    }

    public int index() {
        return Math.max(0, options.indexOf(value));
    }

    public void cycle(int dir) {
        int n = options.size();
        set(options.get(((index() + dir) % n + n) % n));
    }

    public void choose(String option) {
        if (options.contains(option)) {
            set(option);
        }
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String s = element.getAsString();
            if (options.contains(s)) {
                value = s;
            }
        }
    }
}
