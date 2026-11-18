package com.coldfriend.survival;

/**
 * Tuning for a hearth. Fuel is measured in fuel-hours: one fuel-hour burns for one in-game hour at full level.
 * The fire's level is its fuel times {@code levelPerFuel}, capped, so a fuller stove burns hotter and also faster.
 * See docs/development/SURVIVAL_SYSTEM_DESIGN.md.
 *
 * @param fuelPerFirewood fuel-hours one piece of firewood adds
 * @param maxFuel most fuel the stove holds; feeding more is refused
 * @param levelPerFuel fire level per fuel-hour held
 * @param maxLevel highest level the fire reaches
 * @param embersBelow below this level a lit fire is only embers
 * @param strongHeatLevel at or above this level a burning fire gives strong heat
 * @param minimumBurnPerHour least fuel a lit fire uses per hour, so small fires do burn out
 * @param embersToOutMinutes in-game minutes embers last once the fuel is gone
 * @param startFuel fuel in the stove when a world starts
 * @param startLit whether the stove is burning when a world starts
 */
public record HearthParameters(
        double fuelPerFirewood,
        double maxFuel,
        double levelPerFuel,
        double maxLevel,
        double embersBelow,
        double strongHeatLevel,
        double minimumBurnPerHour,
        double embersToOutMinutes,
        double startFuel,
        boolean startLit) {

    /**
     * One firewood is one fuel-hour; the stove holds six (level 0.9); two pieces give the 0.3 level of a strong
     * fire; embers below 0.15; out 30 minutes after the fuel is gone. A new world starts with the stove lit and three
     * fuel-hours in it (level 0.45), which stays a strong fire for about 2.7 in-game hours.
     */
    public static final HearthParameters DEFAULT = new HearthParameters(
            1.0, 6.0, 0.15, 0.9, 0.15, 0.3, 0.1, 30.0, 3.0, true);

    /** Checks the values. */
    public HearthParameters {
        if (fuelPerFirewood <= 0 || maxFuel <= 0 || levelPerFuel <= 0 || maxLevel <= 0 || minimumBurnPerHour <= 0) {
            throw new IllegalArgumentException("Hearth rates must be positive");
        }
        if (startFuel < 0 || startFuel > maxFuel) {
            throw new IllegalArgumentException("Start fuel must be between 0 and the maximum");
        }
    }
}
