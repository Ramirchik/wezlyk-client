package dev.wezlyk.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

public final class SliderSetting extends Setting<Double> {
    public final double min, max, step;
    public final int decimals;

    public SliderSetting(String name, double defaultValue, double min, double max, double step, int decimals) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
        this.decimals = decimals;
    }

    private double fix(double v) {
        double c = Math.max(min, Math.min(max, v));
        if (step > 0) {
            c = min + Math.round((c - min) / step) * step;
        }
        return Math.max(min, Math.min(max, c));
    }

    public void setSnapped(double v) {
        set(fix(v));
    }

    public double value() {
        return value;
    }

    public String display() {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            double v = element.getAsDouble();
            if (!Double.isNaN(v) && !Double.isInfinite(v)) {
                value = fix(v);
            }
        }
    }
}
