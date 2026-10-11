package dev.wezlyk.client.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.wezlyk.client.config.Config;
import dev.wezlyk.client.setting.ActionSetting;
import dev.wezlyk.client.setting.BoolSetting;
import dev.wezlyk.client.setting.ColorSetting;
import dev.wezlyk.client.setting.LabelSetting;
import dev.wezlyk.client.setting.ModeSetting;
import dev.wezlyk.client.setting.SliderSetting;
import dev.wezlyk.client.ui.ClickGuiHost;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

/** The module registry. Every module here has real logic behind it. */
public final class Modules {
    // Combat: intentionally empty ("SOON..." is shown by the ClickGUI)

    // Movement
    public static final Module SPRINT = new Module("Toggle Sprint", Category.MOVEMENT,
        "Keeps you sprinting while you walk forward.", false);

    // Render: ClickGUI itself. Its on/off state mirrors the open window and is never stored.
    public static final Module CLICKGUI = new Module("ClickGUI", Category.RENDER,
        "Opens the Wezlyk menu. Middle click to change the key.", false).transientState()
        .defaultBind(GLFW.GLFW_KEY_RIGHT_SHIFT);

    // Render: the one HUD entry. Left click = whole HUD on/off, right click = per-element switches.
    public static final Module HUD = new Module("HUD", Category.RENDER,
        "All on-screen overlays. Right click to choose which ones show.", true);
    public static final BoolSetting HUD_WATERMARK = HUD.add(new BoolSetting("Wezlyk Watermark", true));
    public static final BoolSetting WATERMARK_FPS = HUD.add(new BoolSetting("Watermark FPS", true));
    public static final BoolSetting HUD_FPS = HUD.add(new BoolSetting("FPS", false));
    public static final BoolSetting HUD_COORDS = HUD.add(new BoolSetting("Coordinates", false));
    public static final BoolSetting HUD_TARGET = HUD.add(new BoolSetting("Target Info", true));
    public static final SliderSetting TARGET_TIMEOUT =
        HUD.add(new SliderSetting("Target hide after (s)", 4.0, 1.0, 10.0, 0.5, 1));
    public static final BoolSetting HUD_KEYBINDS = HUD.add(new BoolSetting("Keybinds", true));

    public static final Module ASPECT = new Module("Aspect Ratio", Category.RENDER,
        "Changes the aspect ratio of the game image (not the window).", false);
    public static final ModeSetting ASPECT_MODE =
        ASPECT.add(new ModeSetting("Preset", "4:3", "4:3", "1:1", "16:10", "Custom"));
    public static final SliderSetting ASPECT_CUSTOM = ASPECT.add(customRatioSlider());
    public static final LabelSetting ASPECT_INFO =
        ASPECT.add(new LabelSetting("Current", Modules::aspectLabel));

    public static final Module JUMP_CIRCLE = new Module("Jump Circle", Category.RENDER,
        "A ring that spreads from your feet when you jump.", false);
    public static final ModeSetting JC_ANIM = JUMP_CIRCLE.add(new ModeSetting("Animation", "Expand", "Expand", "Shrink"));
    public static final ModeSetting JC_COLOR_MODE = JUMP_CIRCLE.add(new ModeSetting("Color", "Auto", "Auto", "Custom"));
    public static final ColorSetting JC_COLOR = custom(JUMP_CIRCLE, JC_COLOR_MODE);
    public static final SliderSetting JC_OPACITY = JUMP_CIRCLE.add(new SliderSetting("Opacity (%)", 70, 10, 100, 5, 0));
    public static final SliderSetting JC_SIZE = JUMP_CIRCLE.add(new SliderSetting("Size", 1.5, 0.5, 4.0, 0.1, 1));
    public static final SliderSetting JC_DURATION = JUMP_CIRCLE.add(new SliderSetting("Duration (s)", 1.0, 0.3, 3.0, 0.1, 1));

