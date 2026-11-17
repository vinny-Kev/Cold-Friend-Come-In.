package com.coldfriend.world.gen;

import java.util.SplittableRandom;

/**
 * The large-scale character of one world, decided first from the seed. Later stages read it, so it is what
 * makes one seed's wilderness differ from another's: which way the land falls, how mountainous and wet it
 * is, how wooded.
 *
 * @param downhillX x part of the unit vector pointing toward the low side of the map
 * @param downhillZ z part of the unit vector pointing toward the low side of the map
 * @param tiltMeters height difference across the whole map
 * @param mountainBias shift toward (positive) or away from mountains
 * @param forestBias shift toward (positive) or away from dense forest
 * @param clearingBias shift toward (positive) or away from open clearings
 * @param riverTargetCells how many routing cells should be river; more means a wetter land
 */
record Geography(
        float downhillX,
        float downhillZ,
        float tiltMeters,
        float mountainBias,
        float forestBias,
        float clearingBias,
        int riverTargetCells) {

    static Geography derive(RandomStreams streams, WorldGenParameters p) {
        SplittableRandom random = streams.random(RandomStreams.GEOGRAPHY);
        double angle = random.nextDouble() * 2.0 * StrictMath.PI;
        float tilt = Maths.lerp(p.tiltMetersMin(), p.tiltMetersMax(), (float) random.nextDouble());
        float mountain = Maths.lerp(p.mountainBiasMin(), p.mountainBiasMax(), (float) random.nextDouble());
        float forest = (float) (random.nextDouble() * 2.0 - 1.0) * p.biasRange();
        float clearing = (float) (random.nextDouble() * 2.0 - 1.0) * p.biasRange();
        int rivers = p.riverTargetCellsMin()
                + random.nextInt(p.riverTargetCellsMax() - p.riverTargetCellsMin() + 1);
        return new Geography(
                (float) -StrictMath.cos(angle), (float) -StrictMath.sin(angle), tilt, mountain, forest, clearing,
                rivers);
    }
}
