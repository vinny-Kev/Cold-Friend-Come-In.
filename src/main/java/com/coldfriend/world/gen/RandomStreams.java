package com.coldfriend.world.gen;

import java.util.SplittableRandom;

/**
 * Independent random streams for the generation stages. Each stream is derived from the world seed, the
 * generation version, the attempt number and the stage name, so one stage never disturbs another.
 */
public final class RandomStreams {

    /** Stage name for the large-scale geography. */
    public static final String GEOGRAPHY = "geography";
    /** Stage name for terrain elevation. */
    public static final String TERRAIN = "terrain";
    /** Stage name for rivers. */
    public static final String HYDROLOGY = "hydrology";
    /** Stage name for environmental regions. */
    public static final String REGIONS = "regions";

    private final long seed;
    private final int version;
    private final int attempt;

    /**
     * @param seed the world seed
     * @param version the generation version
     * @param attempt 0 for the first try; higher when validation made the generator try again
     */
    public RandomStreams(long seed, int version, int attempt) {
        this.seed = seed;
        this.version = version;
        this.attempt = attempt;
    }

    /**
     * @param stage the stage or sub-stage name
     * @return a 64-bit seed for that stage; the same inputs always give the same value
     */
    public long seedFor(String stage) {
        long h = Mixer.mix(seed ^ Mixer.mix(version));
        h = Mixer.mix(h ^ Mixer.mix(attempt));
        return Mixer.mix(h ^ Mixer.hash(stage));
    }

    /**
     * @param stage the stage name
     * @return a random generator private to that stage
     */
    public SplittableRandom random(String stage) {
        return new SplittableRandom(seedFor(stage));
    }
}