    public static final Module CHINA_HAT = new Module("China Hat", Category.RENDER,
        "A translucent cone above your head (third person).", false);
    public static final SliderSetting HAT_SIZE = CHINA_HAT.add(new SliderSetting("Size", 1.0, 0.5, 2.0, 0.05, 2));
    public static final ModeSetting HAT_COLOR_MODE = CHINA_HAT.add(new ModeSetting("Color", "Auto", "Auto", "Custom"));
    public static final ColorSetting HAT_COLOR = custom(CHINA_HAT, HAT_COLOR_MODE);
    public static final SliderSetting HAT_OPACITY = CHINA_HAT.add(new SliderSetting("Opacity (%)", 35, 5, 100, 5, 0));
    public static final BoolSetting HAT_OUTLINE = CHINA_HAT.add(new BoolSetting("Outline", true));
    public static final SliderSetting HAT_OUTLINE_OPACITY = CHINA_HAT.add(outlineOpacity());

    public static final Module PREDICTIONS = new Module("Predictions", Category.RENDER,
        "Shows where a held Ender Pearl or Splash Potion will land.", false);
    public static final BoolSetting PRED_PEARL = PREDICTIONS.add(new BoolSetting("Ender Pearl", true));
    public static final BoolSetting PRED_POTION = PREDICTIONS.add(new BoolSetting("Splash Potion", true));
    public static final ModeSetting PRED_COLOR_MODE = PREDICTIONS.add(new ModeSetting("Color", "Auto", "Auto", "Custom"));
    public static final ColorSetting PRED_COLOR = custom(PREDICTIONS, PRED_COLOR_MODE);
    public static final SliderSetting PRED_OPACITY = PREDICTIONS.add(new SliderSetting("Opacity (%)", 80, 10, 100, 5, 0));
    public static final SliderSetting PRED_WIDTH = PREDICTIONS.add(new SliderSetting("Line width (px)", 2.0, 1.0, 6.0, 0.5, 1));
    public static final BoolSetting PRED_IMPACT = PREDICTIONS.add(new BoolSetting("Impact point", true));

    public static final Module FAKE_PLAYER = new Module("Fake Player", Category.RENDER,
        "Local training dummy. Exists only on your screen.", false).transientState();
    public static final BoolSetting FAKE_PROT = FAKE_PLAYER.add(new BoolSetting("Model Protection IV", true));
    public static final ActionSetting FAKE_CREATE = FAKE_PLAYER.add(new ActionSetting("Create"));
    public static final ActionSetting FAKE_REMOVE = FAKE_PLAYER.add(new ActionSetting("Remove"));
    public static final ActionSetting FAKE_RESET = FAKE_PLAYER.add(new ActionSetting("Reset"));
    public static final ActionSetting FAKE_PLACE = FAKE_PLAYER.add(new ActionSetting("Place at crosshair"));
    public static final ActionSetting FAKE_HIT = FAKE_PLAYER.add(new ActionSetting("Test hit (4 HP)"));

    private static final List<Module> ALL = List.of(SPRINT, CLICKGUI, HUD, ASPECT, JUMP_CIRCLE, CHINA_HAT,
        PREDICTIONS, FAKE_PLAYER);

    private Modules() {}

    /** "Custom Color" HSL palette, only shown while the Color mode is Custom. */
    private static ColorSetting custom(Module m, ModeSetting mode) {
        ColorSetting c = m.add(new ColorSetting("Custom Color", 0.14f, 1.0f, 0.55f));
        c.visibleWhen(() -> mode.get().equals("Custom"));
        return c;
    }

    private static SliderSetting outlineOpacity() {
        SliderSetting s = new SliderSetting("Outline opacity (%)", 80, 10, 100, 5, 0);
        s.visibleWhen(HAT_OUTLINE::get);
        return s;
    }

    private static SliderSetting customRatioSlider() {
        SliderSetting s = new SliderSetting("Custom ratio", 1.78, 0.5, 2.5, 0.01, 2);
        s.visibleWhen(() -> ASPECT_MODE.get().equals("Custom"));
        return s;
    }

    public static List<Module> all() {
        return ALL;
    }

    public static List<Module> in(Category category) {
        return ALL.stream().filter(m -> m.category == category).toList();
    }

    /** Width / height requested by the preset or the slider, regardless of the module state. */
    public static double aspectRaw() {
        return switch (ASPECT_MODE.get()) {
            case "4:3" -> 4.0 / 3.0;
            case "1:1" -> 1.0;
            case "16:10" -> 1.6;
            default -> ASPECT_CUSTOM.get();
        };
    }

    /** Target aspect ratio to apply to the game image, or -1 when the module is off. */
    public static double aspectTarget() {
        return ASPECT.enabled() ? aspectRaw() : -1.0;
    }

