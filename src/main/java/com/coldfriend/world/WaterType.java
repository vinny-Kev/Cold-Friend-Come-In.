package com.coldfriend.world;

/** The kinds of water body the generator produces. */
public enum WaterType {
    /** Wide flowing channel. */
    RIVER(true),
    /** Narrow flowing channel. */
    STREAM(true),
    /** Large still basin. */
    LAKE(false),
    /** Small still basin; also forms where a river ends in a hollow. */
    POND(false);

    private final boolean flowing;

    WaterType(boolean flowing) {
        this.flowing = flowing;
    }

    /** @return true for bodies that have a flow direction */
    public boolean isFlowing() {
        return flowing;
    }
}
