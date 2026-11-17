package com.coldfriend.environment;

/**
 * Wind as the foliage shader sees it. The shader turns this into motion from engine time, so changing wind
 * (calm to storm) is a handful of uniform writes with no mesh rebuild.
 *
 * @param dirX wind direction x on the ground plane (normalised on use)
 * @param dirZ wind direction z on the ground plane
 * @param strength steady push, 0 for still air, about 1 for a brisk breeze
 * @param gust extra push that comes and goes in slow waves, 0 for none
 */
public record Wind(float dirX, float dirZ, float strength, float gust) {

    /** @return almost still air */
    public static Wind calm() {
        return new Wind(1f, 0.4f, 0.12f, 0.05f);
    }

    /** @return a gentle steady wind with light gusts */
    public static Wind breezy() {
        return new Wind(1f, 0.4f, 0.5f, 0.35f);
    }

    /** @return a hard wind with strong gusts */
    public static Wind storm() {
        return new Wind(1f, 0.4f, 1.1f, 0.9f);
    }
}