    private static String aspectLabel() {
        String value = String.format(Locale.ROOT, "%.2f", aspectRaw());
        String base = ASPECT_MODE.get() + " (" + value + ")";
        return ASPECT.enabled() ? base : base + ", module off";
    }

    /** Migration helper: module states from the pre-profile config file. */
    public static void load() {
        Config cfg = Config.get();
        for (Module m : ALL) {
            Boolean saved = cfg.modules.get(m.name);
            if (saved != null) {
                m.restore(saved);
            }
        }
        for (var e : cfg.modules.entrySet()) {
            BoolSetting legacy = legacyHudSwitch(e.getKey());
            if (legacy != null && e.getValue() != null) legacy.set(e.getValue());
        }
    }

    /** Old builds had one module per overlay; they are now switches inside the HUD module. */
    private static BoolSetting legacyHudSwitch(String oldModuleName) {
        return switch (oldModuleName) {
            case "Watermark" -> HUD_WATERMARK;
            case "FPS" -> HUD_FPS;
            case "Coordinates" -> HUD_COORDS;
            case "Target Info" -> HUD_TARGET;
            case "Keybinds" -> HUD_KEYBINDS;
            default -> null;
        };
    }

    /**
     * Reads a profile written by an older build (modules "Watermark", "FPS", "Coordinates", "Target Info",
     * "Keybinds") and carries their on/off states and the two renamed settings into the HUD module.
     * Does nothing when the profile already has a HUD entry.
     */
    public static void migrateLegacyProfile(JsonObject mods) {
        if (mods == null || mods.has(HUD.name)) return;
        for (String old : new String[] {"Watermark", "FPS", "Coordinates", "Target Info", "Keybinds"}) {
            JsonElement el = mods.get(old);
            if (el == null || !el.isJsonObject()) continue;
            JsonObject mo = el.getAsJsonObject();
            JsonElement en = mo.get("enabled");
            BoolSetting target = legacyHudSwitch(old);
            if (target != null && en != null && en.isJsonPrimitive() && en.getAsJsonPrimitive().isBoolean()) {
                target.set(en.getAsBoolean());
            }
            JsonElement st = mo.get("settings");
            if (st != null && st.isJsonObject()) {
                JsonObject so = st.getAsJsonObject();
                try {
                    if (old.equals("Watermark") && so.has("Show FPS")) WATERMARK_FPS.load(so.get("Show FPS"));
                    if (old.equals("Target Info") && so.has("Hide after (s)")) TARGET_TIMEOUT.load(so.get("Hide after (s)"));
                } catch (Exception ignored) {
                    // keep defaults
                }
            }
        }
    }

    /**
     * Called from the keyboard event (KeyboardMixin) for every key PRESS, before the game or any screen
     * sees it. One physical press gives exactly one call, so a tap can never be missed or counted twice.
     * Only acts while no screen is open (an open ClickGUI handles its own keys, which is what closes it).
     *
     * @return true when the key belongs to a module bind and was consumed, so nothing else (including a
     *         screen opened by this very press) receives the same event
     */
    public static boolean onKey(MinecraftClient mc, long window, int key) {
        if (key < 0 || mc.getWindow().getHandle() != window) return false;
        if (mc.currentScreen != null || mc.player == null) return false;
        boolean used = false;
        for (Module m : ALL) {
            if (m.bind() != key) continue;
            used = true;
            if (m == CLICKGUI) {
                ClickGuiHost.open(mc, null); // the module mirrors the open window, it is not toggled directly
            } else {
                m.toggle();
            }
        }
        return used;
    }

    /** Returns a human-readable reason when the key can't be assigned to this module, otherwise null. */
    public static String conflict(int key, Module self, MinecraftClient mc) {
        for (Module m : ALL) {
            if (m != self && m.bind() == key) {
                return "Already used by " + m.name;
            }
        }
        String translation = InputUtil.Type.KEYSYM.createFromCode(key).getTranslationKey();
        for (KeyBinding kb : mc.options.allKeys) {
            if (!kb.isUnbound() && kb.getBoundKeyTranslationKey().equals(translation)) {
                return "Used by Minecraft: " + Text.translatable(kb.getTranslationKey()).getString();
            }
        }
        return null;
    }
}
