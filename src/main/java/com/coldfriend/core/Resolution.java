package com.coldfriend.core;

import java.util.List;

/**
 * A display resolution in pixels.
 *
 * @param width horizontal size in pixels, must be positive
 * @param height vertical size in pixels, must be positive
 */
public record Resolution(int width, int height) {

    /** Resolutions offered by the settings screen, in cycling order. */
    public static final List<Resolution> OPTIONS = List.of(
            new Resolution(1280, 720),
            new Resolution(1600, 900),
            new Resolution(1920, 1080),
            new Resolution(2560, 1440));

    public Resolution {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Resolution must be positive: " + width + "x" + height);
        }
    }

    /**
     * Parses a {@code WIDTHxHEIGHT} string such as {@code 1280x720}.
     *
     * @param text the text to parse
     * @return the parsed resolution
     * @throws IllegalArgumentException if the text is not in the expected form
     */
    public static Resolution parse(String text) {
        // Limit -1 keeps a trailing empty part, so "1280x720x" is rejected rather than read as "1280x720".
        String[] parts = text.trim().split("x", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Expected WIDTHxHEIGHT but was: " + text);
        }
        return new Resolution(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
    }

    @Override
    public String toString() {
        return width + "x" + height;
    }
}
