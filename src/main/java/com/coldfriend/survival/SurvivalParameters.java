package com.coldfriend.survival;

/**
 * Tuning for a player's needs. All rates are per in-game minute unless named per day; all values are on the
 * 0 to 100 scale. These values are part of the game's balance, so they live here rather than in the configuration
 * file (see docs/DECISIONS.md D-024). See docs/development/SURVIVAL_SYSTEM_DESIGN.md for the reasoning.
 *
 * @param startWarmth warmth of a new player
 * @param startNourishment nourishment of a new player
 * @param startHydration hydration of a new player
 * @param startHealth health of a new player
 * @param warmthRise how fast warmth rises toward a warmer target
 * @param warmthFall how fast warmth falls toward a colder target
 * @param warmthInsideStrongFire target inside by a strong fire
 * @param warmthInsideLowFire target inside by a small fire or embers
 * @param warmthInsideNoFire target inside with no fire
 * @param warmthOutsideDay target outside in daylight
 * @param warmthOutsideTwilight target outside at dawn or dusk
 * @param warmthOutsideNight target outside at night
 * @param wadingPenalty how much colder the target is while wading
 * @param nourishmentPerDay how much nourishment one in-game day uses
 * @param hydrationPerDay how much hydration one in-game day uses
 * @param warmthLow below this, warmth is shown as a warning
 * @param warmthCritical below this, warmth harms health
 * @param nourishmentLow below this, nourishment is shown as a warning
 * @param nourishmentCritical below this, nourishment harms health
 * @param hydrationLow below this, hydration is shown as a warning
 * @param hydrationCritical below this, hydration harms health
 * @param healthLow below this, health is shown as a warning
 * @param healthLossOneCritical health lost per minute while one need is critical
 * @param healthLossSeveral health lost per minute while two or more needs are critical
 * @param healthRecovery health regained per minute while every need is at or above {@code recoveryAbove}
 * @param recoveryAbove the level every need must hold for health to recover
 * @param rationNourishment nourishment one ration restores
 * @param drinkHydration hydration one drink restores
 */
public record SurvivalParameters(
        double startWarmth,
        double startNourishment,
        double startHydration,
        double startHealth,
        double warmthRise,
        double warmthFall,
        double warmthInsideStrongFire,
        double warmthInsideLowFire,
        double warmthInsideNoFire,
        double warmthOutsideDay,
        double warmthOutsideTwilight,
        double warmthOutsideNight,
        double wadingPenalty,
        double nourishmentPerDay,
        double hydrationPerDay,
        double warmthLow,
        double warmthCritical,
        double nourishmentLow,
        double nourishmentCritical,
        double hydrationLow,
        double hydrationCritical,
        double healthLow,
        double healthLossOneCritical,
        double healthLossSeveral,
        double healthRecovery,
        double recoveryAbove,
        double rationNourishment,
        double drinkHydration) {

    /** The values proposed in the survival design, to be tuned by playtesting. */
    public static final SurvivalParameters DEFAULT = new SurvivalParameters(
            80, 100, 100, 100,
            4, 2,
            85, 55, 25,
            40, 25, 12,
            25,
            70, 90,
            35, 20,
            30, 10,
            30, 10,
            50,
            1, 4, 1, 40,
            45, 30);
}
