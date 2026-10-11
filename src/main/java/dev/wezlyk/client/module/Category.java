package dev.wezlyk.client.module;

/** Module categories. Themes and Configs are ClickGUI pages, not module categories. */
public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    RENDER("Render");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
