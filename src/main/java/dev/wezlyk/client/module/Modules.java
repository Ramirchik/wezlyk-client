package dev.wezlyk.client.module;

import dev.wezlyk.client.config.Config;
import dev.wezlyk.client.setting.BoolSetting;
import dev.wezlyk.client.setting.LabelSetting;
import dev.wezlyk.client.setting.ModeSetting;
import dev.wezlyk.client.setting.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;

/** The module registry. Every module here has real logic behind it. */
public final class Modules {
    // Combat
    public static final Module TARGET_INFO = new Module("Target Info", Category.COMBAT,
        "Shows name and health of the last player you hit.", true);
    public static final SliderSetting TARGET_TIMEOUT =
        TARGET_INFO.add(new SliderSetting("Hide after (s)", 4.0, 1.0, 10.0, 0.5, 1));

    // Movement
    public static final Module SPRINT = new Module("Toggle Sprint", Category.MOVEMENT,
        "Keeps you sprinting while you walk forward.", false);

    // Render
    public static final Module FPS = new Module("FPS", Category.RENDER,
        "Frame rate readout on the HUD.", false);
    public static final Module COORDS = new Module("Coordinates", Category.RENDER,
        "Block coordinates readout on the HUD.", false);
    public static final Module WATERMARK = new Module("Watermark", Category.RENDER,
        "Client name pill at the top of the screen.", true);
    public static final BoolSetting WATERMARK_FPS =
        WATERMARK.add(new BoolSetting("Show FPS", true));
    public static final Module KEYBINDS = new Module("Keybinds", Category.RENDER,
        "Lists enabled modules that have a key assigned.", true);
    public static final Module ASPECT = new Module("Aspect Ratio", Category.RENDER,
        "Changes the aspect ratio of the game image (not the window).", false);
    public static final ModeSetting ASPECT_MODE =
        ASPECT.add(new ModeSetting("Preset", "4:3", "4:3", "1:1", "16:10", "Custom"));
    public static final SliderSetting ASPECT_CUSTOM = ASPECT.add(customRatioSlider());
    public static final LabelSetting ASPECT_INFO =
        ASPECT.add(new LabelSetting("Current", Modules::aspectLabel));

    private static final List<Module> ALL =
        List.of(TARGET_INFO, SPRINT, FPS, COORDS, WATERMARK, KEYBINDS, ASPECT);

    private Modules() {}

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
    }

    /** Fires key binds. Edge state is tracked every tick so holding a key never re-triggers later. */
    public static void tickBinds(MinecraftClient mc) {
        long window = mc.getWindow().getHandle();
        for (Module m : ALL) {
            if (!m.hasBind()) continue;
            boolean pressed = m.pollBind(window);
            if (pressed && mc.currentScreen == null) {
                m.toggle();
            }
        }
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
