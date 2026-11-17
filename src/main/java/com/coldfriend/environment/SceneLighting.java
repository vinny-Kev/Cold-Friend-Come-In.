package com.coldfriend.environment;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;

/**
 * Light values the custom shaders (water, foliage) use, so they follow whatever lighting the scene has (day, night, interior
 * glow). The shader does not read jME light objects, so the owner of the scene passes these in.
 *
 * @param sunDirection direction the main light travels (not normalised)
 * @param sun colour of the main light
 * @param ambient ambient light colour
 * @param sky colour reflected at grazing angles
 */
public record SceneLighting(Vector3f sunDirection, ColorRGBA sun, ColorRGBA ambient, ColorRGBA sky) {

    /** @return overcast winter daylight */
    public static SceneLighting day() {
        return new SceneLighting(new Vector3f(-0.4f, -1f, -0.3f), new ColorRGBA(0.9f, 0.92f, 0.95f, 1f),
                new ColorRGBA(0.55f, 0.58f, 0.64f, 1f), new ColorRGBA(0.52f, 0.58f, 0.66f, 1f));
    }

    /** @return cold moonlit night */
    public static SceneLighting night() {
        return new SceneLighting(new Vector3f(-0.4f, -1f, -0.3f), new ColorRGBA(0.5f, 0.56f, 0.68f, 1f),
                new ColorRGBA(0.2f, 0.23f, 0.3f, 1f), new ColorRGBA(0.08f, 0.1f, 0.16f, 1f));
    }
}
