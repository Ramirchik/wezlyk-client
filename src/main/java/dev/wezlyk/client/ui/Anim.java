package dev.wezlyk.client.ui;

/** Tiny time-based easing value: call to(target) once per frame. */
public final class Anim {
    public float v;
    private long last = System.nanoTime();

    public Anim(float start) {
        this.v = start;
    }

    public float to(float target) {
        return to(target, UiMotion.RATE);
    }

    /** Same, with an explicit rate (see UiMotion). Reverses smoothly when the target flips mid-way. */
    public float to(float target, float rate) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1_000_000_000f);
        last = now;
        v += (target - v) * Math.min(1f, dt * rate);
        if (Math.abs(target - v) < 0.003f) {
            v = target;
        }
        return v;
    }
}
