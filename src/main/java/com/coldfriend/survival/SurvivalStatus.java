package com.coldfriend.survival;

import java.util.Optional;

/**
 * A read-only snapshot of one player's needs, for the screen and for later systems. Changing a need goes through
 * the simulation, never through this.
 *
 * @param warmth body heat, 0 to 100
 * @param nourishment food, 0 to 100
 * @param hydration water, 0 to 100
 * @param health 0 to 100; 0 means dead
 * @param healthFalling true while a critical need is taking health away
 * @param causeOfDeath the need that was critical when health reached 0, if the player is dead
 * @param parameters the thresholds the values are judged by
 */
public record SurvivalStatus(
        double warmth,
        double nourishment,
        double hydration,
        double health,
        boolean healthFalling,
        Optional<Need> causeOfDeath,
        SurvivalParameters parameters) {

    /** @return true once health has reached 0 */
    public boolean dead() {
        return causeOfDeath.isPresent();
    }

    /**
     * @param need which need
     * @return its value, 0 to 100
     */
    public double value(Need need) {
        return switch (need) {
            case WARMTH -> warmth;
            case NOURISHMENT -> nourishment;
            case HYDRATION -> hydration;
            case HEALTH -> health;
        };
    }

    /**
     * @param need which need
     * @return true if it is low enough to warn about (this includes critical)
     */
    public boolean low(Need need) {
        return switch (need) {
            case WARMTH -> warmth < parameters.warmthLow();
            case NOURISHMENT -> nourishment < parameters.nourishmentLow();
            case HYDRATION -> hydration < parameters.hydrationLow();
            case HEALTH -> health < parameters.healthLow();
        };
    }

    /**
     * @param need warmth, nourishment or hydration
     * @return true if it is low enough to take health away; health itself is never critical in this sense
     */
    public boolean critical(Need need) {
        return switch (need) {
            case WARMTH -> warmth < parameters.warmthCritical();
            case NOURISHMENT -> nourishment < parameters.nourishmentCritical();
            case HYDRATION -> hydration < parameters.hydrationCritical();
            default -> false;
        };
    }
}
