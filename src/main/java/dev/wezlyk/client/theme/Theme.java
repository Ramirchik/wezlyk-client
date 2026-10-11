package dev.wezlyk.client.theme;

/**
 * Complete colour set for the interface (all values are ARGB).
 * Glass.apply() copies these into the drawing helpers; HUD elements read the hud* values directly.
 */
public record Theme(
    String name,
    int bg, int bgTop, int panel, int border,
    int card, int cardHover, int field,
    int text, int muted, int faint, int accent,
    int selRing, int tabSel,
    int switchOff, int switchOn, int knobOff, int knobOn,
    int sheen, int dim, int sep,
    int btn, int btnHover, int btnBorder, int btnBorderHover, int btnText,
    int hudBg, int hudBorder, int hudText, int hudMuted
) {
    public static final Theme DARK_CLASSIC = new Theme("Dark Classic",
        0xFF080808, 0xFF101012, 0xE6151515, 0xFF2B2B2D,
        0xFF1A1A1B, 0xFF222325, 0xFF101011,
        0xFFF2F2F2, 0xFF8C8C91, 0xFF5F5F64, 0xFFB9C1CC,
        0xFF5B616A, 0xFF242527,
        0xFF2A2A2C, 0xFF7C8591, 0xFF8C8C91, 0xFFF4F6F8,
        0x14FFFFFF, 0xFF080808, 0x1FFFFFFF,
        0xFF131314, 0xFF1F2022, 0xFF262628, 0xFF4A4D52, 0xFFC9C9CC,
        0xD0101012, 0xFF2B2B2D, 0xFFF2F2F2, 0xFF8C8C91);

    public static final Theme SAKURA = new Theme("Sakura",
        0xFFFFF0F4, 0xFFFFE4EC, 0xF2FFF7FA, 0xFFF0C4D2,
        0xFFFCE4EC, 0xFFF8D2DF, 0xFFFFFFFF,
        0xFF3A2230, 0xFF8A6577, 0xFFB898A6, 0xFFD9668C,
        0xFFE393AE, 0xFFF9D6E2,
        0xFFEBCBD6, 0xFFE07A9D, 0xFFFFFFFF, 0xFFFFFFFF,
        0x55FFFFFF, 0xFFFFE4EC, 0x40D9668C,
        0xFFFCE4EC, 0xFFF8D2DF, 0xFFF0C4D2, 0xFFE393AE, 0xFF5A3347,
        0xE6FBE0E8, 0xFFE9B3C6, 0xFF3A2230, 0xFF8A6577);

    public static final Theme LIGHT = new Theme("Light",
        0xFFEDEDF0, 0xFFF6F6F8, 0xF2F7F7F8, 0xFFCFCFD6,
        0xFFEBEBEF, 0xFFDFDFE5, 0xFFFFFFFF,
        0xFF18181B, 0xFF66666E, 0xFF9A9AA2, 0xFF4F5866,
        0xFF7A8391, 0xFFDCDCE3,
        0xFFC9C9D0, 0xFF4F5866, 0xFFFFFFFF, 0xFFFFFFFF,
        0x66FFFFFF, 0xFFF0F0F2, 0x26000000,
        0xFFF0F0F3, 0xFFE3E3E8, 0xFFCFCFD6, 0xFF9A9AA6, 0xFF2A2A30,
        0xE6F4F4F6, 0xFFC4C4CC, 0xFF18181B, 0xFF66666E);
}
