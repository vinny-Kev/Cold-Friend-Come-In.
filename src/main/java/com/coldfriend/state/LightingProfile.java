package com.coldfriend.state;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;

/**
 * The look of the sky and lights at one time of day. {@link SkyLighting} blends between these as the game clock
 * runs; the profiles themselves are only colours and a light direction.
 */
public enum LightingProfile {
    /** Bright overcast day. */
    DAY(new ColorRGBA(0.70f, 0.78f, 0.88f, 1f), new ColorRGBA(0.72f, 0.74f, 0.78f, 1f),
            new ColorRGBA(0.95f, 0.93f, 0.85f, 1f), new Vector3f(-0.3f, -1f, -0.4f)),
    /** The normal dim winter dusk. */
    DUSK(new ColorRGBA(0.52f, 0.58f, 0.66f, 1f), new ColorRGBA(0.42f, 0.47f, 0.58f, 1f),
            new ColorRGBA(0.62f, 0.68f, 0.85f, 1f), new Vector3f(-0.4f, -1f, -0.3f)),
    /** Dark night lit only by a faint moon. */
    NIGHT(new ColorRGBA(0.03f, 0.04f, 0.09f, 1f), new ColorRGBA(0.10f, 0.12f, 0.20f, 1f),
            new ColorRGBA(0.20f, 0.25f, 0.42f, 1f), new Vector3f(-0.4f, -1f, -0.3f));

    private final ColorRGBA sky;
    private final ColorRGBA ambient;
    private final ColorRGBA sun;
    private final Vector3f direction;

    LightingProfile(ColorRGBA sky, ColorRGBA ambient, ColorRGBA sun, Vector3f direction) {
        this.sky = sky;
        this.ambient = ambient;
        this.sun = sun;
        this.direction = direction.normalize();
    }

    /** @return the sky colour, which fog also fades into */
    public ColorRGBA sky() {
        return sky;
    }

    /** @return the colour of the light that reaches everything */
    public ColorRGBA ambient() {
        return ambient;
    }

    /** @return the colour of the sun or moon */
    public ColorRGBA sun() {
        return sun;
    }

    /** @return the direction the sun or moon shines in */
    public Vector3f direction() {
        return direction.clone();
    }
}
