package dev.wezlyk.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import java.util.function.Supplier;

/**
 * A button row in a settings window. The action returns a short status message for the footer.
 * The action can be attached later (setAction), which keeps the module registry free of feature code.
 */
public final class ActionSetting extends Setting<String> {
    private Supplier<String> action;

    public ActionSetting(String label) {
        super(label, "");
    }

    public void setAction(Supplier<String> action) {
        this.action = action;
    }

    /** Runs the action and returns its message (never null). */
    public String run() {
        if (action == null) return "Not available";
        try {
            String r = action.get();
            return r == null ? "" : r;
        } catch (RuntimeException e) {
            return "Failed: " + e.getClass().getSimpleName();
        }
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
