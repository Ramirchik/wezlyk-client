package dev.wezlyk.client.ui;

/**
 * Shared motion constants for every animated control, so all of them move with the same feel.
 * Everything is evaluated lazily while a screen is being drawn: no timers, no threads.
 */
public final class UiMotion {
    /** Smoothing rate of state animations (1/s). Higher is faster. 14 settles in roughly 0.2 s. */
    public static final float RATE = 14f;
    /** Press feedback is a little quicker than the state change itself. */
    public static final float PRESS_RATE = 26f;

    private UiMotion() {}

    /** Smoothstep easing of a 0..1 value (slow start, slow end). */
    public static float ease(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }
}
