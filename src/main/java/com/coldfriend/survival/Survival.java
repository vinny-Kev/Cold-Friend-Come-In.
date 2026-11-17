package com.coldfriend.survival;

import com.coldfriend.time.DayPhase;
import com.coldfriend.time.WorldClock;
import java.util.Optional;

/**
 * One player's needs: warmth, nourishment, hydration and health, and the rules that change them. Plain Java with
 * no engine types; the simulation owns one per player and calls {@link #update} once per fixed step.
 *
 * <p>Rules (see docs/development/SURVIVAL_SYSTEM_DESIGN.md):
 * <ul>
 * <li>Warmth moves toward a target set by where the player is, the time of day and the fire.</li>
 * <li>Nourishment and hydration fall steadily with game time.</li>
 * <li>Health falls only while warmth, nourishment or hydration is critical, faster if two or more are, and
 * recovers slowly when all three are comfortable. A need always passes a warning level before it is critical,
 * so death never comes without warning.</li>
 * </ul>
 * Every value stays between 0 and 100.
 */
public final class Survival {

    private static final double MIN = 0.0;
    private static final double MAX = 100.0;

    private final SurvivalParameters parameters;
    private double warmth;
    private double nourishment;
    private double hydration;
    private double health;
    private boolean healthFalling;
    private Need causeOfDeath;

    /** @param parameters rates and thresholds */
    public Survival(SurvivalParameters parameters) {
        this.parameters = parameters;
        this.warmth = clamp(parameters.startWarmth());
        this.nourishment = clamp(parameters.startNourishment());
        this.hydration = clamp(parameters.startHydration());
        this.health = clamp(parameters.startHealth());
    }

    /**
     * Advances the needs by some game time. Does nothing once the player is dead.
     *
     * @param minutes in-game minutes that passed; zero or less changes nothing
     * @param insideCabin whether the player is inside the cabin
     * @param wading whether the player is standing in water
     * @param phase the time of day
     * @param heat how much heat the cabin's hearth gives
     */
    public void update(double minutes, boolean insideCabin, boolean wading, DayPhase phase, Heat heat) {
        if (causeOfDeath != null || !(minutes > 0.0)) {
            return;
        }
        double target = warmthTarget(insideCabin, wading, phase, heat);
        if (warmth < target) {
            warmth = Math.min(target, warmth + parameters.warmthRise() * minutes);
        } else {
            warmth = Math.max(target, warmth - parameters.warmthFall() * minutes);
        }
        double dayFraction = minutes / WorldClock.MINUTES_PER_DAY;
        nourishment = clamp(nourishment - parameters.nourishmentPerDay() * dayFraction);
        hydration = clamp(hydration - parameters.hydrationPerDay() * dayFraction);
        warmth = clamp(warmth);

        int critical = (warmth < parameters.warmthCritical() ? 1 : 0)
                + (nourishment < parameters.nourishmentCritical() ? 1 : 0)
                + (hydration < parameters.hydrationCritical() ? 1 : 0);
        healthFalling = critical > 0;
        if (critical >= 2) {
            health -= parameters.healthLossSeveral() * minutes;
        } else if (critical == 1) {
            health -= parameters.healthLossOneCritical() * minutes;
        } else if (warmth >= parameters.recoveryAbove() && nourishment >= parameters.recoveryAbove()
                && hydration >= parameters.recoveryAbove()) {
            health += parameters.healthRecovery() * minutes;
        }
        health = clamp(health);
        if (health <= MIN) {
            causeOfDeath = worstCritical();
            healthFalling = false;
        }
    }

    /**
     * The warmth the player's body moves toward in a situation.
     *
     * @param insideCabin whether the player is inside the cabin
     * @param wading whether the player is standing in water; only matters outside
     * @param phase the time of day; only matters outside
     * @param heat the cabin hearth's heat; only matters inside
     * @return the target, 0 to 100
     */
    public double warmthTarget(boolean insideCabin, boolean wading, DayPhase phase, Heat heat) {
        if (insideCabin) {
            return switch (heat) {
                case STRONG -> parameters.warmthInsideStrongFire();
                case LOW -> parameters.warmthInsideLowFire();
                case NONE -> parameters.warmthInsideNoFire();
            };
        }
        double outside = switch (phase) {
            case DAY -> parameters.warmthOutsideDay();
            case DAWN, DUSK -> parameters.warmthOutsideTwilight();
            case NIGHT -> parameters.warmthOutsideNight();
        };
        return clamp(wading ? outside - parameters.wadingPenalty() : outside);
    }

    /**
     * Eating: restores nourishment, up to the maximum.
     *
     * @param amount how much
     * @return false, with nothing changed, if the player is dead
     */
    public boolean eat(double amount) {
        if (causeOfDeath != null) {
            return false;
        }
        nourishment = clamp(nourishment + amount);
        return true;
    }

    /**
     * Drinking: restores hydration, up to the maximum.
     *
     * @param amount how much
     * @return false, with nothing changed, if the player is dead
     */
    public boolean drink(double amount) {
        if (causeOfDeath != null) {
            return false;
        }
        hydration = clamp(hydration + amount);
        return true;
    }

    /**
     * Healing: restores health, up to the maximum.
     *
     * @param amount how much
     * @return false, with nothing changed, if the player is dead
     */
    public boolean heal(double amount) {
        if (causeOfDeath != null) {
            return false;
        }
        health = clamp(health + amount);
        return true;
    }

    /** @return true once health has reached 0 */
    public boolean dead() {
        return causeOfDeath != null;
    }

    /** @return a read-only snapshot of the needs */
    public SurvivalStatus status() {
        return new SurvivalStatus(warmth, nourishment, hydration, health, healthFalling,
                Optional.ofNullable(causeOfDeath), parameters);
    }

    /** @return the critical need furthest below its threshold, as the cause of death */
    private Need worstCritical() {
        Need worst = Need.WARMTH;
        double worstGap = warmth - parameters.warmthCritical();
        if (nourishment - parameters.nourishmentCritical() < worstGap) {
            worst = Need.NOURISHMENT;
            worstGap = nourishment - parameters.nourishmentCritical();
        }
        if (hydration - parameters.hydrationCritical() < worstGap) {
            worst = Need.HYDRATION;
        }
        return worst;
    }

    private static double clamp(double value) {
        return Math.max(MIN, Math.min(MAX, value));
    }
}
