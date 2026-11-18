package com.coldfriend.survival;

/**
 * A stove that burns fuel and heats a room: the authoritative fire. It knows nothing about lights or particles;
 * drawing reads it and never changes it. Plain Java; the simulation calls {@link #update} once per fixed step.
 *
 * <p>Rules (see docs/development/SURVIVAL_SYSTEM_DESIGN.md):
 * <ul>
 * <li>Fuel only burns while the hearth is lit (burning or embers).</li>
 * <li>Level is fuel times {@code levelPerFuel}, capped at {@code maxLevel}. A lit fire uses its level in
 * fuel-hours per hour, but at least {@code minimumBurnPerHour}, so it always burns down in the end.</li>
 * <li>Below {@code embersBelow} a lit fire is embers. Feeding embers brings back flames without a light.</li>
 * <li>When the fuel is gone, embers last {@code embersToOutMinutes}, then the hearth is out and must be lit
 * again.</li>
 * </ul>
 */
public final class Hearth {

    private static final double MINUTES_PER_HOUR = 60.0;

    private final HearthParameters parameters;
    private HearthState state;
    private double fuel;
    private double emptyMinutes;

    /** @param parameters rates and starting state */
    public Hearth(HearthParameters parameters) {
        this.parameters = parameters;
        this.fuel = parameters.startFuel();
        this.state = parameters.startLit() && fuel > 0 ? stateForFuel() : HearthState.UNLIT;
    }

    /**
     * Burns fuel for some game time.
     *
     * @param minutes in-game minutes that passed; zero or less changes nothing
     */
    public void update(double minutes) {
        if (!state.lit() || !(minutes > 0.0)) {
            return;
        }
        if (fuel > 0.0) {
            double burn = Math.max(level(), parameters.minimumBurnPerHour()) * minutes / MINUTES_PER_HOUR;
            fuel = Math.max(0.0, fuel - burn);
            emptyMinutes = 0.0;
            state = stateForFuel();
            return;
        }
        emptyMinutes += minutes;
        if (emptyMinutes >= parameters.embersToOutMinutes()) {
            state = HearthState.OUT;
            emptyMinutes = 0.0;
        }
    }

    /**
     * Whether some firewood would fit.
     *
     * @param pieces how many pieces
     * @return true if the stove has room for all of them
     */
    public boolean canAdd(int pieces) {
        return pieces > 0 && fuel + pieces * parameters.fuelPerFirewood() <= parameters.maxFuel() + 1e-9;
    }

    /**
     * Puts firewood in the stove. Embers flare back into flames if there is enough fuel.
     *
     * @param pieces how many pieces
     * @return false, with nothing changed, if they do not fit
     */
    public boolean addFirewood(int pieces) {
        if (!canAdd(pieces)) {
            return false;
        }
        fuel += pieces * parameters.fuelPerFirewood();
        if (state.lit()) {
            emptyMinutes = 0.0;
            state = stateForFuel();
        }
        return true;
    }

    /** @return true if the hearth is not lit and has fuel to light */
    public boolean canLight() {
        return !state.lit() && fuel > 0.0;
    }

    /**
     * Lights the fuel in a cold stove. Whatever is needed to light it (kindling, a match) is checked and used by
     * the caller.
     *
     * @return false, with nothing changed, if it is already lit or has no fuel
     */
    public boolean light() {
        if (!canLight()) {
            return false;
        }
        state = stateForFuel();
        emptyMinutes = 0.0;
        return true;
    }

    /** @return how big the fire is, 0 when not lit */
    public double level() {
        return state.lit() ? Math.min(parameters.maxLevel(), fuel * parameters.levelPerFuel()) : 0.0;
    }

    /** @return how much heat the hearth gives its room */
    public Heat heat() {
        if (state == HearthState.BURNING && level() >= parameters.strongHeatLevel()) {
            return Heat.STRONG;
        }
        return state.lit() ? Heat.LOW : Heat.NONE;
    }

    /** @return a read-only snapshot */
    public HearthStatus status() {
        return new HearthStatus(state, level(), fuel, parameters.maxFuel(), heat());
    }

    private HearthState stateForFuel() {
        return fuel * parameters.levelPerFuel() < parameters.embersBelow() ? HearthState.EMBERS : HearthState.BURNING;
    }
}
