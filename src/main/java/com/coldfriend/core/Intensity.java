package com.coldfriend.core;

import java.util.Locale;

/** A four-step strength choice (for example fog or snow), with a multiplier applied to the configured base value. */
public enum Intensity {
    OFF(0f, "Off"),
    LIGHT(0.5f, "Light"),
    NORMAL(1f, "Normal"),
    HEAVY(2f, "Heavy");

    private final float factor;
    private final String label;

    Intensity(float factor, String label) {
        this.factor = factor;
        this.label = label;
    }

    /** @return the multiplier applied to the base value; 1 for {@link #NORMAL} */
    public float factor() {
        return factor;
    }

    /** @return the text shown on the settings screen */
    public String label() {
        return label;
    }

    /**
     * Moves along the steps, wrapping at the ends.
     *
     * @param delta -1 or +1
     * @return the neighbouring step
     */
    public Intensity step(int delta) {
        Intensity[] all = values();
        return all[Math.floorMod(ordinal() + delta, all.length)];
    }

    /**
     * Parses a step name, ignoring case and surrounding spaces.
     *
     * @param text for example {@code heavy}
     * @return the matching step
     * @throws IllegalArgumentException if no step has that name
     */
    public static Intensity parse(String text) {
        return valueOf(text.trim().toUpperCase(Locale.ROOT));
    }
}
