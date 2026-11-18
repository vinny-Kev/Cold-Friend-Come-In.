package com.coldfriend.state;

import com.coldfriend.time.ClockParameters;
import com.coldfriend.time.WorldClock;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 * Presentation only: the sky colour, ambient light and sun or moon, following the game clock. It blends between
 * the {@link LightingProfile}s at fixed times of day (night until dawn, day from full daylight until dusk, dusk
 * halfway into the evening, night from nightfall) so light changes gradually. It reads the time; it never
 * decides it. Reuses its colour objects, so updating every frame allocates nothing.
 */
final class SkyLighting {

    private final double[] times;
    private final LightingProfile[] profiles;
    private final Vector3f[] directions;
    private final AmbientLight ambient = new AmbientLight();
    private final DirectionalLight sun = new DirectionalLight();
    private final ColorRGBA sky = new ColorRGBA();
    private final ColorRGBA ambientColor = new ColorRGBA();
    private final ColorRGBA sunColor = new ColorRGBA();
    private final Vector3f direction = new Vector3f();

    /** @param clock the day's phase times */
    SkyLighting(ClockParameters clock) {
        double duskMiddle = (clock.duskStart() + clock.nightStart()) / 2.0;
        times = new double[] {0, clock.dawnStart(), clock.dayStart(), clock.duskStart(), duskMiddle,
            clock.nightStart(), WorldClock.MINUTES_PER_DAY};
        profiles = new LightingProfile[] {LightingProfile.NIGHT, LightingProfile.NIGHT, LightingProfile.DAY,
            LightingProfile.DAY, LightingProfile.DUSK, LightingProfile.NIGHT, LightingProfile.NIGHT};
        directions = new Vector3f[profiles.length];
        for (int i = 0; i < profiles.length; i++) {
            directions[i] = profiles[i].direction();
        }
    }

    /** Adds the ambient light and the sun to the node they should light. */
    void addTo(Node node) {
        node.addLight(ambient);
        node.addLight(sun);
    }

    /**
     * Sets the lights for a time of day.
     *
     * @param minute minutes after midnight
     * @return the sky colour, also used for the background and the fog; reused, so copy it to keep it
     */
    ColorRGBA update(double minute) {
        int i = 0;
        while (i < times.length - 2 && minute >= times[i + 1]) {
            i++;
        }
        float t = (float) Math.max(0.0, Math.min(1.0, (minute - times[i]) / (times[i + 1] - times[i])));
        LightingProfile from = profiles[i];
        LightingProfile to = profiles[i + 1];
        sky.interpolateLocal(from.sky(), to.sky(), t);
        ambientColor.interpolateLocal(from.ambient(), to.ambient(), t);
        sunColor.interpolateLocal(from.sun(), to.sun(), t);
        direction.interpolateLocal(directions[i], directions[i + 1], t).normalizeLocal();
        ambient.setColor(ambientColor);
        sun.setColor(sunColor);
        sun.setDirection(direction);
        return sky;
    }
}
