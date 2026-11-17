package com.coldfriend.world.gen;

/**
 * Seeded 2D gradient noise. Uses only integer hashing and basic float arithmetic, so a seed gives the same
 * values on every machine.
 */
final class Noise2D {

    private static final int DIRECTIONS = 16;
    private static final int DIRECTION_SHIFT = 60;
    private static final float[] GRADIENT_X = new float[DIRECTIONS];
    private static final float[] GRADIENT_Y = new float[DIRECTIONS];
    private static final long PRIME_X = 0x632BE59BD9B4E019L;
    private static final long PRIME_Y = 0x2545F4914F6CDD1DL;
    /** Rescales raw gradient noise (about -0.7 to 0.7) to roughly -1 to 1. */
    private static final float OUTPUT_SCALE = 1.4142135f;
    private static final float OCTAVE_OFFSET = 1013.7f;
    private static final float FADE_A = 6f;
    private static final float FADE_B = 15f;
    private static final float FADE_C = 10f;
    private static final float GAIN = 0.5f;

    static {
        for (int i = 0; i < DIRECTIONS; i++) {
            double angle = 2.0 * StrictMath.PI * i / DIRECTIONS;
            GRADIENT_X[i] = (float) StrictMath.cos(angle);
            GRADIENT_Y[i] = (float) StrictMath.sin(angle);
        }
    }

    private final long seed;

    Noise2D(long seed) {
        this.seed = seed;
    }

    /** @return smooth noise in about [-1, 1] */
    float noise(float x, float y) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        float fx = x - x0;
        float fy = y - y0;
        float u = fade(fx);
        float v = fade(fy);
        float n00 = corner(x0, y0, fx, fy);
        float n10 = corner(x0 + 1, y0, fx - 1f, fy);
        float n01 = corner(x0, y0 + 1, fx, fy - 1f);
        float n11 = corner(x0 + 1, y0 + 1, fx - 1f, fy - 1f);
        return Maths.lerp(Maths.lerp(n00, n10, u), Maths.lerp(n01, n11, u), v) * OUTPUT_SCALE;
    }

    /** @return layered noise in about [-1, 1]; each octave doubles the frequency and halves the weight */
    float fbm(float x, float y, int octaves) {
        float sum = 0f;
        float amplitude = 1f;
        float norm = 0f;
        float frequency = 1f;
        for (int octave = 0; octave < octaves; octave++) {
            float shift = octave * OCTAVE_OFFSET;
            sum += amplitude * noise(x * frequency + shift, y * frequency - shift);
            norm += amplitude;
            amplitude *= GAIN;
            frequency *= 2f;
        }
        return sum / norm;
    }

    /** @return sharp-crested ridge noise in [0, 1]; high values form ridges */
    float ridged(float x, float y, int octaves) {
        float sum = 0f;
        float amplitude = 1f;
        float norm = 0f;
        float frequency = 1f;
        for (int octave = 0; octave < octaves; octave++) {
            float shift = octave * OCTAVE_OFFSET;
            float ridge = 1f - Math.abs(noise(x * frequency + shift, y * frequency - shift));
            sum += amplitude * ridge * ridge;
            norm += amplitude;
            amplitude *= GAIN;
            frequency *= 2f;
        }
        return sum / norm;
    }

    private float corner(int ix, int iy, float dx, float dy) {
        long hash = Mixer.mix(seed + ix * PRIME_X + iy * PRIME_Y);
        int direction = (int) (hash >>> DIRECTION_SHIFT);
        return GRADIENT_X[direction] * dx + GRADIENT_Y[direction] * dy;
    }

    private static float fade(float t) {
        return t * t * t * (t * (t * FADE_A - FADE_B) + FADE_C);
    }
}
