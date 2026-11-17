package com.coldfriend.survival;

/** Whether a hearth is burning. */
public enum HearthState {
    /** Never lit. Fuel in it does not burn. */
    UNLIT,
    /** Burning with flames. */
    BURNING,
    /** Glowing low. Feeding it brings back flames without a light. */
    EMBERS,
    /** Gone cold. It must be lit again. */
    OUT;

    /** @return true if the hearth is burning fuel, as flames or embers */
    public boolean lit() {
        return this == BURNING || this == EMBERS;
    }
}
