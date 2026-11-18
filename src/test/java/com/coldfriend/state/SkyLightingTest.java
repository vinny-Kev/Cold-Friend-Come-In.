package com.coldfriend.state;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.coldfriend.time.ClockParameters;
import com.jme3.math.ColorRGBA;
import org.junit.jupiter.api.Test;

class SkyLightingTest {

    private static final ClockParameters P = ClockParameters.DEFAULT;
    private static final float TOLERANCE = 1e-4f;

    private static void assertSky(SkyLighting lighting, double minute, LightingProfile expected) {
        ColorRGBA sky = lighting.update(minute);
        ColorRGBA want = expected.sky();
        assertEquals(want.r, sky.r, TOLERANCE, "red at " + minute);
        assertEquals(want.g, sky.g, TOLERANCE, "green at " + minute);
        assertEquals(want.b, sky.b, TOLERANCE, "blue at " + minute);
    }

    @Test
    void theSkyMatchesTheProfileAtEachFixedTime() {
        SkyLighting lighting = new SkyLighting(P);
        assertSky(lighting, 0, LightingProfile.NIGHT);
        assertSky(lighting, P.dawnStart(), LightingProfile.NIGHT);
        assertSky(lighting, P.dayStart(), LightingProfile.DAY);
        assertSky(lighting, 12 * 60, LightingProfile.DAY);
        assertSky(lighting, P.duskStart(), LightingProfile.DAY);
        assertSky(lighting, (P.duskStart() + P.nightStart()) / 2.0, LightingProfile.DUSK);
        assertSky(lighting, P.nightStart(), LightingProfile.NIGHT);
        assertSky(lighting, 1439.9, LightingProfile.NIGHT);
    }

    @Test
    void theSkyBlendsBetweenTheFixedTimes() {
        SkyLighting lighting = new SkyLighting(P);
        double halfwayToDay = (P.dawnStart() + P.dayStart()) / 2.0;
        ColorRGBA night = LightingProfile.NIGHT.sky();
        ColorRGBA day = LightingProfile.DAY.sky();
        ColorRGBA sky = lighting.update(halfwayToDay);
        assertEquals((night.r + day.r) / 2f, sky.r, TOLERANCE);
        assertEquals((night.b + day.b) / 2f, sky.b, TOLERANCE);
    }

    @Test
    void everyMinuteOfTheDayGivesAValidColour() {
        SkyLighting lighting = new SkyLighting(P);
        for (double minute = 0; minute < 1440; minute += 0.5) {
            ColorRGBA sky = lighting.update(minute);
            for (float channel : new float[] {sky.r, sky.g, sky.b}) {
                assertEquals(channel, Math.max(0f, Math.min(1f, channel)), "out of range at " + minute);
            }
        }
    }
}
