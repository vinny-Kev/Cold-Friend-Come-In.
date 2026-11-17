package com.coldfriend.world;

/**
 * Deterministic base ground height: fractal value noise from a seed. This is the geography before any water
 * is carved into it; {@link GeneratedWorld} layers rivers and lakes on top.
 */
public final class BaseTerrain {

    /** The world spans {@code -HALF_EXTENT..HALF_EXTENT} on both horizontal axes. */
    public static final float HALF_EXTENT = 128f;

    private static final float FREQUENCY = 1f / 70f;
    private static final int OCTAVES = 4;
    private static final float LACUNARITY = 2f;
    private static final float GAIN = 0.5f;
    private static final float AMPLITUDE = 60f;
    private static final float HASH_RANGE = 1 << 24;
    private static final int HASH_SHIFT = 40;

    private final long seed;

    /** @param seed world seed */
    public BaseTerrain(long seed) {
        this.seed = seed;
    }

    /**
     * Ground height before water carving.
     *
     * @param x world x
     * @param z world z
     * @return height in metres
     */
    public float height(float x, float z) {
        float sum = 0f;
        float norm = 0f;
        float amp = 1f;
        float freq = FREQUENCY;
        for (int octave = 0; octave < OCTAVES; octave++) {
            sum += amp * valueNoise(x * freq, z * freq, octave);
            norm += amp;
            amp *= GAIN;
            freq *= LACUNARITY;
        }
        return (sum / norm - 0.5f) * AMPLITUDE;
    }

    private float valueNoise(float x, float z, int octave) {
        int ix = (int) Math.floor(x);
        int iz = (int) Math.floor(z);
        float fx = fade(x - ix);
        float fz = fade(z - iz);
        float a = lattice(ix, iz, octave);
        float b = lattice(ix + 1, iz, octave);
        float c = lattice(ix, iz + 1, octave);
        float d = lattice(ix + 1, iz + 1, octave);
        float top = a + (b - a) * fx;
        float bottom = c + (d - c) * fx;
        return top + (bottom - top) * fz;
    }

    private static float fade(float t) {
        return t * t * t * (t * (t * 6f - 15f) + 10f);
    }

    private float lattice(int ix, int iz, int octave) {
        long h = seed ^ (ix * 0x9E3779B97F4A7C15L) ^ (iz * 0xC2B2AE3D27D4EB4FL) ^ (octave * 0x165667B19E3779F9L);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> HASH_SHIFT) / HASH_RANGE;
    }
}
